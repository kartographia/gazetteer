package com.kartographia.gazetter.utils;
import java.util.*;
import java.util.regex.Pattern;
import javaxt.sql.*;

public class Parser {

    private LinkedHashMap<String, Integer> countryCodes = new LinkedHashMap<>();
    private LinkedHashMap<String, String> countryNames;


  //**************************************************************************
  //** Parser
  //**************************************************************************
    public Parser(Database database){
        countryNames = null; //(LinkedHashMap<String, String>) dictionaries.get("CountryNames");
    }

    
    public LinkedHashMap<String, Integer> getCountryCodes(){
        return countryCodes;
    }


  //**************************************************************************
  //** extractCountries
  //**************************************************************************
  /** Used to extract country codes from a block of text. Returns the text
   *  without the country names.
   *  <pre>
   *  Example:
   *  "US President George Bush went with us to Pakistan to consult with Pakistani President Musharrif"
   *  "[US] President George Bush went with us to [PK] to consult with [PK]i President Musharrif"
   *
   *  Returns:
   *  "President George Bush went with us to to consult with President Musharrif"
   *  </pre>
   */
    public String extractCountries(String InputText){

        Iterator<String> it = countryNames.keySet().iterator();
        while (it.hasNext()){
            String countryName = it.next();
            String countryCode = countryNames.get(countryName);



          //Set up the regex used to replace the country names
            String SearchPrefix, SearchSuffix;
            boolean CaseSensativeSearch = false;
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
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                if (CaseSensativeSearch==false) pattern = java.util.regex.Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher matcher = pattern.matcher(InputText);

                int numMatches = 0;
                while (matcher.find()){
                    int a = matcher.start();
                    int b = matcher.end();
                    System.out.println(InputText.substring(a, b));

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



        }//end loop


      //Remove extrenous whitespaces
        while (InputText.contains("  ")){
            InputText = InputText.replace("  ", " ");
        }


      //Sort the list of countries based on number of occurances
        countryCodes = sortHashMap(countryCodes);




        return InputText;
    }


  //**************************************************************************
  //** isCapitalized
  //**************************************************************************
  /** Used to determine whether a word is capitalized. Returns true only if
   *  all the characters in the string are uppercase.
   */
    private boolean isCapitalized(String text){
        for (int i=0; i<text.length(); i++){
            if (!Character.isUpperCase(text.charAt(i))){
                return false;
            }
        }
        return true;
    }


  //**************************************************************************
  //** sortHashMap
  //**************************************************************************
  /** Used to sort a hashmap using it's values.
   */
    private LinkedHashMap sortHashMap(HashMap hashmap) {
        return sortHashMapByValues(hashmap, false);
    }


  //**************************************************************************
  //** sortHashMapByValues
  //**************************************************************************
  /** Used to sort a hashmap using it's values. Based on code by Tim Ringwood:
   *  http://www.theserverside.com/discussions/thread.tss?thread_id=29569
   */
    private LinkedHashMap sortHashMapByValues(HashMap passedMap, boolean ascending) {


      //Create a list of unique values
        HashSet uniqueValues = new HashSet();
        uniqueValues.addAll(passedMap.values());

      //Sort the list of unique values
        List mapValues = new ArrayList(uniqueValues);
        Collections.sort(mapValues);

      //Reverse the sort (as needed)
        if (!ascending){
            Collections.reverse(mapValues);
        }



      //Create a new hashmap
        LinkedHashMap newMap = new LinkedHashMap();
        Iterator valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Object val = valueIt.next();

            HashSet newEntries = new HashSet();

            Iterator it = passedMap.keySet().iterator();
            while (it.hasNext()) {
                Object key = it.next();
                if (passedMap.get(key).toString().equals(val.toString())){
                    newMap.put(key, val);
                    newEntries.add(key);
                    //System.out.println(val + " --> " + key);
                }
            }


          //Update the input map to minimize iterations on the next pass
            it = newEntries.iterator();
            while (it.hasNext()) {
                passedMap.remove(it.next());
            }

        }


      //Return the new hashmap
        return newMap;
    }


