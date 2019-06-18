package com.kartographia.gazetter.web;
import com.kartographia.gazetter.*;
import java.io.IOException;
import javaxt.express.ws.*;
import javaxt.http.servlet.*;
import javaxt.io.Jar;
import javaxt.sql.*;
import javaxt.json.*;

public class WebServices extends WebService {

    private Database database;
    private MapService mapService;
    private CountryService countryService;

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public WebServices(Database database) throws Exception {
        this.database = database;

      //Register classes that this service will support
        Jar jar = new Jar(com.kartographia.gazetter.Source.class);
        for (Class c : jar.getClasses()){
            if (javaxt.sql.Model.class.isAssignableFrom(c)){
                addClass(c);
            }
        }

        mapService = new MapService();
        countryService = new CountryService();
    }


  //**************************************************************************
  //** processRequest
  //**************************************************************************
  /** Used to process an HTTP request and generate an HTTP response.
   *  @param service The first "directory" found in the path, after the
   *  servlet context.
   */
    protected void processRequest(String service, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {


        if (request.isWebSocket()){
            //processWebSocketRequest(service, request, response);
        }
        else{

          //Process request
            ServiceResponse rsp = getServiceResponse(service, request);
            int status = rsp.getStatus();



          //Set general response headers
            response.setContentType(rsp.getContentType());
            response.setStatus(status == 501 ? 404 : status);



          //Set authentication header as needed
            String authMessage = rsp.getAuthMessage();
            String authType = request.getAuthType();
            if (authMessage!=null && authType!=null){
                //"WWW-Authenticate", "Basic realm=\"Access Denied\""
                if (authType.equalsIgnoreCase("BASIC")){
                    response.setHeader("WWW-Authenticate", "Basic realm=\"" + authMessage + "\"");
                }
            }


          //Send body
            Object obj = rsp.getResponse();
            if (obj instanceof java.io.InputStream){
                Long contentLength = rsp.getContentLength();
                if (contentLength!=null){
                    response.setHeader("Content-Length", contentLength+"");
                }

                java.io.InputStream inputStream = (java.io.InputStream) obj;
                response.write(inputStream, true);
                inputStream.close();
            }
            else{
                response.write((byte[]) obj, true);
            }


        }
    }


  //**************************************************************************
  //** getServiceResponse
  //**************************************************************************
  /** Maps a ServiceRequest to a WebService. Returns a ServiceResponse object
   *  to send back to the client.
   */
    private ServiceResponse getServiceResponse(String service, HttpServletRequest request)
        throws ServletException {


        if (service.equals("map")){
            return mapService.getServiceResponse(new ServiceRequest(service, request), database);
        }
        else if (service.equals("country") || service.equals("countries")){
            return countryService.getServiceResponse(service, request, database);
        }
        else{
            return getServiceResponse(new ServiceRequest(request), database);
        }
    }


  //**************************************************************************
  //** saveName
  //**************************************************************************
  /** Override the saveName method in the WebService base class. Place names
   *  have a one-to-many relationship with places. There is a diamond table
   *  used to associate names with places. However, the diamond table will
   *  not update when adding new place names. As a workaround, new place names
   *  are added to a place via the Place class.
   */
    public ServiceResponse saveName(ServiceRequest request, Database database)
        throws ServletException {

        console.log("saveName!");

      //Parse and update the payload as needed
        JSONObject json = request.getJson();
        json.remove("uname");
        json.remove("lastModified");
        if (!json.has("languageCode")) json.set("languageCode", "eng");

        
      //Create or update the place name
        try{
            Long nameID = request.getID();
            if (nameID==null){
                //add new name to place

                Long placeID = request.getParameter("placeID").toLong();
                Place place = new Place(placeID);


                Name placeName = new Name(json);


                boolean addName = true;
                for (Name name : place.getNames()){
                    if (name.getName().equalsIgnoreCase(placeName.getName())){
                        name.update(request.getJson());
                        name.save();
                        placeName = name;
                        addName = false;
                        break;
                    }
                }

                if (addName){
                    place.addName(placeName);
                    place.save();
                }

            }
            else{

              //update name
                Name placeName = new Name(nameID);
                placeName.update(json);
                placeName.save();
            }

            return new ServiceResponse(200);

        }
        catch(Exception e){
            return new ServiceResponse(e);
        }
    }
}