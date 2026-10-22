package com.yongzhe.localserver;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class LocalHttpServer {
    public interface Logger { void log(String line); }
    private final int port; private final Logger logger; private final GameDatabase db;
    private volatile boolean running; private ServerSocket serverSocket; private ExecutorService pool;
    public LocalHttpServer(int port,GameDatabase db,Logger logger){this.port=port;this.db=db;this.logger=logger;}
    public boolean isRunning(){return running;}
    public synchronized void start() throws IOException{if(running)return;serverSocket=new ServerSocket(port,50,InetAddress.getByName("127.0.0.1"));pool=Executors.newCachedThreadPool();running=true;log("HTTP LISTEN 127.0.0.1:"+port);new Thread(()->{while(running){try{Socket s=serverSocket.accept();pool.execute(()->handle(s));}catch(IOException e){if(running)log("HTTP ACCEPT ERROR: "+e.getMessage());}}},"Yongzhe-HTTP").start();}
    public synchronized void stop(){running=false;try{if(serverSocket!=null)serverSocket.close();}catch(Exception ignored){}if(pool!=null)pool.shutdownNow();serverSocket=null;pool=null;log("HTTP SERVER STOPPED");}

    private void handle(Socket socket){try{socket.setSoTimeout(10000);InputStream raw=socket.getInputStream();String req=readHttpLine(raw);if(req==null||req.trim().isEmpty())return;Map<String,String> headers=new HashMap<>();String line;int len=0;while((line=readHttpLine(raw))!=null&&!line.isEmpty()){int p=line.indexOf(':');if(p>0){String k=line.substring(0,p).trim().toLowerCase(Locale.ROOT),v=line.substring(p+1).trim();headers.put(k,v);if("content-length".equals(k))try{len=Integer.parseInt(v);}catch(Exception ignored){}}}if(len<0||len>1048576)throw new IOException("Invalid body size");byte[] bytes=new byte[len];int got=0;while(got<len){int n=raw.read(bytes,got,len-got);if(n<0)break;got+=n;}String body=new String(bytes,0,got,StandardCharsets.UTF_8);String[] a=req.split(" ");String method=a.length>0?a[0]:"GET",rawPath=a.length>1?a[1]:"/",path=rawPath,query="";int q=rawPath.indexOf('?');if(q>=0){path=rawPath.substring(0,q);query=rawPath.substring(q+1);}log(method+" "+rawPath);if("OPTIONS".equals(method)){send(socket,204,"text/plain","");return;}route(socket,method,path,query,body,headers);}catch(Exception e){log("HTTP CLIENT ERROR: "+e.getClass().getSimpleName()+": "+e.getMessage());}finally{try{socket.close();}catch(Exception ignored){}}}

    private static String readHttpLine(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1){if(c==10)break;if(c!=13)b.write(c);if(b.size()>8192)throw new IOException("HTTP header too long");}
        if(c==-1&&b.size()==0)return null;
        return new String(b.toByteArray(),StandardCharsets.UTF_8);
    }
    private void route(Socket s,String method,String path,String query,String body,Map<String,String> headers)throws Exception{
        Map<String,String> q=parseQuery(query);JSONObject in=parseBody(body,headers.get("content-type"));
        if("/".equals(path)){redirect(s,"/gm");return;}if("/api/health".equals(path)){okRaw(s,new JSONObject().put("ok",true).put("service","勇者之路本地服务器").put("version","5.0").put("httpPort",9055).put("wsPort",8058));return;}if("/gm".equals(path)||"/gm/".equals(path)){send(s,200,"text/html; charset=utf-8",gmHtml());return;}

        // Account / region entry
        if(path.equals("/user/session-login")||path.equals("/user/phone-login")||path.equals("/user/phone-register")||path.equals("/user/tap-login")||path.equals("/user/tap-pc-login")){JSONObject p=db.player();JSONObject d=new JSONObject();d.put("userid",p.optString("userid","local-user"));d.put("userId",p.optString("userid","local-user"));d.put("displayName",p.optString("name","勇者"));d.put("sessionToken","local-session");d.put("token","local-session");ok(s,d);return;}
        if(path.equals("/user/info")||path.equals("/user/tap-profile")){ok(s,db.player());return;}
        if(path.equals("/user/cancel")){ok(s,new JSONObject().put("cancelled",false).put("local",true));return;}
        if(path.equals("/upinfo/enter")){JSONObject p=db.player();ok(s,new JSONObject().put("upid",p.optString("upid","local-up")).put("userid",p.optString("userid","local-user")).put("characterId",p.optString("charId","1")).put("serverTime",System.currentTimeMillis()));return;}

        // Character
        if(path.equals("/character/list")){boolean equipOnly="1".equals(q.get("equipOnly"));ok(s,db.characterList(equipOnly));return;}
        if(path.equals("/character/create")){JSONObject p=db.player();String name=first(in,"name","characterName","displayName");if(name!=null&&!name.trim().isEmpty())db.updatePlayer(name,p.optInt("level",1),p.optLong("exp",0),p.optLong("diamond",0),p.optLong("gold",0));ok(s,db.player());return;}
        if(path.equals("/character/delete")){ok(s,new JSONObject().put("deleted",false).put("localSingleCharacter",true));return;}
        if(path.equals("/character/bag")){String storage=q.containsKey("storage")?q.get("storage"):"bag";int offset=intVal(q.get("offset"),0),limit=intVal(q.get("limit"),24);ok(s,db.bagPage(storage,offset,limit));return;}
        if(path.equals("/character/bag/item")){String toolId=q.containsKey("toolId")?q.get("toolId"):q.get("id");if(toolId==null)toolId=first(in,"toolId","id");ok(s,db.bagItem(toolId==null?"":toolId));return;}
        if(path.equals("/character/equip")){String id=first(in,"toolId","id","bagId");boolean yes=db.equip(id);ok(s,new JSONObject().put("success",yes).put("character",db.player()).put("equips",db.equippedItems()));return;}
        if(path.equals("/character/unequip")){String id=first(in,"toolId","id","bagId");boolean yes=db.unequip(id);ok(s,new JSONObject().put("success",yes).put("equips",db.equippedItems()));return;}
        if(path.equals("/character/unequip-all")){db.unequipAll();ok(s,new JSONObject().put("success",true).put("equips",db.equippedItems()));return;}
        if(path.equals("/character/auto-equip")){JSONArray bag=db.bag("bag");for(int i=0;i<bag.length();i++){JSONObject item=bag.optJSONObject(i);if(item!=null&&"equipment".equals(item.optString("type")))db.equip(item.optString("toolId"));}ok(s,new JSONObject().put("success",true).put("equips",db.equippedItems()));return;}
        if(path.equals("/character/allocate")){String stat=first(in,"stat","key","type");int points=in.optInt("points",in.optInt("value",1));ok(s,db.allocate(stat,points));return;}
        if(path.equals("/character/tool-groups")){ok(s,new JSONObject().put("items",new JSONArray()).put("groups",new JSONArray()));return;}
        if(path.equals("/character/skill-slot")){
            if("POST".equalsIgnoreCase(method)||"PUT".equalsIgnoreCase(method)){
                JSONObject update=new JSONObject();update.put("localSkillSlot",in.toString());db.saveSettings(update);
            }
            String saved=db.settings().optString("localSkillSlot","{}");JSONObject slots;
            try{slots=new JSONObject(saved);}catch(Exception ignored){slots=new JSONObject();}
            ok(s,new JSONObject().put("success",true).put("skillSlot",slots).put("slots",slots));return;
        }
        if(path.equals("/character/follow-target")){ok(s,new JSONObject().put("success",true));return;}
        if(path.startsWith("/character/transfer/")){ok(s,new JSONObject().put("success",true).put("character",db.player()));return;}
        if(path.equals("/character/bag/to-warehouse")){String id=first(in,"toolId","id");ok(s,new JSONObject().put("success",db.moveItem(id,"warehouse")));return;}
        if(path.equals("/character/bag/to-bag")){String id=first(in,"toolId","id");ok(s,new JSONObject().put("success",db.moveItem(id,"bag")));return;}
        if(path.equals("/character/bag/discard")){String id=first(in,"toolId","id");ok(s,new JSONObject().put("success",db.deleteItem(id)));return;}
        if(path.equals("/character/bag/sell")){String id=first(in,"toolId","id");long g=db.sellItem(id,in.optInt("count",1));ok(s,new JSONObject().put("goldEarned",g).put("player",db.player()));return;}
        if(path.equals("/character/bag/sell-all")){long g=db.sellAll();ok(s,new JSONObject().put("goldEarned",g).put("player",db.player()));return;}
        if(path.equals("/character/bag/use-ticket")||path.equals("/character/bag/open-box")||path.equals("/character/bag/open-blindbox")||path.equals("/character/bag/confirm-blindbox")||path.equals("/character/bag/learn-skill")||path.equals("/character/bag/use-rebirth")||path.equals("/character/bag/use-alliance-reset")){String id=first(in,"toolId","id");JSONObject item=db.bagItem(id);if(item.length()>0)db.sellItem(id,1);ok(s,new JSONObject().put("success",true).put("item",item).put("character",db.player()));return;}
        if(path.equals("/character/bag/list-market")){ok(s,new JSONObject().put("success",true).put("listingId","local-"+System.currentTimeMillis()));return;}

        // Maps / monsters / AFK battle
        if(path.equals("/afk/maps")){ok(s,GameContent.maps());return;}
        if(path.equals("/afk/instance-maps")){ok(s,GameContent.instanceMaps());return;}
        if(path.equals("/afk/instance-maps/levels")){String instanceMapId=q.containsKey("instanceMapId")?q.get("instanceMapId"):"instance_1";ok(s,GameContent.instanceLevels(instanceMapId));return;}
        if(path.equals("/afk/monsters")){String mapId=q.containsKey("mapId")?q.get("mapId"):"map_1";ok(s,GameContent.monsters(mapId));return;}
        if(path.equals("/afk/monster/detail")){String mapId=q.containsKey("mapId")?q.get("mapId"):"map_1",mid=q.get("monsterId");ok(s,GameContent.monster(mapId,mid==null?"":mid));return;}
        if(path.equals("/afk/battle/status")){ok(s,db.battle());return;}
        if(path.equals("/afk/battle/start")){String mapId=first(in,"mapId");if(mapId==null)mapId="map_1";String mid=first(in,"monsterId");JSONObject m=GameContent.monster(mapId,mid==null?"":mid);JSONObject state=db.startBattle(mapId,m.optString("id",mid),m.optString("name","怪物"),m.optInt("level",1));state.put("mapName",GameContent.mapName(mapId));state.put("enterGoldSpent",0);state.put("totalExp",0);state.put("totalGold",0);state.put("levelStart",db.player().optInt("level",1));state.put("levelEnd",db.player().optInt("level",1));JSONObject hero=db.player();hero.put("equips",db.equippedItems());hero.put("equipment",db.equippedItems());hero.put("attackMin",Math.max(1,hero.optInt("attack",1)-2));hero.put("attackMax",hero.optInt("attack",1)+2);hero.put("maxHp",Math.max(1,hero.optInt("maxHp",100)));hero.put("hp",Math.max(1,hero.optInt("hp",100)));state.put("heroes",new JSONArray().put(hero));state.put("monsters",new JSONArray().put(m));state.put("mqtt",new JSONObject().put("wsUrl","ws://127.0.0.1:8058/afk/ws"));ok(s,state);return;}
        if(path.equals("/afk/battle/stop")){ok(s,db.stopBattle());return;}
        if(path.equals("/afk/battle/manual")){ok(s,db.manualBattle());return;}
        if(path.equals("/afk/instance/enter")){String mapId=first(in,"mapId","instanceMapId");ok(s,new JSONObject().put("success",true).put("mapId",mapId).put("player",db.player()));return;}
        if(path.equals("/afk/instance/sweep")){db.addGold(50);ok(s,new JSONObject().put("success",true).put("rewards",new JSONObject().put("gold",50)).put("player",db.player()));return;}

        // Settings
        if(path.equals("/upinfo/settings")){if("POST".equals(method)){ok(s,db.saveSettings(in));}else{JSONObject st=db.settings();st.put("filter",new JSONObject().put("mode",st.optString("pickFilter","all")));ok(s,st);}return;}

        // Craft - persistent core enhancement, other formal subsystems return current state envelopes rather than 404
        if(path.equals("/craft/enhance/state")){ok(s,new JSONObject().put("state",new JSONObject().put("category","enhance")).put("inventory",db.bag("bag")).put("slotItem",JSONObject.NULL));return;}
        if(path.equals("/craft/enhance")){String id=first(in,"toolId","id","itemId");JSONObject item=db.enhance(id);ok(s,new JSONObject().put("success",item.length()>0).put("item",item).put("inventory",db.bag("bag")));return;}
        if(path.equals("/craft/enhance/preview-attrs")){String id=first(in,"toolId","id","itemId");JSONObject item=db.bagItem(id);ok(s,new JSONObject().put("item",item).put("nextEnhanceLevel",item.optInt("enhanceLevel",0)+1));return;}
        if(path.equals("/craft/probability")){ok(s,new JSONObject().put("successRate",1.0).put("localMode",true));return;}
        if(path.startsWith("/craft/")&&path.endsWith("/state")){ok(s,new JSONObject().put("inventory",db.bag("bag")).put("state",new JSONObject()).put("slotItem",JSONObject.NULL));return;}
        if(path.startsWith("/craft/")){error(s,501,"该合成接口尚未实现");return;}

        // Shop / market / activity
        if(path.equals("/shop/list")){ok(s,new JSONObject().put("items",GameContent.shop()).put("list",GameContent.shop()));return;}
        if(path.equals("/shop/buy")){int itemId=in.optInt("itemId",intVal(first(in,"id"),1001));JSONObject target=null;JSONArray items=GameContent.shop();for(int i=0;i<items.length();i++){JSONObject x=items.optJSONObject(i);if(x!=null&&x.optInt("itemId")==itemId){target=x;break;}}if(target==null){error(s,400,"商品不存在");return;}long price=target.optLong("price",0);JSONObject p=db.player();if(p.optLong("gold",0)<price){error(s,400,"金币不足");return;}db.addGold(-price);db.addItem(itemId,target.optString("name"),Math.max(1,target.optInt("count",1)),"item","",1);ok(s,new JSONObject().put("success",true).put("player",db.player()).put("bag",db.bag("bag")));return;}
        if(path.equals("/shop/ad/state")){ok(s,new JSONObject().put("available",false).put("cooldownSec",0));return;}
        if(path.equals("/shop/ad/reward")){ok(s,new JSONObject().put("success",false).put("message","本地模式不使用广告奖励"));return;}
        if(path.equals("/market/exchange/list")){ok(s,new JSONObject().put("listings",new JSONArray()).put("subCategories",new JSONArray()).put("backpackUsed",db.bag("bag").length()).put("backpackCapacity",300));return;}
        if(path.equals("/market/exchange/mine")){ok(s,new JSONObject().put("listings",new JSONArray()));return;}
        if(path.equals("/market/exchange/detail")){ok(s,new JSONObject());return;}
        if(path.equals("/market/exchange/buy")||path.equals("/market/exchange/cancel")){error(s,501,"本地交易所尚未实现");return;}
        if(path.equals("/market/rank")||path.equals("/rank")){JSONArray list=new JSONArray().put(new JSONObject().put("rank",1).put("name",db.player().optString("name","勇者")).put("level",db.player().optInt("level",1)));ok(s,new JSONObject().put("items",list).put("list",list).put("myRank",1));return;}
        if(path.equals("/market/redeem")){ok(s,new JSONObject().put("success",false).put("message","本地模式未配置兑换码"));return;}
        if(path.equals("/activity/list")){ok(s,new JSONObject().put("items",new JSONArray()).put("backpackUsed",db.bag("bag").length()).put("backpackCapacity",300));return;}
        if(path.equals("/activity/claim")){error(s,501,"活动奖励尚未实现");return;}if(path.equals("/activity/dismiss-notice")){ok(s,new JSONObject().put("success",true));return;}
        if(path.startsWith("/email/")){if(path.endsWith("/unread")){ok(s,new JSONObject().put("count",0));}else if(path.endsWith("/list")){ok(s,new JSONObject().put("items",new JSONArray()));}else ok(s,new JSONObject().put("success",true));return;}

        // GM
        if(path.equals("/gm/api/state")){okRaw(s,new JSONObject().put("ok",true).put("data",new JSONObject(db.snapshot())));return;}
        if(path.equals("/gm/api/player")&&"POST".equals(method)){JSONObject p=db.player();String name=first(in,"name");db.updatePlayer(name,intVal(first(in,"level"),p.optInt("level",1)),longVal(first(in,"exp"),p.optLong("exp",0)),longVal(first(in,"diamond"),p.optLong("diamond",0)),longVal(first(in,"gold"),p.optLong("gold",0)));db.setRechargeDiamond(longVal(first(in,"rechargeDiamond"),p.optLong("rechargeDiamond",0)));okRaw(s,new JSONObject().put("ok",true).put("player",db.player()));return;}
        if(path.equals("/gm/api/item")&&"POST".equals(method)){int id=intVal(first(in,"itemId"),1),count=intVal(first(in,"count"),1);db.setItem(id,first(in,"name"),count);okRaw(s,new JSONObject().put("ok",true).put("bag",db.bag("bag")));return;}
        if(path.equals("/gm/api/clear-bag")&&"POST".equals(method)){db.clearBag();okRaw(s,new JSONObject().put("ok",true));return;}
        if(path.equals("/gm/api/reset")&&"POST".equals(method)){db.reset();okRaw(s,new JSONObject().put("ok",true));return;}
        if(path.equals("/gm/api/battle-stop")&&"POST".equals(method)){db.stopBattle();okRaw(s,new JSONObject().put("ok",true).put("battle",db.battle()));return;}

        sendJson(s,404,new JSONObject().put("code",404).put("message","not found").put("path",path).toString());
    }

    private void ok(Socket s,Object data)throws IOException{JSONObject o=new JSONObject();try{o.put("code",0);o.put("message","ok");o.put("data",data);}catch(Exception ignored){}sendJson(s,200,o.toString());}
    private void okRaw(Socket s,JSONObject data)throws IOException{sendJson(s,200,data.toString());}
    private void error(Socket s,int code,String msg)throws IOException{JSONObject o=new JSONObject();try{o.put("code",code);o.put("message",msg);o.put("data",JSONObject.NULL);}catch(Exception ignored){}sendJson(s,code,o.toString());}
    private void sendJson(Socket s,int code,String body)throws IOException{send(s,code,"application/json; charset=utf-8",body);}
    private void send(Socket s,int code,String type,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);OutputStream out=s.getOutputStream();String status=code==200?"OK":code==204?"No Content":code==400?"Bad Request":code==404?"Not Found":code==501?"Not Implemented":"Error";String h="HTTP/1.1 "+code+" "+status+"\r\nContent-Type: "+type+"\r\nContent-Length: "+b.length+"\r\nConnection: close\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Methods: GET,POST,OPTIONS\r\nAccess-Control-Allow-Headers: Content-Type,Authorization\r\n\r\n";out.write(h.getBytes(StandardCharsets.UTF_8));if(b.length>0)out.write(b);out.flush();}
    private void redirect(Socket s,String loc)throws IOException{byte[] b="Redirecting".getBytes(StandardCharsets.UTF_8);OutputStream out=s.getOutputStream();String h="HTTP/1.1 302 Found\r\nLocation: "+loc+"\r\nContent-Length: "+b.length+"\r\nConnection: close\r\n\r\n";out.write(h.getBytes(StandardCharsets.UTF_8));out.write(b);out.flush();}
    private static Map<String,String> parseQuery(String q){Map<String,String> m=new HashMap<>();if(q==null||q.isEmpty())return m;for(String p:q.split("&")){int i=p.indexOf('=');String k=i>=0?p.substring(0,i):p,v=i>=0?p.substring(i+1):"";try{m.put(URLDecoder.decode(k,"UTF-8"),URLDecoder.decode(v,"UTF-8"));}catch(Exception ignored){}}return m;}
    private static JSONObject parseBody(String body,String contentType){if(body==null||body.trim().isEmpty())return new JSONObject();try{if(body.trim().startsWith("{")||body.trim().startsWith("["))return new JSONObject(body);}catch(Exception ignored){}JSONObject o=new JSONObject();for(Map.Entry<String,String> e:parseQuery(body).entrySet())try{o.put(e.getKey(),e.getValue());}catch(Exception ignored){}return o;}
    private static String first(JSONObject o,String...keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&v!=JSONObject.NULL){String s=String.valueOf(v);if(!s.trim().isEmpty())return s;}}return null;}
    private static int intVal(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}
    private static long longVal(String s,long d){try{return Long.parseLong(s);}catch(Exception e){return d;}}
    private void log(String x){if(logger!=null)logger.log(x);}

    private static String gmHtml(){return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'><title>勇者之路 GM</title><style>*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f1f5f9;color:#0f172a;margin:0;padding:calc(env(safe-area-inset-top) + 16px) 14px 28px}.head{margin-bottom:14px}.head h2{margin:0 0 6px;font-size:22px}.muted{color:#64748b;font-size:13px}.card{background:#fff;padding:16px;border-radius:14px;margin-bottom:14px;box-shadow:0 1px 3px #0001}h3{margin:0 0 12px;font-size:17px}label{display:block;margin:9px 0 5px;font-size:14px;color:#334155}input{width:100%;padding:11px;border:1px solid #cbd5e1;border-radius:9px;font-size:16px}button{padding:11px 14px;border:0;border-radius:9px;margin:10px 8px 0 0;font-size:15px}.p{background:#2563eb;color:#fff}.s{background:#e2e8f0}.d{background:#b91c1c;color:#fff}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.badge{display:inline-block;padding:7px 10px;border-radius:999px;background:#dcfce7;color:#166534;font-size:13px;margin-right:6px}pre{white-space:pre-wrap;word-break:break-word;background:#0f172a;color:#e2e8f0;padding:12px;border-radius:10px;max-height:300px;overflow:auto}@media(max-width:520px){.grid{grid-template-columns:1fr}}</style></head><body><div class='head'><h2>勇者之路 · GM 管理后台</h2><div class='muted'>正式本地服务 · SQLite 持久化</div></div><div class='card'><h3>服务状态</h3><span class='badge'>HTTP 9055</span><span class='badge'>WS 8058</span><button class='s' id='refresh'>刷新</button></div><div class='card'><h3>角色</h3><form id='pf'><label>角色名</label><input name='name' id='n'><div class='grid'><div><label>等级</label><input name='level' type='number' min='1' id='lv'></div><div><label>经验</label><input name='exp' type='number' min='0' id='xp'></div><div><label>钻石</label><input name='diamond' type='number' min='0' id='dia'></div><div><label>第二种钻石（充值钻石）</label><input name='rechargeDiamond' type='number' min='0' id='rechargeDia'></div><div><label>金币</label><input name='gold' type='number' min='0' id='gold'></div></div><button class='p'>保存角色</button></form></div><div class='card'><h3>背包</h3><form id='itemForm'><div class='grid'><div><label>物品 ID</label><input name='itemId' type='number' min='1' required></div><div><label>数量</label><input name='count' type='number' min='0' required></div></div><label>名称</label><input name='name'><button class='p'>保存物品</button></form><button class='d' id='clear'>清空背包</button></div><div class='card'><h3>挂机战斗</h3><button class='d' id='stopBattle'>停止当前战斗</button></div><div class='card'><h3>数据快照</h3><pre id='out'>读取中...</pre></div><div class='card'><h3>维护</h3><button class='d' id='reset'>重置本地存档</button></div><script>const out=document.getElementById('out');async function api(u,o){const r=await fetch(u,o);return await r.json()}function enc(f){return new URLSearchParams(new FormData(f)).toString()}async function load(){try{const s=await api('/gm/api/state');const d=s.data||{};out.textContent=JSON.stringify(d,null,2);const p=d.player||{};n.value=p.name||'';lv.value=p.level||1;xp.value=p.exp||0;dia.value=p.diamond||0;rechargeDia.value=p.rechargeDiamond||0;gold.value=p.gold||0}catch(e){out.textContent='读取失败 '+e}}pf.onsubmit=async e=>{e.preventDefault();await api('/gm/api/player',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:enc(pf)});load()};itemForm.onsubmit=async e=>{e.preventDefault();await api('/gm/api/item',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:enc(itemForm)});load()};refresh.onclick=load;clear.onclick=async()=>{if(confirm('确认清空背包？')){await api('/gm/api/clear-bag',{method:'POST'});load()}};stopBattle.onclick=async()=>{await api('/gm/api/battle-stop',{method:'POST'});load()};reset.onclick=async()=>{if(confirm('确认重置全部本地存档？')){await api('/gm/api/reset',{method:'POST'});load()}};load()</script></body></html>";}
}
