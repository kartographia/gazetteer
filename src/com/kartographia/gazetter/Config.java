package com.kartographia.gazetter;
import javaxt.express.utils.DbUtils;
import javaxt.json.JSONObject;
import javaxt.sql.*;

//******************************************************************************
//**  Config Class
//******************************************************************************
/**
 *   Provides static methods used get/set configuration parameters
 *
 ******************************************************************************/

public class Config extends javaxt.express.Config {

    private Config(){}


  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to load a config file (JSON) and initialize the database
   */
    public static void init(JSONObject json, javaxt.io.Jar jar) throws Exception {

        Config.init(json);

      //Get database connection info
        Database database = getDatabase();


      //Initialize schema (create tables, indexes, etc)
        JSONObject gazetteer = get("schema").get("gazetter").toJSONObject();
        javaxt.io.File schema = new javaxt.io.File(gazetteer.get("path").toString());
        if (!schema.exists()) throw new Exception("Schema not found");
        String sql = schema.getText();
        javaxt.io.File updates = new javaxt.io.File(gazetteer.get("updates").toString());
        if (updates.exists()){
            sql += "\r\n";
            sql += updates.getText();
        }
        String tableSpace = gazetteer.get("tablespace").toString();
        DbUtils.initSchema(database, sql, tableSpace);


      //Inititalize connection pool
        database.initConnectionPool();


      //Initialize models
        Model.init(jar, database.getConnectionPool());


      //Create sources as needed
        for (String name : new String[]{"NGA", "USGS", "VLIZ", "OSM"}){
            Source source = Source.get("name=",name);
            if (source==null){
                source = new Source();
                source.setName(name);
                source.save();
            }
        }
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
    public static void updateDir(String key, JSONObject config, javaxt.io.File configFile){
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
                    if (!dir.exists()) dir = new javaxt.io.Directory(configFile.MapPath(path));

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
                    if (!file.exists()) file = new javaxt.io.File(configFile.MapPath(path));

                    if (file.exists()){
                        config.set(key, file.toString());
                    }
                    else{
                        config.remove(key);
                    }
                }
            }
        }
    }
}