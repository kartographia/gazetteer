package com.kartographia.gazetter.web;
import java.io.IOException;
import javaxt.express.ws.*;
import javaxt.http.servlet.*;
import javaxt.io.Jar;
import javaxt.sql.*;

public class WebServices extends WebService {

    private Database database;

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public WebServices(Database database) throws Exception {
        this.database = database;

      //Register classes that this service will support
        Jar jar = new Jar(this);
        Jar.Entry[] jarEntries = jar.getEntries();
        for (Jar.Entry entry : jar.getEntries()){
            String name = entry.getName();
            if (name.endsWith(".class")){
                name = name.substring(0, name.length()-6).replace("/", ".");
                Class c = Class.forName(name);
                if (javaxt.sql.Model.class.isAssignableFrom(c)){
                    addClass(c);
                }
            }
        }

    }


  //**************************************************************************
  //** processRequest
  //**************************************************************************
  /** Used to process an HTTP request and generate an HTTP response.
   */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {


        if (request.isWebSocket()){
            //processWebSocketRequest(service, request, response);
        }
        else{

          //Process request
            ServiceResponse rsp = getServiceResponse(new ServiceRequest(request));
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
    private ServiceResponse getServiceResponse(ServiceRequest request)
        throws ServletException {


//      //Authenticate request
//        try{
//            request.authenticate();
//        }
//        catch(Exception e){
//            return new ServiceResponse(403, "Not Authorized");
//        }


        String service = request.getPath(0).toString();
        if (service.equals("upload")){
            return new ServiceResponse(501, "Not Implemented");
        }
        else if (service.equals("reports")){
            return new ServiceResponse(501, "Not Implemented");
        }
        else{
            return getServiceResponse(request, database);
        }
    }

}