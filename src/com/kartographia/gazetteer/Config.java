package com.kartographia.gazetteer;
import javaxt.express.utils.DbUtils;
import javaxt.json.*;
import javaxt.sql.*;
import java.util.*;

//******************************************************************************
//**  Config Class
//******************************************************************************
/**
 *   Provides static methods used get/set configuration parameters
 *
 ******************************************************************************/

public class Config {

    private static javaxt.express.Config config = new javaxt.express.Config();
    private Config(){}


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to load a config file (JSON) and update config settings
   */
    public static void load(javaxt.io.File configFile, javaxt.io.Jar jar) throws Exception {


      //Parse config file
        JSONObject json = new JSONObject(configFile.getText());


      //Get database config
        JSONObject dbConfig = json.get("database").toJSONObject();
        javaxt.io.File schemaFile = null;
        if (dbConfig.has("schema")){
            updateFile("schema", dbConfig, configFile);
            schemaFile = new javaxt.io.File(dbConfig.get("schema").toString());
            dbConfig.remove("schema");
        }
        javaxt.io.File updateFile = null;
        if (dbConfig.has("updates")){
            updateFile("updates", dbConfig, configFile);
            updateFile = new javaxt.io.File(dbConfig.get("updates").toString());
            dbConfig.remove("updates");
        }
        String tablespace = dbConfig.get("tablespace").toString();
        dbConfig.remove("tablespace");


      //Update relative paths in the web config
        JSONObject webConfig = json.get("webserver").toJSONObject();
        updateDir("webDir", webConfig, configFile, false);
        updateDir("logDir", webConfig, configFile, true);
        updateDir("jobDir", webConfig, configFile, true);
        updateDir("scriptDir", webConfig, configFile, false);
        updateFile("keystore", webConfig, configFile);


      //Load config
        config.init(json);



      //Add additional properties to the config
        config.set("jar", jar);
        config.set("configFile", configFile);
        Properties props = config.getDatabase().getProperties();
        if (props==null){
            props = new Properties();
            config.getDatabase().setProperties(props);
        }
        props.put("schema", schemaFile);
        if (updateFile!=null) props.put("updates", updateFile);
        if (tablespace!=null) props.put("tablespace", tablespace);
    }


  //**************************************************************************
  //** initDatabase
  //**************************************************************************
  /** Used to initialize the database
   */
    public static void initDatabase() throws Exception {
        Database database = config.getDatabase();


      //Get database schema
        String schema = null;
        Object obj = config.getDatabase().getProperties().get("schema");
        if (obj!=null){
            javaxt.io.File schemaFile = (javaxt.io.File) obj;
            if (schemaFile.exists()){
                schema = schemaFile.getText();
            }
        }
        if (schema==null) throw new Exception("Schema not found");


      //Get updates
        obj = config.getDatabase().getProperties().get("updates");
        if (obj!=null){
            javaxt.io.File updateFile = (javaxt.io.File) obj;
            if (updateFile.exists()){
                schema += "\r\n" + updateFile.getText();
            }
        }


      //Get tablespace
        obj = config.getDatabase().getProperties().get("tablespace");
        String tablespace = obj==null ? null : obj.toString();



      //Initialize schema (create tables, indexes, etc)
        DbUtils.initSchema(database, schema, tablespace);


      //Inititalize connection pool
        database.initConnectionPool();


      //Initialize models
        javaxt.io.Jar jar = (javaxt.io.Jar) config.get("jar").toObject();
        Model.init(jar, database.getConnectionPool());


      //Create sources as needed
        for (String name : new String[]{"NGA", "USGS", "VLIZ", "OSM", "PB"}){
            Source source = Source.get("name=",name);
            if (source==null){
                source = new Source();
                source.setName(name);
                source.save();
            }
        }
    }


  //**************************************************************************
  //** has
  //**************************************************************************
  /** Returns true if the config has a given key.
   */
    public static boolean has(String key){
        return config.has(key);
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value for a given key.
   */
    public static JSONValue get(String key){
        return config.get(key);
    }


  //**************************************************************************
  //** set
  //**************************************************************************
    public static void set(String key, Object value){
        config.set(key, value);
    }


  //**************************************************************************
  //** getDatabase
  //**************************************************************************
    public static javaxt.sql.Database getDatabase(){
        return config.getDatabase();
    }


  //**************************************************************************
  //** getBaseMaps
  //**************************************************************************
    public static JSONArray getBaseMaps(){
        return config.get("basemaps").toJSONArray();
    }


  //**************************************************************************
  //** getFile
  //**************************************************************************
  /** Returns a File for a given path
   *  @param path Full canonical path to a file or a relative path (relative
   *  to the jarFile)
   */
    public static javaxt.io.File getFile(String path, javaxt.io.File jarFile){
        javaxt.io.File file = new javaxt.io.File(path);
        if (!file.exists()){
            file = new javaxt.io.File(jarFile.MapPath(path));
        }
        return file;
    }

    private static javaxt.io.File getFile(String path, java.io.File jarFile){
        return getFile(path, new javaxt.io.File(jarFile));
    }



  //**************************************************************************
  //** updateDir
  //**************************************************************************
  /** Used to update a path to a directory defined in a config file. Resolves
   *  both canonical and relative paths (relative to the configFile).
   */
    public static void updateDir(String key, JSONObject config, javaxt.io.File configFile, boolean create){
        if (config.has(key)){
            String path = config.get(key).toString();
            if (path==null){
                config.remove(key);
            }
            else{
                path = path.trim();
                if (path.length()==0){
                    config.remove(key);
                }
                else{

                    javaxt.io.Directory dir = new javaxt.io.Directory(path);
                    if (dir.exists()){
                        try{
                            java.io.File f = new java.io.File(path);
                            javaxt.io.Directory d = new javaxt.io.Directory(f.getCanonicalFile());
                            if (!dir.toString().equals(d.toString())){
                                dir = d;
                            }
                        }
                        catch(Exception e){
                        }
                    }
                    else{
                        dir = new javaxt.io.Directory(new java.io.File(configFile.MapPath(path)));
                    }


                    if (!dir.exists() && create) dir.create();


                    if (dir.exists()){
                        config.set(key, dir.toString());
                    }
                    else{
                        config.remove(key);
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** updateFile
  //**************************************************************************
  /** Used to update a path to a file defined in a config file. Resolves
   *  both canonical and relative paths (relative to the configFile).
   */
    public static void updateFile(String key, JSONObject config, javaxt.io.File configFile){
        if (config.has(key)){
            String path = config.get(key).toString();
            if (path==null){
                config.remove(key);
            }
            else{
                path = path.trim();
                if (path.length()==0){
                    config.remove(key);
                }
                else{

                    javaxt.io.File file = new javaxt.io.File(path);
                    if (file.exists()){
                        try{
                            java.io.File f = new java.io.File(path);
                            javaxt.io.File _file = new javaxt.io.File(f.getCanonicalFile());
                            if (!file.toString().equals(_file.toString())){
                                file = _file;
                            }
                        }
                        catch(Exception e){
                        }
                    }
                    else{
                        file = new javaxt.io.File(configFile.MapPath(path));
                    }

                    config.set(key, file.toString());
//                    if (file.exists()){
//                        config.set(key, file.toString());
//                    }
//                    else{
//                        config.remove(key);
//                    }
                }
            }
        }
    }
}