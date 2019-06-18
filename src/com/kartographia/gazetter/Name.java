package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  Name Class
//******************************************************************************
/**
 *   Used to represent a Name
 *
 ******************************************************************************/

public class Name extends javaxt.sql.Model {

    private String name;
    private String uname;
    private String languageCode;
    private Integer type;
    private Source source;
    private Long sourceKey;
    private Integer sourceDate;
    private JSONObject info;
    private Date lastModified;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Name(){
        super("gazetter.name", new java.util.HashMap<String, String>() {{
            
            put("name", "name");
            put("uname", "uname");
            put("languageCode", "language_code");
            put("type", "type");
            put("source", "source_id");
            put("sourceKey", "source_key");
            put("sourceDate", "source_date");
            put("info", "info");
            put("lastModified", "last_modified");

        }});
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Name(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Name.
   */
    public Name(JSONObject json){
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
            this.uname = getValue(rs, "uname").toString();
            this.languageCode = getValue(rs, "language_code").toString();
            this.type = getValue(rs, "type").toInteger();
            Long sourceID = getValue(rs, "source_id").toLong();
            this.sourceKey = getValue(rs, "source_key").toLong();
            this.sourceDate = getValue(rs, "source_date").toInteger();
            this.info = new JSONObject(getValue(rs, "info").toString());
            this.lastModified = getValue(rs, "last_modified").toDate();



          //Set source
            if (sourceID!=null) source = new Source(sourceID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Name.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.uname = json.get("uname").toString();
        this.languageCode = json.get("languageCode").toString();
        this.type = json.get("type").toInteger();
        if (json.has("source")){
            source = new Source(json.get("source").toJSONObject());
        }
        this.sourceKey = json.get("sourceKey").toLong();
        this.sourceDate = json.get("sourceDate").toInteger();
        this.info = json.get("info").toJSONObject();
        this.lastModified = json.get("lastModified").toDate();
    }


    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getUname(){
        return uname;
    }

    public void setUname(String uname){
        this.uname = uname;
    }

    public String getLanguageCode(){
        return languageCode;
    }

    public void setLanguageCode(String languageCode){
        this.languageCode = languageCode;
    }

    public Integer getType(){
        return type;
    }

    public void setType(Integer type){
        this.type = type;
    }

    public Source getSource(){
        return source;
    }

    public void setSource(Source source){
        this.source = source;
    }

    public Long getSourceKey(){
        return sourceKey;
    }

    public void setSourceKey(Long sourceKey){
        this.sourceKey = sourceKey;
    }

    public Integer getSourceDate(){
        return sourceDate;
    }

    public void setSourceDate(Integer sourceDate){
        this.sourceDate = sourceDate;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }

    public Date getLastModified(){
        return lastModified;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Name using a given set of constraints. Example:
   *  Name obj = Name.get("name=", name);
   */
    public static Name get(Object...args) throws SQLException {
        Object obj = _get(Name.class, args);
        return obj==null ? null : (Name) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Names using a given set of constraints.
   */
    public static Name[] find(Object...args) throws SQLException {
        Object[] obj = _find(Name.class, args);
        Name[] arr = new Name[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Name) obj[i];
        }
        return arr;
    }
}