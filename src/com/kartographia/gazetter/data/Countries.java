package com.kartographia.gazetter.data;

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

    private HashMap<String, javaxt.utils.Record> records = new HashMap<>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate this class using a csv file with a list of countries
   *  like one found in the data directory (e.g. "countries.csv")
   */
    public Countries(javaxt.io.File countries) throws Exception {


        try(java.io.InputStream is = countries.getInputStream()){
            String header = CSV.readLine(is); //skip header
            String row;
            while (!(row=CSV.readLine(is)).isEmpty()){
                CSV.Columns columns = CSV.getColumns(row, ",");
                String fips = columns.get(0).toString();
                String name = columns.get(1).toString();
                String iso = columns.get(2).toString();
                String region = columns.get(3).toString();
                javaxt.utils.Record record = new javaxt.utils.Record();
                record.set("name", name);
                record.set("fips", fips);
                record.set("iso", iso);
                record.set("region", region);
                records.put(fips, record);
            }
        }
    }

    public javaxt.utils.Record getCountry(String fips){
        return records.get(fips);
    }

}