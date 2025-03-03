package com.kartographia.gazetteer.source;

import com.kartographia.gazetteer.Name;
import com.kartographia.gazetteer.Place;
import com.kartographia.gazetteer.Source;
import com.kartographia.gazetteer.data.CountryCodes;
import com.kartographia.gazetteer.data.CountryNames;
import com.kartographia.gazetteer.utils.Counter;

import java.util.*;

import javaxt.sql.Database;
import static javaxt.utils.Console.console;


public class Kartographia {

    private static Source source;


  //**************************************************************************
  //** init
  //**************************************************************************
    private static void init(){
        if (source==null);
        try{
            String name = "Kartographia";
            source = Source.get("name=",name);
            if (source==null){
                source = new Source();
                source.setName(name);
                source.save();
            }
        }
        catch(Exception e){}
    }


  //**************************************************************************
  //** load
  //**************************************************************************
    public static void load(CountryNames countryNames, CountryCodes countryCodes,
        Database database) throws Exception {

        init();


      //Group country names by country code
        HashMap<String, HashSet<String>> countries = new HashMap<>();
        LinkedHashMap<String, String> list = countryNames.getList();
        for (String countryName : list.keySet()){
            String fips = list.get(countryName);
            String iso2 = countryCodes.getISOCode(fips);
            if (iso2==null) continue;

            HashSet<String> names = countries.get(iso2);
            if (names==null){
                names = new HashSet<>();
                countries.put(iso2, names);
            }
            names.add(countryName);
        }


        long numRecords = 0;
        for (String cc : countries.keySet()){
            numRecords += countries.get(cc).size();
        }


        Counter counter = new Counter(numRecords);


      //Add records
        for (String cc : countries.keySet()){

            String type = "boundary";
            String subtype = "country";
            Place place = Place.get("country_code=", cc, "type=", type, "subtype=", subtype);


          //If place doesn't exist, don't add names. Simply update count...
            if (place==null){
                for (String name : countries.get(cc)){
                    console.log("Skipping " + name + " (" + cc + ")...");
                    counter.updateCount();
                }
                continue;
            }



            HashSet<String> names = countries.get(cc);


            String formalName = countryCodes.getCountryByISOCode(cc).get("name").toString();
            boolean countFormalName = true;
            if (!names.contains(formalName)){
                names.add(formalName);
                countFormalName = false;
            }


            for (String name : names){
                String uname = name.toUpperCase(Locale.US);



                Name placeName = Name.get("place_id=", place.getID(), "uname=", uname);
                if (placeName==null){
                    placeName = new Name();
                    placeName.setName(name);
                    placeName.setUname(uname);
                    placeName.setLanguageCode("eng");
                    placeName.setType(name.equalsIgnoreCase(formalName) ? 2 : 3); //2=formal
                    placeName.setSource(source);
                    placeName.setPlace(place);

                    try{
                        placeName.save();
                    }
                    catch(Exception e){
                        //console.log(e.getMessage());
                    }
                }



                boolean updateCount = true;
                if (name.equalsIgnoreCase(formalName)){
                    if (!countFormalName) updateCount = false;
                }
                if (updateCount) counter.updateCount();
            }
        }

        counter.stop();
    }

}