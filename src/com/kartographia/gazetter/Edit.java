package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;
import javaxt.utils.Date;

//******************************************************************************
//**  Edit Class
//******************************************************************************
/**
 *   Used to represent a Edit
 *
 ******************************************************************************/

public class Edit extends javaxt.sql.Model {

    private String summary;
    private String details;
    private JSONObject info;
    private Date startDate;
    private Date endDate;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Edit(){
        super("edit", new java.util.HashMap<String, String>() {{
            
            put("summary", "summary");
            put("details", "details");
            put("info", "info");
            put("startDate", "start_date");
            put("endDate", "end_date");

        }});
        
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Edit(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Edit.
   */
    public Edit(JSONObject json){
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
            this.summary = getValue(rs, "summary").toString();
            this.details = getValue(rs, "details").toString();
            this.info = new JSONObject(getValue(rs, "info").toString());
            this.startDate = getValue(rs, "start_date").toDate();
            this.endDate = getValue(rs, "end_date").toDate();


        }
        catch(Exception e){
            if (e instanceof SQLException) throw (SQLException) e;
            else throw new SQLException(e.getMessage());
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to update attributes with attributes from another Edit.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.summary = json.get("summary").toString();
        this.details = json.get("details").toString();
        this.info = json.get("info").toJSONObject();
        this.startDate = json.get("startDate").toDate();
        this.endDate = json.get("endDate").toDate();
    }


    public String getSummary(){
        return summary;
    }

    public void setSummary(String summary){
        this.summary = summary;
    }

    public String getDetails(){
        return details;
    }

    public void setDetails(String details){
        this.details = details;
    }

    public JSONObject getInfo(){
        return info;
    }

    public void setInfo(JSONObject info){
        this.info = info;
    }

    public Date getStartDate(){
        return startDate;
    }

    public void setStartDate(Date startDate){
        this.startDate = startDate;
    }

    public Date getEndDate(){
        return endDate;
    }

    public void setEndDate(Date endDate){
        this.endDate = endDate;
    }
    
    



  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Edit using a given set of constraints. Example:
   *  Edit obj = Edit.get("summary=", summary);
   */
    public static Edit get(Object...args) throws SQLException {
        Object obj = _get(Edit.class, args);
        if (obj==null) return null;
        return (Edit) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Edits using a given set of constraints.
   */
    public static Edit[] find(Object...args) throws SQLException {
        Object[] arr = _find(Edit.class, args);
        if (arr==null) return null;
        return (Edit[]) arr;
    }
}