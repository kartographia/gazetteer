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
        System.out.println(sql);
        if (!sql.exists()) throw new Exception("Schema not found");
        String tableSpace = gazetteer.get("tablespace").toString();
        DbUtils.initSchema(database, sql.getText(), tableSpace);
        database.initConnectionPool();


      //Initialize models
        Model.init(jar, database.getConnectionPool());


      //Insert sources as needed
        Connection conn = null;
        try{
            conn = database.getConnection();

            Recordset rs = new Recordset();
            rs.open("select count(id) from gazetter.source", conn);
            int sources = rs.EOF ? 0 : rs.getValue(0).toInteger();
            rs.close();

            if (sources==0){
                for (String name : new String[]{"NGA", "USGS"}){
                    Source source = new Source();
                    source.setName(name);
                    source.save();
                }
            }
        }
        catch(Exception e){
            if (conn!=null) conn.close();
            throw e;
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