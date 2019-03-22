package com.kartographia.gazetter;
import javaxt.json.JSONObject;
import javaxt.json.JSONValue;

public class Config {

    private static JSONObject config;

    public static void init(JSONObject config){
        Config.config = config;
    }

  //**************************************************************************
  //** getDatabase
  //**************************************************************************
    public static javaxt.sql.Database getDatabase(){
        JSONValue val = get("database");
        if (val==null) return null;

        JSONObject json = val.toJSONObject();
        javaxt.sql.Database database = new javaxt.sql.Database();
        database.setDriver(json.get("driver").toString());
        database.setHost(json.get("host").toString());
        database.setName(json.get("name").toString());
        database.setUserName(json.get("username").toString());
        database.setPassword(json.get("password").toString());
        if (json.has("maxConnections")){
            database.setConnectionPoolSize(json.get("maxConnections").toInteger());
        }
        return database;

    }

    private static JSONValue get(String key){
        return config.get(key);
    }
}