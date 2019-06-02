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


      //Initialize database
        JSONObject gazetteer = get("schema").get("gazetter").toJSONObject();
        javaxt.io.File sql = getFile(gazetteer.get("path").toString(), jar.getFile());
        if (!sql.exists()) throw new Exception("Schema not found");
        String tableSpace = gazetteer.get("tablespace").toString();
        DbUtils.initSchema(database, sql.getText(), tableSpace);
        database.initConnectionPool();


      //Initialize models
        Model.init(jar, database.getConnectionPool());


      //Insert sources as needed
        for (String name : new String[]{"NGA", "USGS", "VLIZ"}){
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
    private static javaxt.io.File getFile(String path, javaxt.io.File jarFile){
        javaxt.io.File file = new javaxt.io.File(path);
        if (!file.exists()){
            file = new javaxt.io.File(jarFile.MapPath(path));
        }
        return file;
    }

    private static javaxt.io.File getFile(String path, java.io.File jarFile){
        return getFile(path, new javaxt.io.File(jarFile));
    }
}