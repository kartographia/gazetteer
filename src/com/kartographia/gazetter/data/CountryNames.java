package com.kartographia.gazetter.data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//******************************************************************************
//**  CountryNames
//******************************************************************************
/**
 *   Used to represent a list of country names. Individual country names may
 *   have multiple variations (e.g. "United States" vs "United States of
 *   America" vs "USA"). These variations are all mapped to a common FIPS
 *   country code.
 *
 ******************************************************************************/

public class CountryNames {

    private LinkedHashMap<String, String> countryNames;
    private HashMap<String, String> suggestions;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to instantiate this class using a file with country names like one
   *  found in the data directory (e.g. "countries.txt")
   */
    public CountryNames(javaxt.io.File countryNames) throws Exception {
        parseNames(countryNames.getText().split("\n"));
    }


  //**************************************************************************
  //** getList
  //**************************************************************************
  /** Returns a sorted list of country names and their associated country
   *  codes.
   */
    public LinkedHashMap<String, String> getList(){
        return countryNames;
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
    public String toString(){
        StringBuilder str = new StringBuilder();
        Iterator<String> it = countryNames.keySet().iterator();
        while (it.hasNext()){
            String countryName = it.next();
            String countryCode = countryNames.get(countryName);
            str.append(countryName + ";" + countryCode);
            if (it.hasNext()) str.append("\r\n");
        }
        return str.toString();
    }


  //**************************************************************************
  //** getSuggestions
  //**************************************************************************
  /** Prints a list of keywords that we might consider adding to the list.
   */
    public HashMap<String, String> getSuggestions(){
        return suggestions;
    }


  //**************************************************************************
  //** extractCountries
  //**************************************************************************
  /** Used to parse a given block of text and extract countries.
   *  @param countryCodes A hashmap of country codes and counts for each.
   *  Entries are created or updated as countries are found.
   */
    public String extractCountries(String InputText, Map<String, Integer> countryCodes){

        if (countryCodes==null) countryCodes = new LinkedHashMap<>();

      //Remove extra whitespaces
        InputText = InputText.replaceAll("\\s+", " ").trim();
        InputText = " " + InputText + " ";



        Iterator<String> it = countryNames.keySet().iterator();
        while (it.hasNext()){
            String countryName = it.next();
            String countryCode = countryNames.get(countryName);



          //Set up the regex used to replace the country names
            String SearchPrefix, SearchSuffix;
            boolean CaseSensativeSearch;
            if (isCapitalized(countryName) && countryName.length()<=4){ //ex. US, USA, UN, UK, DPRK
                SearchPrefix = " ";
                SearchSuffix = " ";
                CaseSensativeSearch = true;
            }
            else{ //America, United States, England
                SearchPrefix = " ";
                SearchSuffix = ""; //no need to have a space at the end so we can catch varients - e.g. Pakistani
                CaseSensativeSearch = false;
            }

            countryName = countryName.replace("(", "\\(").replace(")", "\\)");
            String regex = "(" + SearchPrefix + countryName + SearchSuffix + ")";



/*
          //Find keyword
            String keyword = countryName;
            String searchText = SearchPrefix + countryName + SearchSuffix;
            int idx;
            while ((idx=InputText.indexOf(searchText))>-1){


                String a = InputText.substring(0, idx);
                String b = InputText.substring(idx+keyword.length()+1);


              //Find what we've matched to. For example, the search term
              //"Pakistan" might yield "Pakistani".
                String endsWith = "";
                String c = b.substring(0, 1);
                if (!c.equals(" ")){

                    int x = b.indexOf(" ");

                    if (x>-1){
                        endsWith = b.substring(0, x);
                        b = b.substring(x);
                    }
                }
                String match = keyword+endsWith;
                //System.out.println(keyword + " --> " + match);




              //Logic used to determine whether the match is valid.
                boolean foundMatch = true;
                if (endsWith.length()>0){


                }
                else{

                }
                if (foundMatch){
                    Integer prevOccurances = countryCodes.get(countryCode);
                    if (prevOccurances==null) prevOccurances=0;
                    countryCodes.put(countryCode, prevOccurances++);
                }



              //Update input text for the next pass
                StringBuffer str = new StringBuffer();
                str.append(a); str.append(b);
                InputText = str.toString();
            }
*/



          //Replace all instances of the country name in the text
            Pattern pattern = Pattern.compile(regex);
            if (CaseSensativeSearch==false) pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(InputText);

            int numMatches = 0;
            while (matcher.find()){
                int a = matcher.start();
                int b = matcher.end();
                //console.log(InputText.substring(a, b));

              //Update the end position by moving it to the next whitespace or period, whichever is first
                int x = InputText.substring(b).indexOf(" "); if (x<0) x=0;
                int y = InputText.substring(b).indexOf("."); if (y<0) y=0;
                if (y<x) x = y;
                if (SearchSuffix.equals("")) b+=x;

              //Remove the country name
                InputText = InputText.substring(0, a) + " " + InputText.substring(b);
                matcher = pattern.matcher(InputText);

                numMatches++;
            }


          //Update the list of countrycodes
            if (numMatches>0){
                Integer prevOccurances = countryCodes.get(countryCode);
                if (prevOccurances!=null) numMatches+=prevOccurances;
                countryCodes.put(countryCode, numMatches);
            }



            if (InputText.isBlank()) break;


        }//end loop


        return InputText;
    }

    private void parseNames(String[] rows){

        List<String> list = new ArrayList<>();
        StringLengthListSort ss = new StringLengthListSort();


        for (String row : rows){
            row = row.trim();
            if (row.isEmpty()) continue;


            String[] country = row.split(";");
            String countryName = country[0].trim();
            String countryCode = country[1].trim().toUpperCase();
            String[] arr;




          //Update country name
            if (countryName.startsWith("\"") && countryName.endsWith("\"")){
                countryName = countryName.substring(1, countryName.length()-1).trim();
                arr = countryName.split(" ");
            }
            else{

              //Update case. Some versions of the parser are case sensative so
              //we need to make sure the country names have the proper case.
                arr = countryName.split(" ");
                if (arr.length>1){
                    String a = countryName;
                    String b = "";

                    for (String str : arr){

                        String c = fixCase(str);
                        if (c.equals("Of")) c = "of";
                        if (c.equals("And")) c = "and";
                        if (c.equals("The")) c = "the";

                        if (c.startsWith("(")){
                            if (c.endsWith(")")){
                                c = "(" + fixCase(str.substring(1, str.length()-1)) + ")";
                            }
                            else{
                                c = "(" + fixCase(str.substring(1));
                            }
                        }
                        else if(c.endsWith(")")) {
                            c = fixCase(str.substring(0, str.length()-1)) + ")";
                        }

                        b+=c + " ";
                    }
                    b = b.trim();

                    if (b.startsWith("the ")) b = "T" + b.substring(1);

                    //System.out.println(countryName + (a.equals(b) ? "" : (" --> " + b)));

                    countryName = b;
                }
                else{

                    String a = countryName;
                    String b = fixCase(countryName);

                    //System.out.println(countryName + (a.equals(b) ? "" : (" --> " + b)));

                    countryName = b;
                }
            }

          //Add record
            addRecord(countryName + ";" + countryCode, list);
            //String a = country[0].trim();
            //System.out.println(a + (a.equals(countryName) ? "" : (" --> " + countryName)));


          //Add varients of names (e.g. "St." vs "Saint" and "and" vs "&")
            if (arr.length>1){
                if (countryName.startsWith("St. ")){
                    countryName = "Saint" + countryName.substring(3);
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.startsWith("Saint ")){
                    countryName = "St." + countryName.substring(5);
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.contains(" and ")){
                    countryName = countryName.replace(" and ", " & ");
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.contains(" & ")){
                    countryName = countryName.replace(" & ", " and ");
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.endsWith("I.")){
                    countryName = countryName.substring(0, countryName.length()-1) + "sland";
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.endsWith("Is.")){
                    countryName = countryName.substring(0, countryName.length()-1) + "lands";
                    addRecord(countryName + ";" + countryCode, list);
                }

                if (countryName.endsWith("Island") || countryName.endsWith("Islands")){
                    countryName = countryName.substring(0, countryName.length()-5) + ".";
                    addRecord(countryName + ";" + countryCode, list);
                }
            }

        }





      //Sort the list
        Collections.sort(list, ss);



      //Create sorted hashmap of countries and a list of suggestions
        countryNames = new LinkedHashMap<>();
        suggestions = new HashMap<>();
        Iterator<String> it = list.iterator();
        while (it.hasNext()){
            String record = it.next();
            String[] country = record.split(";");
            String countryName = country[0];
            String countryCode = country[1];
            countryNames.put(countryName, countryCode);


          //Populate suggestions
            String[] names = countryName.split(" & ");
            if (names.length==2) for (String name : names){
                if (!list.contains(name + ";" + countryCode)) suggestions.put(name, countryCode);
            }
            names = countryName.split(" and ");
            if (names.length==2) for (String name : names){
                if (!list.contains(name + ";" + countryCode)) suggestions.put(name, countryCode);
            }

            String altName = removeIsland(countryName);
            if (!altName.equals(countryName)){
                if (countryName.contains(" and ") || countryName.contains(" & ") ){
                    if (!list.contains(altName + ";" + countryCode))
                    suggestions.put(altName, countryCode);
                }
            }
        }


    }



  //**************************************************************************
  //** addRecord
  //**************************************************************************
    private void addRecord(String record, List<String> list){
        addRecord(record, list, false);
    }

    private void addRecord(String record, List<String> list, boolean print){
        if (!list.contains(record)){
            list.add(record);
            if (print) System.out.println(" ++ " + record);
        }
    }


  //**************************************************************************
  //** removeIsland
  //**************************************************************************
    private String removeIsland(String countryName){
        for (String str : new String[]{"Islands", "Island", "Is.", "I."}){
            if (countryName.endsWith(" " + str)){
                return countryName.substring(0, countryName.length()-(str.length()+1));
            }
        }
        return countryName;
    }


  //**************************************************************************
  //** isCapitalized
  //**************************************************************************
  /** Used to determine whether a word is capitalized. Returns true only if
   *  all the characters in the string are uppercase.
   */
    private static boolean isCapitalized(String text){
        for (int i=0; i<text.length(); i++){
            if (!Character.isUpperCase(text.charAt(i))){
                return false;
            }
        }
        return true;
    }


  //**************************************************************************
  //** fixCase
  //**************************************************************************
  /** Used to update characters in a string into the proper case. Upper case
   *  for the first character, lower case for the rest. Supports abbreviations
   *  and hyphenated words.
   *
   *  @param str A single word (string without whitespace)
   */
    private static String fixCase(String str){

      //Special case for abbreviations
        if (str.contains(".")){
            String[] arr = str.split("\\.");
            str = "";
            for (String s : arr){
                if (s.length()==1) str+=s+".";
                else{
                    String a = s.substring(0, 1).toUpperCase();
                    String b = s.substring(1).toLowerCase();
                    str+=a+b+".";
                }
            }
            return str;
        }


      //Special case for hyphens
        if (str.contains("-")){
            String[] arr = str.split("-");
            str = "";
            for (int i=0; i<arr.length; i++){
                String s = arr[i];
                str += fixCase(s);
                if (i<arr.length-1) str += "-";
            }
            return str;
        }


        String a = str.substring(0, 1).toUpperCase();
        String b = str.substring(1).toLowerCase();


      //Special Case for McDonald Island
        if (a.equals("M") && b.startsWith("c")){
            String c = b.substring(1,2).toUpperCase();
            b = "c" + c + b.substring(2);
            //System.out.println(a+b+"**********************************");
        }


        return a+b;
    }


  //**************************************************************************
  //** StringLengthListSort
  //**************************************************************************
  /** Simple class used to help sort strings by length.
   */
    private class StringLengthListSort implements Comparator<String>{
        @Override
        public int compare(String s1, String s2) {
            return s2.length() - s1.length();
        }
    }

}