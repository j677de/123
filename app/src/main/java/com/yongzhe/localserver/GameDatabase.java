package com.yongzhe.localserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class GameDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "yongzhe_local.db";
    private static final int DB_VERSION = 5;

    public GameDatabase(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) { createSchema(db); seed(db); }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createSchema(db);
        addColumn(db,"player","user_id TEXT NOT NULL DEFAULT 'local-user'");
        addColumn(db,"player","upid TEXT NOT NULL DEFAULT 'local-up'");
        addColumn(db,"player","clazz TEXT NOT NULL DEFAULT 'warrior'");
        addColumn(db,"player","recharge_diamond INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"player","hp INTEGER NOT NULL DEFAULT 100");
        addColumn(db,"player","max_hp INTEGER NOT NULL DEFAULT 100");
        addColumn(db,"player","mp INTEGER NOT NULL DEFAULT 50");
        addColumn(db,"player","max_mp INTEGER NOT NULL DEFAULT 50");
        addColumn(db,"player","attack INTEGER NOT NULL DEFAULT 10");
        addColumn(db,"player","defense INTEGER NOT NULL DEFAULT 5");
        addColumn(db,"player","strength INTEGER NOT NULL DEFAULT 5");
        addColumn(db,"player","agility INTEGER NOT NULL DEFAULT 5");
        addColumn(db,"player","vitality INTEGER NOT NULL DEFAULT 5");
        addColumn(db,"player","energy INTEGER NOT NULL DEFAULT 5");
        addColumn(db,"player","stat_points INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"player","map_id TEXT NOT NULL DEFAULT 'map_1'");
        addColumn(db,"bag","char_id INTEGER NOT NULL DEFAULT 1");
        addColumn(db,"bag","type TEXT NOT NULL DEFAULT 'item'");
        addColumn(db,"bag","slot TEXT NOT NULL DEFAULT ''");
        addColumn(db,"bag","rarity INTEGER NOT NULL DEFAULT 1");
        addColumn(db,"bag","enhance INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"bag","equipped INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"bag","storage TEXT NOT NULL DEFAULT 'bag'");
        addColumn(db,"bag","attrs_json TEXT NOT NULL DEFAULT '{}'");
        seed(db);
    }

    private void createSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS player (id INTEGER PRIMARY KEY,user_id TEXT NOT NULL DEFAULT 'local-user',upid TEXT NOT NULL DEFAULT 'local-up',name TEXT NOT NULL,clazz TEXT NOT NULL DEFAULT 'warrior',level INTEGER NOT NULL,exp INTEGER NOT NULL,diamond INTEGER NOT NULL,gold INTEGER NOT NULL,recharge_diamond INTEGER NOT NULL DEFAULT 0,hp INTEGER NOT NULL DEFAULT 100,max_hp INTEGER NOT NULL DEFAULT 100,mp INTEGER NOT NULL DEFAULT 50,max_mp INTEGER NOT NULL DEFAULT 50,attack INTEGER NOT NULL DEFAULT 10,defense INTEGER NOT NULL DEFAULT 5,strength INTEGER NOT NULL DEFAULT 5,agility INTEGER NOT NULL DEFAULT 5,vitality INTEGER NOT NULL DEFAULT 5,energy INTEGER NOT NULL DEFAULT 5,stat_points INTEGER NOT NULL DEFAULT 0,map_id TEXT NOT NULL DEFAULT 'map_1')");
        db.execSQL("CREATE TABLE IF NOT EXISTS bag (id INTEGER PRIMARY KEY AUTOINCREMENT,char_id INTEGER NOT NULL DEFAULT 1,item_id INTEGER NOT NULL,name TEXT NOT NULL,count INTEGER NOT NULL,type TEXT NOT NULL DEFAULT 'item',slot TEXT NOT NULL DEFAULT '',rarity INTEGER NOT NULL DEFAULT 1,enhance INTEGER NOT NULL DEFAULT 0,equipped INTEGER NOT NULL DEFAULT 0,storage TEXT NOT NULL DEFAULT 'bag',attrs_json TEXT NOT NULL DEFAULT '{}')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bag_storage ON bag(storage,equipped)");
        db.execSQL("CREATE TABLE IF NOT EXISTS settings (k TEXT PRIMARY KEY,v TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS battle (id INTEGER PRIMARY KEY CHECK(id=1),active INTEGER NOT NULL DEFAULT 0,map_id TEXT NOT NULL DEFAULT 'map_1',monster_id TEXT NOT NULL DEFAULT '',monster_name TEXT NOT NULL DEFAULT '',monster_level INTEGER NOT NULL DEFAULT 1,started_at INTEGER NOT NULL DEFAULT 0,last_tick INTEGER NOT NULL DEFAULT 0,kills INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS gm_log (id INTEGER PRIMARY KEY AUTOINCREMENT,ts INTEGER NOT NULL,action TEXT NOT NULL,detail TEXT NOT NULL DEFAULT '')");
    }

    private void seed(SQLiteDatabase db) {
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM player",null); boolean empty=true; try{if(c.moveToFirst()) empty=c.getInt(0)==0;}finally{c.close();}
        if(empty) db.execSQL("INSERT INTO player(id,user_id,upid,name,clazz,level,exp,diamond,recharge_diamond,gold) VALUES(1,'local-user','local-up','勇者','warrior',1,0,100000,100000,0)");
        db.execSQL("INSERT OR IGNORE INTO battle(id,active,map_id) VALUES(1,0,'map_1')");
        putSetting(db,"music","1"); putSetting(db,"sound","1"); putSetting(db,"autoPotion","1"); putSetting(db,"autoRepair","1"); putSetting(db,"pickFilter","all");
    }
    private void addColumn(SQLiteDatabase db,String table,String def){ try{db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+def);}catch(Exception ignored){} }
    private void putSetting(SQLiteDatabase db,String k,String v){ ContentValues cv=new ContentValues();cv.put("k",k);cv.put("v",v);db.insertWithOnConflict("settings",null,cv,SQLiteDatabase.CONFLICT_IGNORE); }

    public synchronized JSONObject player() {
        Cursor c=getReadableDatabase().rawQuery("SELECT id,user_id,upid,name,clazz,level,exp,diamond,recharge_diamond,gold,hp,max_hp,mp,max_mp,attack,defense,strength,agility,vitality,energy,stat_points,map_id FROM player WHERE id=1",null);
        try{ JSONObject o=new JSONObject(); if(!c.moveToFirst()) return o;
            o.put("id",c.getInt(0));o.put("charId",String.valueOf(c.getInt(0)));o.put("userid",c.getString(1));o.put("userId",c.getString(1));o.put("upid",c.getString(2));o.put("name",c.getString(3));o.put("characterName",c.getString(3));o.put("class",c.getString(4));o.put("job",c.getString(4));o.put("level",c.getInt(5));o.put("exp",c.getLong(6));o.put("diamond",c.getLong(7));o.put("diamonds",c.getLong(7));o.put("boundDiamond",c.getLong(7));o.put("rechargeDiamond",c.getLong(8));o.put("gold",c.getLong(9));o.put("goldcoin",c.getLong(9));o.put("hp",c.getInt(10));o.put("maxHp",c.getInt(11));o.put("mp",c.getInt(12));o.put("maxMp",c.getInt(13));o.put("attack",c.getInt(14));o.put("defense",c.getInt(15));o.put("strength",c.getInt(16));o.put("agility",c.getInt(17));o.put("vitality",c.getInt(18));o.put("energy",c.getInt(19));o.put("statPoints",c.getInt(20));o.put("mapId",c.getString(21));return o;
        }catch(Exception e){return new JSONObject();}finally{c.close();}
    }

    public synchronized JSONArray characterList(boolean equipOnly) {
        JSONArray a=new JSONArray(); JSONObject p=player(); try{ if(p.length()==0)return a; p.put("equips",equippedItems()); p.put("equipment",equippedItems()); a.put(p);}catch(Exception ignored){} return a;
    }

    public synchronized JSONArray bag(String storage) {
        JSONArray a=new JSONArray(); Cursor c=getReadableDatabase().rawQuery("SELECT id,item_id,name,count,type,slot,rarity,enhance,equipped,storage,attrs_json FROM bag WHERE storage=? ORDER BY id",new String[]{storage});
        try{while(c.moveToNext()){JSONObject o=new JSONObject();o.put("id",String.valueOf(c.getInt(0)));o.put("toolId",String.valueOf(c.getInt(0)));o.put("itemId",c.getInt(1));o.put("templateId",c.getInt(1));o.put("name",c.getString(2));o.put("count",c.getInt(3));o.put("num",c.getInt(3));o.put("type",c.getString(4));o.put("slot",c.getString(5));o.put("rarity",c.getInt(6));o.put("enhance",c.getInt(7));o.put("enhanceLevel",c.getInt(7));o.put("equipped",c.getInt(8)!=0);o.put("storage",c.getString(9));try{o.put("attrs",new JSONObject(c.getString(10)));}catch(Exception x){o.put("attrs",new JSONObject());}a.put(o);}}catch(Exception ignored){}finally{c.close();}return a;
    }
    public synchronized JSONArray equippedItems(){ JSONArray all=bag("bag"),a=new JSONArray();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&o.optBoolean("equipped"))a.put(o);}return a; }
    public synchronized JSONObject bagPage(String storage,int offset,int limit){JSONArray all=bag(storage);JSONArray items=new JSONArray();for(int i=Math.max(0,offset);i<Math.min(all.length(),Math.max(0,offset)+Math.max(1,limit));i++)items.put(all.opt(i));JSONObject o=new JSONObject();try{o.put("items",items);o.put("total",all.length());o.put("offset",Math.max(0,offset));o.put("limit",Math.max(1,limit));o.put("capacity",300);o.put("used",all.length());}catch(Exception ignored){}return o;}

    public synchronized JSONObject bagItem(String toolId){ Cursor c=getReadableDatabase().rawQuery("SELECT id,item_id,name,count,type,slot,rarity,enhance,equipped,storage,attrs_json FROM bag WHERE id=?",new String[]{toolId});try{if(!c.moveToFirst())return new JSONObject();JSONObject o=new JSONObject();o.put("id",String.valueOf(c.getInt(0)));o.put("toolId",String.valueOf(c.getInt(0)));o.put("itemId",c.getInt(1));o.put("name",c.getString(2));o.put("count",c.getInt(3));o.put("type",c.getString(4));o.put("slot",c.getString(5));o.put("rarity",c.getInt(6));o.put("enhanceLevel",c.getInt(7));o.put("equipped",c.getInt(8)!=0);o.put("storage",c.getString(9));return o;}catch(Exception e){return new JSONObject();}finally{c.close();}}

    public synchronized long addItem(int itemId,String name,int count,String type,String slot,int rarity){if(count<=0)return -1;ContentValues v=new ContentValues();v.put("item_id",itemId);v.put("name",name==null||name.trim().isEmpty()?String.valueOf(itemId):name.trim());v.put("count",count);v.put("type",type==null?"item":type);v.put("slot",slot==null?"":slot);v.put("rarity",Math.max(1,rarity));v.put("storage","bag");return getWritableDatabase().insert("bag",null,v);}
    public synchronized void setItem(int itemId,String name,int count){SQLiteDatabase db=getWritableDatabase();Cursor c=db.rawQuery("SELECT id FROM bag WHERE item_id=? AND storage='bag' AND equipped=0 LIMIT 1",new String[]{String.valueOf(itemId)});try{if(count<=0){db.delete("bag","item_id=?",new String[]{String.valueOf(itemId)});return;}ContentValues v=new ContentValues();v.put("name",name==null||name.trim().isEmpty()?String.valueOf(itemId):name.trim());v.put("count",count);if(c.moveToFirst())db.update("bag",v,"id=?",new String[]{String.valueOf(c.getInt(0))});else{v.put("item_id",itemId);v.put("storage","bag");db.insert("bag",null,v);}}finally{c.close();}}
    public synchronized boolean moveItem(String toolId,String storage){ContentValues v=new ContentValues();v.put("storage",storage);v.put("equipped",0);return getWritableDatabase().update("bag",v,"id=? AND equipped=0",new String[]{toolId})>0;}
    public synchronized boolean equip(String toolId){JSONObject item=bagItem(toolId);String slot=item.optString("slot","");if(slot.isEmpty())slot=item.optString("type","");SQLiteDatabase db=getWritableDatabase();ContentValues off=new ContentValues();off.put("equipped",0);if(item.length()==0 || !"equipment".equals(item.optString("type")) || !"bag".equals(item.optString("storage")))return false;
        if(!slot.isEmpty())db.update("bag",off,"slot=? AND storage='bag' AND equipped=1",new String[]{slot});ContentValues on=new ContentValues();on.put("equipped",1);on.put("storage","bag");return db.update("bag",on,"id=?",new String[]{toolId})>0;}
    public synchronized boolean unequip(String toolId){ContentValues v=new ContentValues();v.put("equipped",0);return getWritableDatabase().update("bag",v,"id=?",new String[]{toolId})>0;}
    public synchronized void unequipAll(){ContentValues v=new ContentValues();v.put("equipped",0);getWritableDatabase().update("bag",v,null,null);}
    public synchronized boolean deleteItem(String toolId){return getWritableDatabase().delete("bag","id=?",new String[]{toolId})>0;}
    public synchronized long sellItem(String toolId,int amount){JSONObject i=bagItem(toolId);int have=i.optInt("count",0);if(have<=0||i.optBoolean("equipped"))return 0;int n=Math.max(1,Math.min(have,amount));long gold=n*Math.max(1,i.optInt("rarity",1))*10L;SQLiteDatabase db=getWritableDatabase();if(n>=have)db.delete("bag","id=?",new String[]{toolId});else{ContentValues v=new ContentValues();v.put("count",have-n);db.update("bag",v,"id=?",new String[]{toolId});}addGold(gold);return gold;}
    public synchronized long sellAll(){JSONArray a=bag("bag");long total=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&!o.optBoolean("equipped"))total+=Math.max(1,o.optInt("rarity",1))*10L*o.optInt("count",0);}getWritableDatabase().delete("bag","storage='bag' AND equipped=0",null);addGold(total);return total;}

    public synchronized void updatePlayer(String name,int level,long exp,long diamond,long gold){ContentValues v=new ContentValues();if(name!=null&&!name.trim().isEmpty())v.put("name",name.trim());v.put("level",Math.max(1,level));v.put("exp",Math.max(0,exp));v.put("diamond",Math.max(0,diamond));v.put("gold",Math.max(0,gold));getWritableDatabase().update("player",v,"id=1",null);}
    public synchronized void addGold(long n){getWritableDatabase().execSQL("UPDATE player SET gold=MAX(0,gold+?) WHERE id=1",new Object[]{n});}
    public synchronized void setRechargeDiamond(long amount){ContentValues v=new ContentValues();v.put("recharge_diamond",Math.max(0,amount));getWritableDatabase().update("player",v,"id=1",null);}
    public synchronized void addDiamond(long n){getWritableDatabase().execSQL("UPDATE player SET diamond=MAX(0,diamond+?) WHERE id=1",new Object[]{n});}
    public synchronized JSONObject allocate(String stat,int points){JSONObject p=player();int available=p.optInt("statPoints",0);int n=Math.max(0,Math.min(available,points));if(n==0)return p;String col="strength";if("agi".equals(stat)||"agility".equals(stat))col="agility";else if("vit".equals(stat)||"vitality".equals(stat))col="vitality";else if("ene".equals(stat)||"energy".equals(stat))col="energy";getWritableDatabase().execSQL("UPDATE player SET "+col+"="+col+"+?,stat_points=stat_points-? WHERE id=1",new Object[]{n,n});return player();}

    public synchronized JSONObject battle(){tickBattle();Cursor c=getReadableDatabase().rawQuery("SELECT active,map_id,monster_id,monster_name,monster_level,started_at,last_tick,kills FROM battle WHERE id=1",null);try{JSONObject o=new JSONObject();if(!c.moveToFirst())return o;boolean active=c.getInt(0)!=0;String mapId=c.getString(1),monsterId=c.getString(2);o.put("active",active);o.put("fighting",active);o.put("isFighting",active);o.put("battleKind","afk");o.put("mapId",mapId);o.put("currentMapId",active?mapId:"");o.put("monsterId",monsterId);o.put("currentMonsterId",active?monsterId:JSONObject.NULL);o.put("monsterName",c.getString(3));o.put("monsterLevel",c.getInt(4));o.put("startedAt",c.getLong(5));o.put("lastTick",c.getLong(6));o.put("kills",c.getInt(7));o.put("killCount",c.getInt(7));o.put("sessionTimeSec",active?Math.max(0,(System.currentTimeMillis()-c.getLong(5))/1000):0);o.put("battleSlots",new JSONArray());o.put("partySlots",new JSONArray());o.put("selectedMonsterSlotIndex",0);o.put("logs",new JSONArray());o.put("totalExp",0);o.put("totalGold",0);o.put("itemCount",0);o.put("normalEquipCount",0);o.put("excellentEquipCount",0);o.put("player",player());return o;}catch(Exception e){return new JSONObject();}finally{c.close();}}
    public synchronized JSONObject startBattle(String mapId,String monsterId,String monsterName,int monsterLevel){long now=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("active",1);v.put("map_id",mapId);v.put("monster_id",monsterId);v.put("monster_name",monsterName);v.put("monster_level",Math.max(1,monsterLevel));v.put("started_at",now);v.put("last_tick",now);v.put("kills",0);getWritableDatabase().update("battle",v,"id=1",null);ContentValues pv=new ContentValues();pv.put("map_id",mapId);getWritableDatabase().update("player",pv,"id=1",null);return battle();}
    public synchronized JSONObject stopBattle(){tickBattle();ContentValues v=new ContentValues();v.put("active",0);getWritableDatabase().update("battle",v,"id=1",null);return battle();}
    public synchronized JSONObject manualBattle(){tickBattle();Cursor c=getReadableDatabase().rawQuery("SELECT active FROM battle WHERE id=1",null);try{if(!c.moveToFirst()||c.getInt(0)==0)return battle();}finally{c.close();}rewardKill(1);return battle();}
    private void tickBattle(){Cursor c=getReadableDatabase().rawQuery("SELECT active,last_tick,monster_level FROM battle WHERE id=1",null);try{if(!c.moveToFirst()||c.getInt(0)==0)return;long now=System.currentTimeMillis(),last=c.getLong(1);int lv=c.getInt(2);int ticks=(int)Math.min(120,Math.max(0,(now-last)/5000));if(ticks>0){rewardKill(ticks);ContentValues v=new ContentValues();v.put("last_tick",ticks==120?now:last+ticks*5000L);getWritableDatabase().update("battle",v,"id=1",null);}}finally{c.close();}}
    private void rewardKill(int n){Cursor c=getReadableDatabase().rawQuery("SELECT monster_level FROM battle WHERE id=1",null);int lv=1;try{if(c.moveToFirst())lv=Math.max(1,c.getInt(0));}finally{c.close();}long exp=n*lv*8L,gold=n*lv*3L;SQLiteDatabase db=getWritableDatabase();db.execSQL("UPDATE battle SET kills=kills+? WHERE id=1",new Object[]{n});db.execSQL("UPDATE player SET exp=exp+?,gold=gold+? WHERE id=1",new Object[]{exp,gold});levelUp();}
    private void levelUp(){JSONObject p=player();int level=p.optInt("level",1);long exp=p.optLong("exp",0);boolean changed=false;while(exp>=level*100L){exp-=level*100L;level++;changed=true;}if(changed){ContentValues v=new ContentValues();v.put("level",level);v.put("exp",exp);v.put("stat_points",p.optInt("statPoints",0)+(level-p.optInt("level",1))*5);v.put("max_hp",100+(level-1)*10);v.put("hp",100+(level-1)*10);v.put("attack",10+(level-1)*2);v.put("defense",5+(level-1));getWritableDatabase().update("player",v,"id=1",null);}}

    public synchronized JSONObject settings(){JSONObject o=new JSONObject();Cursor c=getReadableDatabase().rawQuery("SELECT k,v FROM settings",null);try{while(c.moveToNext()){String k=c.getString(0),v=c.getString(1);if("1".equals(v)||"0".equals(v))o.put(k,"1".equals(v));else o.put(k,v);}}catch(Exception ignored){}finally{c.close();}return o;}
    public synchronized JSONObject saveSettings(JSONObject in){SQLiteDatabase db=getWritableDatabase();java.util.Iterator<String> it=in.keys();while(it.hasNext()){String k=it.next();if("upid".equals(k))continue;Object val=in.opt(k);ContentValues cv=new ContentValues();cv.put("k",k);cv.put("v",String.valueOf(val));db.insertWithOnConflict("settings",null,cv,SQLiteDatabase.CONFLICT_REPLACE);}return settings();}

    public synchronized JSONObject enhance(String toolId){JSONObject item=bagItem(toolId);if(item.length()==0)return item;int lvl=item.optInt("enhanceLevel",0)+1;ContentValues v=new ContentValues();v.put("enhance",lvl);getWritableDatabase().update("bag",v,"id=?",new String[]{toolId});return bagItem(toolId);}

    public synchronized void clearBag(){getWritableDatabase().delete("bag",null,null);}    
    public synchronized void reset(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("player",null,null);db.delete("bag",null,null);db.delete("battle",null,null);db.delete("settings",null,null);seed(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public synchronized String snapshot(){JSONObject o=new JSONObject();try{o.put("player",player());o.put("bag",bag("bag"));o.put("warehouse",bag("warehouse"));o.put("battle",battle());o.put("settings",settings());}catch(Exception ignored){}return o.toString();}
}
