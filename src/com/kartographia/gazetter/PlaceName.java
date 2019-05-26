package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  PlaceName Class
//******************************************************************************
/**
 *   Used to represent a PlaceName
 *
 ******************************************************************************/

public class PlaceName extends javaxt.sql.Model {

    private String name;
    private String uname;
    private String languageCode;
    private Integer type;
    private Place place;
    private JSONObject info;
    private Date lastModified;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public PlaceName(){
        super("gazetter.place_name", new java.util.HashMap<String, String>() {{
            
            put("name", "name");
            put("uname", "uname");
            put("languageCode", "language_code");
            put("type", "type");
            put("place", "place_id");
            put("info", "info");
            put("lastModified", "last_modified");

        }});
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public PlaceName(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  PlaceName.
   */
    public PlaceName(JSONObject json){
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
            Long placeID = getValue(rs, "place_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());
            this.lastModified = getValue(rs, "last_modified").toDate();



          //Set place
            if (placeID!=null) place = new Place(placeID);

        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another PlaceName.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.name = json.get("name").toString();
        this.uname = json.get("uname").toString();
        this.languageCode = json.get("languageCode").toString();
        this.type = json.get("type").toInteger();
        if (json.has("place")){
            place = new Place(json.get("place").toJSONObject());
        }
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

    public Place getPlace(){
        return place;
    }

    public void setPlace(Place place){
        this.place = place;
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
  /** Used to find a PlaceName using a given set of constraints. Example:
   *  PlaceName obj = PlaceName.get("name=", name);
   */
    public static PlaceName get(Object...args) throws SQLException {
        Object obj = _get(PlaceName.class, args);
        return obj==null ? null : (PlaceName) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find PlaceNames using a given set of constraints.
   */
    public static PlaceName[] find(Object...args) throws SQLException {
        Object[] obj = _find(PlaceName.class, args);
        PlaceName[] arr = new PlaceName[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (PlaceName) obj[i];
        }
        return arr;
    }
}