package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  ChangeRequest Class
//******************************************************************************
/**
 *   Used to represent a ChangeRequest
 *
 ******************************************************************************/

public class ChangeRequest extends javaxt.sql.Model {

    private Place place;
    private JSONObject info;
    private String status;
    private Date lastModified;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public ChangeRequest(){
        super("gazetter.change_request", new java.util.HashMap<String, String>() {{
            
            put("place", "place_id");
            put("info", "info");
            put("status", "status");
            put("lastModified", "last_modified");

        }});
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public ChangeRequest(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  ChangeRequest.
   */
    public ChangeRequest(JSONObject json){
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
            Long placeID = getValue(rs, "place_id").toLong();
            this.info = new JSONObject(getValue(rs, "info").toString());
            this.status = getValue(rs, "status").toString();
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
  /** Used to update attributes with attributes from another ChangeRequest.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        if (json.has("place")){
            place = new Place(json.get("place").toJSONObject());
        }
        this.info = json.get("info").toJSONObject();
        this.status = json.get("status").toString();
        this.lastModified = json.get("lastModified").toDate();
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

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public Date getLastModified(){
        return lastModified;
    }
    
    


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a ChangeRequest using a given set of constraints. Example:
   *  ChangeRequest obj = ChangeRequest.get("place_id=", place_id);
   */
    public static ChangeRequest get(Object...args) throws SQLException {
        Object obj = _get(ChangeRequest.class, args);
        return obj==null ? null : (ChangeRequest) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find ChangeRequests using a given set of constraints.
   */
    public static ChangeRequest[] find(Object...args) throws SQLException {
        Object[] obj = _find(ChangeRequest.class, args);
        ChangeRequest[] arr = new ChangeRequest[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (ChangeRequest) obj[i];
        }
        return arr;
    }
}