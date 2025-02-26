package com.kartographia.gazetteer;
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
        super("gazetteer.edit", java.util.Map.ofEntries(
            
            java.util.Map.entry("summary", "summary"),
            java.util.Map.entry("details", "details"),
            java.util.Map.entry("info", "info"),
            java.util.Map.entry("startDate", "start_date"),
            java.util.Map.entry("endDate", "end_date")

        ));
        
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
        return obj==null ? null : (Edit) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Edits using a given set of constraints.
   */
    public static Edit[] find(Object...args) throws SQLException {
        Object[] obj = _find(Edit.class, args);
        Edit[] arr = new Edit[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Edit) obj[i];
        }
        return arr;
    }
}