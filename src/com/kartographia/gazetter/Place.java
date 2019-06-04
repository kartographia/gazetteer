package com.kartographia.gazetter;
import javaxt.json.*;
import java.sql.SQLException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import java.util.ArrayList;
import javaxt.utils.Date;

//******************************************************************************
//**  Place Class
//******************************************************************************
/**
 *   Used to represent a Place
 *
 ******************************************************************************/

public class Place extends javaxt.sql.Model {

    private String countryCode;
    private String admin1;
    private String admin2;
    private Geometry geom;
    private String type;
    private String subtype;
    private Integer rank;
    private Source source;
    private Long sourceKey;
    private Integer sourceDate;
    private JSONObject info;
    private Date lastModified;
    private ArrayList<Name> names;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Place(){
        super("gazetter.place", new java.util.HashMap<String, String>() {{

            put("countryCode", "country_code");
            put("admin1", "admin1");
            put("admin2", "admin2");
            put("geom", "geom");
            put("type", "type");
            put("subtype", "subtype");
            put("rank", "rank");
            put("source", "source_id");
            put("sourceKey", "source_key");
            put("sourceDate", "source_date");
            put("info", "info");
            put("lastModified", "last_modified");
            put("names", "names");

        }});
        names = new ArrayList<Name>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a record ID in the database.
   */
    public Place(long id) throws SQLException {
        this();
        init(id);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a JSON representation of a
   *  Place.
   */
    public Place(JSONObject json){
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
            this.countryCode = getValue(rs, "country_code").toString();
            this.admin1 = getValue(rs, "admin1").toString();
            this.admin2 = getValue(rs, "admin2").toString();
            this.geom = new WKTReader().read(getValue(rs, "geom").toString());
            this.type = getValue(rs, "type").toString();
            this.subtype = getValue(rs, "subtype").toString();
            this.rank = getValue(rs, "rank").toInteger();
            Long sourceID = getValue(rs, "source_id").toLong();
            this.sourceKey = getValue(rs, "source_key").toLong();
            this.sourceDate = getValue(rs, "source_date").toInteger();
            this.info = new JSONObject(getValue(rs, "info").toString());
            this.lastModified = getValue(rs, "last_modified").toDate();


            javaxt.sql.Connection conn = null;
            try{
                conn = getConnection(this.getClass());


              //Set names
                ArrayList<Long> nameIDs = new ArrayList<Long>();
                for (javaxt.sql.Recordset row : conn.getRecordset(
                    "select name_id from gazetter.place_name where place_id="+id)){
                    nameIDs.add(row.getValue(0).toLong());
                }
                for (long nameID : nameIDs){
                    names.add(new Name(nameID));
                }

                conn.close();
            }
            catch(SQLException e){
                if (conn!=null) conn.close();
                throw e;
            }



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
  /** Used to update attributes with attributes from another Place.
   */
    public void update(JSONObject json){

        Long id = json.get("id").toLong();
        if (id!=null && id>0) this.id = id;
        this.countryCode = json.get("countryCode").toString();
        this.admin1 = json.get("admin1").toString();
        this.admin2 = json.get("admin2").toString();
        try {
            this.geom = new WKTReader().read(json.get("geom").toString());
        }
        catch(Exception e) {}
        this.type = json.get("type").toString();
        this.subtype = json.get("subtype").toString();
        this.rank = json.get("rank").toInteger();
        if (json.has("source")){
            source = new Source(json.get("source").toJSONObject());
        }
        this.sourceKey = json.get("sourceKey").toLong();
        this.sourceDate = json.get("sourceDate").toInteger();
        this.info = json.get("info").toJSONObject();
        this.lastModified = json.get("lastModified").toDate();

      //Set names
        if (json.has("names")){
            JSONArray _names = json.get("names").toJSONArray();
            for (int i=0; i<_names.length(); i++){
                names.add(new Name(_names.get(i).toJSONObject()));
            }
        }
    }


    public String getCountryCode(){
        return countryCode;
    }

    public void setCountryCode(String countryCode){
        this.countryCode = countryCode;
    }

    public String getAdmin1(){
        return admin1;
    }

    public void setAdmin1(String admin1){
        this.admin1 = admin1;
    }

    public String getAdmin2(){
        return admin2;
    }

    public void setAdmin2(String admin2){
        this.admin2 = admin2;
    }

    public Geometry getGeom(){
        return geom;
    }

    public void setGeom(Geometry geom){
        this.geom = geom;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getSubtype(){
        return subtype;
    }

    public void setSubtype(String subtype){
        this.subtype = subtype;
    }

    public Integer getRank(){
        return rank;
    }

    public void setRank(Integer rank){
        this.rank = rank;
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

    public Name[] getNames(){
        return names.toArray(new Name[names.size()]);
    }

    public void setNames(Name[] arr){
        names = new ArrayList<Name>();
        for (int i=0; i<arr.length; i++){
            names.add(arr[i]);
        }
    }

    public void addName(Name name){
        this.names.add(name);
    }

  //**************************************************************************
  //** save
  //**************************************************************************
  /** Used to save a Place in the database.
   */
    public void save() throws SQLException {
        super.save();
        javaxt.sql.Connection conn = null;
        try{
            conn = getConnection(this.getClass());
            javaxt.sql.Recordset rs = new javaxt.sql.Recordset();

          //Save names
            ArrayList<Long> nameIDs = new ArrayList<Long>();
            for (Name obj : names){
                obj.save();
                nameIDs.add(obj.getID());
            }
            for (long nameID : nameIDs){
                rs.open("select * from gazetter.place_name where place_id=" + id +
                " and name_id=" + nameID, conn, false);
                if (rs.EOF){
                    rs.addNew();
                    rs.setValue("place_id", id);
                    rs.setValue("name_id", nameID);
                    rs.update();
                }
                rs.close();
            }


            conn.close();
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }
    }




  //**************************************************************************
  //** get
  //**************************************************************************
  /** Used to find a Place using a given set of constraints. Example:
   *  Place obj = Place.get("country_code=", country_code);
   */
    public static Place get(Object...args) throws SQLException {
        Object obj = _get(Place.class, args);
        return obj==null ? null : (Place) obj;
    }


  //**************************************************************************
  //** find
  //**************************************************************************
  /** Used to find Places using a given set of constraints.
   */
    public static Place[] find(Object...args) throws SQLException {
        Object[] obj = _find(Place.class, args);
        Place[] arr = new Place[obj.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Place) obj[i];
        }
        return arr;
    }
}