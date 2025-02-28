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


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate this class using a csv file with a list of countries
   *  like one found in the data directory (e.g. "countries.csv")
   */
    public Countries(javaxt.io.File countries) throws Exception {
        records = new HashMap<>();
        fipsToISO = new HashMap<>();

        try(java.io.InputStream is = countries.getInputStream()){
            String header = CSV.readLine(is); //skip header
            //boolean v1 = header.startsWith("CC,Name,");
            String row;
            while (!(row=CSV.readLine(is)).isEmpty()){
                CSV.Columns columns = CSV.getColumns(row, ",");
                String iso2 = columns.get(0).toString();
                String iso3 = columns.get(1).toString();
                String fips = columns.get(2).toString();
                String name = columns.get(3).toString();
                String admin = columns.get(4).toString();
                String region = columns.get(5).toString();
                javaxt.utils.Record record = new javaxt.utils.Record();
                record.set("name", name);
                record.set("fips", fips);
                record.set("iso2", iso2);
                record.set("iso3", iso3);
                record.set("region", region);
                fipsToISO.put(fips, iso2);
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
}