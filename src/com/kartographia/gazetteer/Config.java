package com.kartographia.gazetteer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javaxt.sql.*;
import javaxt.json.*;

import javaxt.express.utils.DbUtils;
import static javaxt.express.ConfigFile.updateDir;
import static javaxt.express.ConfigFile.updateFile;


//******************************************************************************
//**  Config Class
//******************************************************************************
/**
 *   Provides static methods used get/set configuration parameters
 *
 ******************************************************************************/

public class Config {

    private static javaxt.express.Config config = new javaxt.express.Config();
    private static AtomicBoolean dbInitialized = new AtomicBoolean(false);
    protected Config(){}


  //**************************************************************************
  //** load
  //**************************************************************************
  /** Used to load a config file (JSON) and update config settings
   */
    public static void load(javaxt.io.File configFile) throws Exception {


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


      //Load config
        config.init(json);


      //Add additional properties to the config
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
  //** getDatabase
  //**************************************************************************
    public static Database getDatabase() throws Exception{
      //Get database
        Database database = config.getDatabase();
        if (database==null) throw new Exception("Invalid database");


      //Initialize the database as needed
        synchronized(dbInitialized){
            boolean initialized = dbInitialized.get();
            if (!initialized){

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


              //Enable database caching
                database.enableMetadataCache(true);


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


              //Update status
                dbInitialized.set(true);

            }
        }

        return database;
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

}