  //**************************************************************************
  //** prepText
  //**************************************************************************
  /** Used to prep a block of text for the parser. Replaces unwanted
   *  characters, punctuation, etc.
   */
    public String prepText(String InputText){

      //Set start time
        long startTime = new Date().getTime();


      //Replace some Unicode characters with ASCII approximations
        Iterator<Character> it = test.keySet().iterator();
        while (it.hasNext()){
            char c = it.next();
            char r = test.get(c);
            InputText = InputText.replace(c, r);
        }


      //Replace quotes
        InputText = InputText.replace("\"", " ");


      //Remove punctuations
        StringBuffer str = new StringBuffer();
        String[] words = InputText.split(" ");
        for (int i=0; i<words.length; i++){
            String word = words[i].trim();
            if (word.length()>0){

                if (word.endsWith("...")){
                    word = word.substring(0, word.length()-3);
                }
                else if (word.endsWith("?") || word.endsWith("!")){
                    word = word.substring(0, word.length()-1);
                }
//                else if (word.endsWith(",")){
//                    word = word.substring(0, word.length()-1);
//                    if (!word.endsWith(".")) word+="."; //His father, George Bush Sr., went to the store
//                }
                str.append(word + " ");
            }
        }
        InputText = str.toString();



      //Replace unwanted characters
        String inValidChars = "\r\n\tï¿½~`@#$%&*_=+()[]{}|\\/;:<>^";
        char replacement = (" ").charAt(0);
        for (int i=0; i < inValidChars.length(); i++){
             InputText = InputText.replace(inValidChars.charAt(i), replacement);
        }


      //Remove extra whitespaces
        InputText = InputText.replaceAll("\\s+", " ").trim();

      //Update stats
        //stats.put("prepText", new Date().getTime()-startTime);

        return " " + InputText + " ";
    }

  //**************************************************************************
  //** Unicode punctiation map
  //**************************************************************************
  /**
   *  http://stackoverflow.com/questions/4808967/replacing-unicode-punctuation-with-ascii-approximations
   */
    private Map<Character, Character> test = new HashMap<Character, Character>(){{

     put((char) 0x00AB, '"');
     put((char) 0x00AD, '-');
     put((char) 0x00B4, '\'');
     put((char) 0x00BB, '"');
     put((char) 0x00F7, '/');
     put((char) 0x01C0, '|');
     put((char) 0x01C3, '!');
     put((char) 0x02B9, '\'');
     put((char) 0x02BA, '"');
     put((char) 0x02BC, '\'');
     put((char) 0x02C4, '^');
     put((char) 0x02C6, '^');
     put((char) 0x02C8, '\'');
     put((char) 0x02CB, '`');
     put((char) 0x02CD, '_');
     put((char) 0x02DC, '~');
     put((char) 0x0300, '`');
     put((char) 0x0301, '\'');
     put((char) 0x0302, '^');
     put((char) 0x0303, '~');
     put((char) 0x030B, '"');
     put((char) 0x030E, '"');
     put((char) 0x0331, '_');
     put((char) 0x0332, '_');
     put((char) 0x0338, '/');
     put((char) 0x0589, ':');
     put((char) 0x05C0, '|');
     put((char) 0x05C3, ':');
     put((char) 0x066A, '%');
     put((char) 0x066D, '*');
     put((char) 0x200B, ' ');
     put((char) 0x2010, '-');
     put((char) 0x2011, '-');
     put((char) 0x2012, '-');
     put((char) 0x2013, '-');
     put((char) 0x2014, '-');
     put((char) 0x2015, '-');
     put((char) 0x2016, '|');
     put((char) 0x2017, '_');
     put((char) 0x2018, '\'');
     put((char) 0x2019, '\'');
     put((char) 0x201A, ',');
     put((char) 0x201B, '\'');
     put((char) 0x201C, '"');
     put((char) 0x201D, '"');
     put((char) 0x201E, '"');
     put((char) 0x201F, '"');
     put((char) 0x2032, '\'');
     put((char) 0x2033, '"');
     put((char) 0x2034, '\'');
     put((char) 0x2035, '`');
     put((char) 0x2036, '"');
     put((char) 0x2037, '\'');
     put((char) 0x2038, '^');
     put((char) 0x2039, '<');
     put((char) 0x203A, '>');
     put((char) 0x203D, '?');
     put((char) 0x2044, '/');
     put((char) 0x204E, '*');
     put((char) 0x2052, '%');
     put((char) 0x2053, '~');
     put((char) 0x2060, ' ');
     put((char) 0x20E5, '\\');
     put((char) 0x2212, '-');
     put((char) 0x2215, '/');
     put((char) 0x2216, '\\');
     put((char) 0x2217, '*');
     put((char) 0x2223, '|');
     put((char) 0x2236, ':');
     put((char) 0x223C, '~');
     put((char) 0x2264, '<');
     put((char) 0x2265, '>');
     put((char) 0x2266, '<');
     put((char) 0x2267, '>');
     put((char) 0x2303, '^');
     put((char) 0x2329, '<');
     put((char) 0x232A, '>');
     put((char) 0x266F, '#');
     put((char) 0x2731, '*');
     put((char) 0x2758, '|');
     put((char) 0x2762, '!');
     put((char) 0x27E6, '[');
     put((char) 0x27E8, '<');
     put((char) 0x27E9, '>');
     put((char) 0x2983, '{');
     put((char) 0x2984, '}');
     put((char) 0x3003, '"');
     put((char) 0x3008, '<');
     put((char) 0x3009, '>');
     put((char) 0x301B, ']');
     put((char) 0x301C, '~');
     put((char) 0x301D, '"');
     put((char) 0x301E, '"');
     put((char) 0xFEFF, ' ');

    }};

}