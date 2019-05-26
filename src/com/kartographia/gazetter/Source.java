package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;


//******************************************************************************
//**  Source Class
//******************************************************************************
/**
 *   Used to represent a Source
 *
 ******************************************************************************/

public class Source extends javaxt.sql.Model {

    private String name;
    private String description;
    private JSONObject info;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Source(){
        super("gazetter.source", new java.util.HashMap<String, String>() {{
            
            put("name", "name");
            put("description", "description");
            put("info", "info");

        }});
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Source(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Source.
   */
    public Source(JSONObject json){
        this();
        update(json);
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes using a record in the database.
   */
    protected void update(Object rs) throws SQLException {

        try{
            this.id = getValue(rs, "id").toLong();
            this.name = getValue(rs, "name").toString();
            this.description = getValue(rs, "description").toString();
            this.info = new JSONObject(getValue(rs, "info").toString());


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Source.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.description = json.get("description").toString();
        this.info = json.get("info").toJSONObject();
    }


    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Source using a given set of constraints. Example:
   *  Source obj = Source.get("name=", name);
   */
    public static Source get(Object...args) throws SQLException {
        Object obj = _get(Source.class, args);
        return obj==null ? null : (Source) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Sources using a given set of constraints.
   */
    public static Source[] find(Object...args) throws SQLException {
        Object[] obj = _find(Source.class, args);
        Source[] arr = new Source[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Source) obj[i];
        }
        return arr;
    }
}