package com.yongzhe.localserver;

import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class LocalWebSocketServer {
    public interface Logger { void log(String line); }
    private static final String MAGIC="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final int port; private final Logger logger; private final GameDatabase db;
    private volatile boolean running; private ServerSocket serverSocket; private ExecutorService pool;
    public LocalWebSocketServer(int port,GameDatabase db,Logger logger){this.port=port;this.db=db;this.logger=logger;}
    public boolean isRunning(){return running;}
    public synchronized void start() throws IOException{if(running)return;serverSocket=new ServerSocket(port,20,InetAddress.getByName("127.0.0.1"));pool=Executors.newCachedThreadPool();running=true;log("WS LISTEN 127.0.0.1:"+port+" /afk/ws");new Thread(()->{while(running){try{Socket s=serverSocket.accept();pool.execute(()->handle(s));}catch(IOException e){if(running)log("WS ACCEPT ERROR: "+e.getMessage());}}},"Yongzhe-WS").start();}
    public synchronized void stop(){running=false;try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}if(pool!=null)pool.shutdownNow();serverSocket=null;pool=null;log("WS SERVER STOPPED");}
    private void handle(Socket socket){try{socket.setSoTimeout(0);InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();String request=readHttpLine(in);if(request==null)return;String[] rp=request.split(" ");String path=rp.length>1?rp[1]:"/";Map<String,String> h=new HashMap<>();String line;while((line=readHttpLine(in))!=null&&!line.isEmpty()){int p=line.indexOf(':');if(p>0)h.put(line.substring(0,p).trim().toLowerCase(Locale.ROOT),line.substring(p+1).trim());}String key=h.get("sec-websocket-key");if(key==null)return;String response="HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+wsAccept(key)+"\r\n\r\n";out.write(response.getBytes(StandardCharsets.UTF_8));out.flush();log("WS CONNECT "+path);sendText(out,"{\"type\":\"connected\",\"battle\":"+db.battle().toString()+"}");while(running&&!socket.isClosed()){Frame f=readFrame(in);if(f==null)break;if(f.opcode==8)break;if(f.opcode==9){sendFrame(out,10,f.payload);continue;}if(f.opcode!=1)continue;String msg=new String(f.payload,StandardCharsets.UTF_8);log("WS << "+trim(msg,160));if(msg.contains("\"type\":\"ping\"")||msg.contains("\"type\" : \"ping\"")){sendText(out,"{\"type\":\"pong\",\"ts\":"+System.currentTimeMillis()+",\"battle\":"+db.battle().toString()+"}");}else if(msg.contains("manual")){sendText(out,"{\"type\":\"battle\",\"data\":"+db.manualBattle().toString()+"}");}else sendText(out,"{\"type\":\"state\",\"data\":"+db.battle().toString()+"}");}}catch(Exception e){if(running)log("WS CLIENT ERROR: "+e.getClass().getSimpleName()+": "+e.getMessage());}finally{try{socket.close();}catch(Exception ignored){}log("WS DISCONNECT");}}
    // Read handshake directly from the socket stream: BufferedReader may consume WS frame bytes.
    private static String readHttpLine(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1){if(c==10)break;if(c!=13)b.write(c);if(b.size()>8192)throw new IOException("HTTP header too long");}
        if(c==-1&&b.size()==0)return null;
        return new String(b.toByteArray(),StandardCharsets.UTF_8);
    }
    private static String wsAccept(String key)throws Exception{return Base64.encodeToString(MessageDigest.getInstance("SHA-1").digest((key.trim()+MAGIC).getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
    private static class Frame{int opcode;byte[] payload;Frame(int o,byte[] p){opcode=o;payload=p;}}
    private static Frame readFrame(InputStream in)throws IOException{int b1=in.read();if(b1<0)return null;int b2=in.read();if(b2<0)throw new EOFException();int opcode=b1&15;boolean masked=(b2&128)!=0;long len=b2&127;if(len==126)len=((long)readByte(in)<<8)|readByte(in);else if(len==127){len=0;for(int i=0;i<8;i++)len=(len<<8)|readByte(in);}if(len>1024*1024)throw new IOException("frame too large");byte[] mask=masked?readFully(in,4):null,data=readFully(in,(int)len);if(masked)for(int i=0;i<data.length;i++)data[i]=(byte)(data[i]^mask[i%4]);return new Frame(opcode,data);}
    private static synchronized void sendFrame(OutputStream out,int opcode,byte[] payload)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(0x80|(opcode&15));int len=payload.length;if(len<126)b.write(len);else if(len<=65535){b.write(126);b.write((len>>8)&255);b.write(len&255);}else{b.write(127);long l=len;for(int i=7;i>=0;i--)b.write((int)((l>>(8*i))&255));}b.write(payload);out.write(b.toByteArray());out.flush();}
    private static void sendText(OutputStream out,String text)throws IOException{sendFrame(out,1,text.getBytes(StandardCharsets.UTF_8));}
    private static int readByte(InputStream in)throws IOException{int v=in.read();if(v<0)throw new EOFException();return v;}
    private static byte[] readFully(InputStream in,int n)throws IOException{byte[] b=new byte[n];int p=0;while(p<n){int r=in.read(b,p,n-p);if(r<0)throw new EOFException();p+=r;}return b;}
    private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,n)+"...";}
    private void log(String s){if(logger!=null)logger.log(s);}
}
