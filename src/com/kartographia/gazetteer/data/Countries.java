package com.kartographia.gazetteer.data;

import java.util.*;
import javaxt.express.utils.CSV;

//******************************************************************************
//**  Countries
//******************************************************************************
/**
 *   Used to represent a list of countries
 *
 ******************************************************************************/

public class Countries {

    private HashMap<String, javaxt.utils.Record> records;
    private HashMap<String, String> fipsToISO;
    private HashMap<String, String> iso3;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate this class using a csv file with a list of countries
   *  like one found in the data directory (e.g. "countries.csv")
   */
    public Countries(javaxt.io.File countries) throws Exception {
        records = new HashMap<>();
        fipsToISO = new HashMap<>();
        iso3 = new HashMap<>();

        try(java.io.InputStream is = countries.getInputStream()){
            String header = CSV.readLine(is); //skip header
            //boolean v1 = header.startsWith("CC,Name,");
            String row;
            while (!(row=CSV.readLine(is)).isEmpty()){
                CSV.Columns columns = CSV.getColumns(row, ",");
                String iso2 = get(0, columns);
                String iso3 = get(1, columns);
                String fips = get(2, columns);
                String name = get(3, columns);
                String admin = get(4, columns);
                String region = get(5, columns);
                javaxt.utils.Record record = new javaxt.utils.Record();
                record.set("name", name);
                record.set("fips", fips);
                record.set("iso2", iso2);
                record.set("iso3", iso3);
                record.set("region", region);
                if (fips!=null) fipsToISO.put(fips, iso2);
                if (iso3!=null) this.iso3.put(iso3, iso2);
                records.put(iso2, record);
            }
        }
    }


  //**************************************************************************
  //** getFIPSCode
  //**************************************************************************
  /** Returns a FIPS country code for a given ISO code
   *  @param iso 2 character ISO code
   */
    public String getFIPSCode(String iso){
        return records.get(iso).get("fips").toString();
    }


  //**************************************************************************
  //** getISOCode
  //**************************************************************************
  /** Returns a 2 character ISO country code for a given FIPS code
   *  @param fips 2 character FIPS code
   */
    public String getISOCode(String fips){
        return fipsToISO.get(fips);
    }


  //**************************************************************************
  //** getCountryByFIPSCode
  //**************************************************************************
  /** Returns a country record for a given FIPS code
   *  @param fips 2 character FIPS code
   */
    public javaxt.utils.Record getCountryByFIPSCode(String fips){
        return records.get(fipsToISO.get(fips));
    }


  //**************************************************************************
  //** getCountryByISOCode
  //**************************************************************************
  /** Returns a country record for a given ISO code
   *  @param iso 2 character ISO code
   */
    public javaxt.utils.Record getCountryByISOCode(String iso){
        return records.get(iso);
    }


  //**************************************************************************
  //** getISO3
  //**************************************************************************
  /** Converts a 2 character ISO code to a 3 character code
   *  @param iso2 2 character ISO code
   */
    public String getISO3(String iso2){
        return records.get(iso2).get("iso3").toString();
    }


  //**************************************************************************
  //** getISO2
  //**************************************************************************
  /** Converts a 3 character ISO code to a 2 character code
   *  @param iso3 3 character ISO code
   */
    public String getISO2(String iso3){
       return this.iso3.get(iso3);
    }


  //**************************************************************************
  //** get
  //**************************************************************************
    private String get(int idx, CSV.Columns columns){
        String str = columns.get(idx).toString();
        if (str!=null){
            str = str.trim();
            if (str.isEmpty()) str = null;
        }
        return str;
    }
}