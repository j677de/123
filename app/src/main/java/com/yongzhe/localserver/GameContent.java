package com.yongzhe.localserver;
import org.json.JSONArray;
import org.json.JSONObject;

public final class GameContent {
    private GameContent() {}
    public static JSONArray maps(){JSONArray a=new JSONArray();try{a.put(map("map_1","勇者平原",1,1));a.put(map("map_2","迷雾森林",5,2));a.put(map("map_3","遗迹峡谷",10,3));}catch(Exception ignored){}return a;}
    private static JSONObject map(String id,String name,int level,int order)throws Exception{JSONObject o=new JSONObject();o.put("id",id);o.put("mapId",id);o.put("name",name);o.put("level",level);o.put("minLevel",level);o.put("order",order);o.put("enterGold",0);o.put("unlocked",true);return o;}
    public static JSONArray monsters(String mapId){JSONArray a=new JSONArray();try{if("map_2".equals(mapId)){a.put(mon("m201","森林狼",6,70,12,6));a.put(mon("m202","树妖",8,100,16,9));}else if("map_3".equals(mapId)){a.put(mon("m301","遗迹守卫",11,160,24,15));a.put(mon("m302","峡谷领主",15,260,35,24));}else{a.put(mon("m101","史莱姆",1,30,5,2));a.put(mon("m102","野狼",3,50,8,4));}}catch(Exception ignored){}return a;}
    private static JSONObject mon(String id,String name,int lv,int hp,int atk,int def)throws Exception{JSONObject o=new JSONObject();o.put("id",id);o.put("monsterId",id);o.put("name",name);o.put("level",lv);o.put("hp",hp);o.put("maxHp",hp);o.put("attack",atk);o.put("defense",def);return o;}
    public static JSONObject monster(String mapId,String monsterId){JSONArray a=monsters(mapId);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&monsterId.equals(o.optString("id")))return o;}return a.optJSONObject(0)==null?new JSONObject():a.optJSONObject(0);}
    public static JSONArray shop(){JSONArray a=new JSONArray();try{a.put(shopItem(1001,"小型生命药水",100,"gold",20));a.put(shopItem(1002,"小型魔法药水",100,"gold",20));a.put(shopItem(2001,"回城卷轴",500,"gold",1));}catch(Exception ignored){}return a;}
    private static JSONObject shopItem(int id,String name,long price,String currency,int count)throws Exception{JSONObject o=new JSONObject();o.put("id",String.valueOf(id));o.put("itemId",id);o.put("name",name);o.put("price",price);o.put("currency",currency);o.put("count",count);o.put("enabled",true);return o;}
}
