package com.kartographia.map;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.WKTReader;


import javaxt.utils.Console;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.image.Kernel;

import java.text.DecimalFormat;
import java.util.*;


//******************************************************************************
//**  MapTile
//******************************************************************************
/**
 *   Used to generate images that are rendered on a map. Can be used render
 *   points, lines, polygons, etc.
 *
 ******************************************************************************/

public class MapTile {

    private double ULx = 0;
    private double ULy = 0;
    private double resX = 1;
    private double resY = 1;

    private javaxt.io.Image img;
    private Graphics2D g2d;

    private String wkt;
    private double north;
    private double south;
    private double east;
    private double west;
    private Geometry geom;
    private int srid;

    private static Console console = new Console();
    private static DecimalFormat df = new DecimalFormat("#.##");

    private static int CLAMP_EDGES = 1;
    private static int WRAP_EDGES = 2;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MapTile(double minX, double minY, double maxX, double maxY,
                     int width, int height){
        this(minX, minY, maxX, maxY, width, height, 4326);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public MapTile(double minX, double minY, double maxX, double maxY,
                     int width, int height, int srid){


        df.setMaximumFractionDigits(8);

        this.srid = srid;
        if (srid==3857){



          //Get wkt
            north = getLat(maxY);
            south = getLat(minY);
            east = getLon(maxX);
            west = getLon(minX);
            String NE = df.format(east) + " " + df.format(north);
            String SE = df.format(east) + " " + df.format(south);
            String SW = df.format(west) + " " + df.format(south);
            String NW = df.format(west) + " " + df.format(north);
            wkt = "POLYGON((" + NE + "," +  NW + "," + SW + "," + SE + "," + NE + "))";



            ULx = minX;
            ULy = maxY;


          //Compute pixelsPerMeter
            resX = width  / diff(minX,maxX);
            resY = height / diff(minY,maxY);

        }
        else if (srid==4326){


          //Validate Coordinates
            if (validate(minX, minY, maxX, maxY)==false) throw new IllegalArgumentException();



          //Get wkt
            String NE = df.format(maxX) + " " + df.format(maxY);
            String SE = df.format(maxX) + " " + df.format(minY);
            String SW = df.format(minX) + " " + df.format(minY);
            String NW = df.format(minX) + " " + df.format(maxY);
            wkt = "POLYGON((" + NE + "," +  NW + "," + SW + "," + SE + "," + NE + "))";
            north = maxY;
            south = minY;
            east = maxX;
            west = minX;





          //Update min/max coordinates
            minX = x(minX);
            minY = y(minY);
            maxX = x(maxX);
            maxY = y(maxY);


          //Update Local Variables using updated values
            ULx = minX;
            ULy = maxY;


          //Compute pixelsPerDeg
            resX = ((double) width)  / (maxX-minX);
            resY = ((double) height) / (minY-maxY);//(maxY-minY);
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }


      //Create image
        img = new javaxt.io.Image(width, height);
        g2d = img.getBufferedImage().createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //applyQualityRenderingHints(g2d);
        g2d.setColor(Color.BLACK);
    }


  //**************************************************************************
  //** getSRID
  //**************************************************************************
    public int getSRID(){
        return srid;
    }


  //**************************************************************************
  //** getBounds
  //**************************************************************************
  /** Returns the tile boundary as a Well-Known Text (WKT) in lat/lon
   *  coordinates (EPSG:4326)
   */
    public String getBounds(){
        return wkt;
    }


  //**************************************************************************
  //** getGeometry
  //**************************************************************************
  /** Returns the tile boundary as a lat/lon geometry (EPSG:4326)
   */
    public Geometry getGeometry(){
        if (geom==null){
            try{
                geom = new WKTReader().read(wkt);
            }
            catch(Exception e){
                //should never happen
            }
        }
        return geom;
    }


  //**************************************************************************
  //** getWidth
  //**************************************************************************
    public int getWidth(){
        return img.getWidth();
    }


  //**************************************************************************
  //** getHeight
  //**************************************************************************
    public int getHeight(){
        return img.getHeight();
    }


    public double getNorth(){
        return north;
    }

    public double getSouth(){
        return south;
    }

    public double getEast(){
        return east;
    }

    public double getWest(){
        return west;
    }


  //**************************************************************************
  //** getImage
  //**************************************************************************
    public javaxt.io.Image getImage(){
        return img;
    }


  //**************************************************************************
  //** setBackgroundColor
  //**************************************************************************
    public void setBackgroundColor(int r, int g, int b){
        Color org = g2d.getColor();
        g2d.setColor(new Color(r,g,b));
        g2d.fillRect(0,0,img.getWidth(),img.getHeight());
        g2d.setColor(org);
    }


  //**************************************************************************
  //** addPoint
  //**************************************************************************
  /** Used to add a point to the image
   */
    public void addPoint(double lat, double lon, Color color, int size){

      //Get center point
        double x = x(lon);
        double y = y(lat);


      //Get upper left coordinate
        double r = (double)size/2d;
        x = x-r;
        y = y-r;


      //Render circle
        g2d.setColor(color);
        g2d.fillOval(cint(x), cint(y), size, size);
        g2d.setColor(Color.BLACK);
    }


  //**************************************************************************
  //** addPolygon
  //**************************************************************************
  /** Used to add a polygon to the image
   */
    public void addPolygon(Polygon polygon, Color lineColor, Color fillColor){

        Coordinate[] coordinates = polygon.getCoordinates();
        int[] xPoints = new int[coordinates.length];
        int[] yPoints = new int[coordinates.length];

        for (int i=0; i<coordinates.length; i++){
            Coordinate coordinate = coordinates[i];
            xPoints[i] = cint(x(coordinate.x));
            yPoints[i] = cint(y(coordinate.y));
        }


        if (fillColor!=null){
            g2d.setColor(fillColor);
            g2d.fillPolygon(xPoints, yPoints, coordinates.length);
        }
        g2d.setColor(lineColor);
        g2d.drawPolyline(xPoints, yPoints, coordinates.length);

    }


  //**************************************************************************
  //** addCountries
  //**************************************************************************
  /** Used to add country polygons to the image
   */
    public void addCountries(ArrayList<Polygon> countries, Color lineColor, Color fillColor){


      //Generate a list of AWT Polygons and merge them into a single AWT Area
        Area area = null;
        ArrayList<java.awt.Polygon> polygons = new ArrayList<>();
        for (Polygon country : countries){
            Coordinate[] coordinates = country.getCoordinates();
            int[] xPoints = new int[coordinates.length];
            int[] yPoints = new int[coordinates.length];

            for (int i=0; i<coordinates.length; i++){
                Coordinate coordinate = coordinates[i];
                xPoints[i] = cint(x(coordinate.x));
                yPoints[i] = cint(y(coordinate.y));
            }

            java.awt.Polygon polygon = new java.awt.Polygon(xPoints, yPoints, coordinates.length);
            polygons.add(polygon);

            if (area==null){
                area = new Area(polygon);
            }
            else{
                area.add(new Area(polygon));
            }
        }

        if (area==null) return;




      //Render shadows
        createShadow(area, 2, 0, 0, Color.BLACK, 0.4f);
        createShadow(area, 5, 5, 5, Color.BLACK, 0.4f);


      //Draw land
        g2d.setColor(fillColor);
        g2d.fill(area);




      //Draw country boundaries
        g2d.setStroke(getStroke(0.0f, null));
        g2d.setColor(lineColor);
        for (java.awt.Polygon polygon : polygons){
            g2d.draw(polygon);
        }

    }


  //**************************************************************************
  //** createShadow
  //**************************************************************************
  /** Used to add a shadow or glow for a given object
   *  @param shape The reference object
   *  @param blur Size of the shadow relative to the original shape. For
   *  example, if a given shape is box with 20 pixels on each side, a value of
   *  5 indicates that the shadow will be generated by expanding the box by 5
   *  pixels on each side resulting in a 30x30 pixel shadow. In CSS this is
   *  equivalent to the "blur" property for box-shadows.
   */
    private void createShadow(java.awt.Shape shape, int blur,
        int xOffset, int yOffset, Color color, float opacity){


      //Create color with opacity
        float[] colors = color.getRGBComponents(null);
        Color c = new Color(colors[0], colors[1], colors[2], opacity);


      //Generate shadow
        if (blur>0){


            int width = getWidth()*3;
            int height = getHeight()*3;

            javaxt.io.Image img = new javaxt.io.Image(width, height);
            Graphics2D g2d = img.getBufferedImage().createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


            g2d.translate(width/3, height/3);
            g2d.setColor(c);
            g2d.fill(shape);


            int[] inPixels = new int[width*height];
            int[] outPixels = new int[width*height];
            img.getBufferedImage().getRGB( 0, 0, width, height, inPixels, 0, width );


            Kernel kernel = getGaussianKernel((float) blur);
            convolveAndTranspose(kernel, inPixels, outPixels, width, height, true, CLAMP_EDGES);
            convolveAndTranspose(kernel, outPixels, inPixels, height, width, true, CLAMP_EDGES);


            img = new javaxt.io.Image(width, height);
            img.getBufferedImage().setRGB( 0, 0, width, height, inPixels, 0, width );

            this.img.addImage(img, -(width/3)+xOffset, -(height/3)+yOffset, false);
        }
        else{


          //Create new image
            javaxt.io.Image img = new javaxt.io.Image(getWidth(), getHeight());
            Graphics2D g2d = img.getBufferedImage().createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


          //Create shadow
            g2d.setColor(c);
            g2d.fill(shape);


          //Add shadow to the original image using x and y offsets
            this.img.addImage(img, xOffset, yOffset, false);

        }
    }


  //**************************************************************************
  //** convolveAndTranspose
  //**************************************************************************
  /** Applies 1D Gaussian kernel used to blur an image. This filter should be
   *  applied twice, once horizontally and once vertically.
   *  @author Jerry Huxtable
   */
    private void convolveAndTranspose(Kernel kernel, int[] inPixels, int[] outPixels,
        int width, int height, boolean alpha, int edgeAction) {


        float[] matrix = kernel.getKernelData( null );
        int cols = kernel.getWidth();
        int cols2 = cols/2;

        for (int y = 0; y < height; y++) {
            int index = y;
            int ioffset = y*width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                int moffset = cols2;
                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset+col];

                    if (f != 0) {
                        int ix = x+col;
                        if ( ix < 0 ) {
                            if ( edgeAction == CLAMP_EDGES )
                                    ix = 0;
                            else if ( edgeAction == WRAP_EDGES )
                                    ix = (x+width) % width;
                        } else if ( ix >= width) {
                            if ( edgeAction == CLAMP_EDGES )
                                    ix = width-1;
                            else if ( edgeAction == WRAP_EDGES )
                                    ix = (x+width) % width;
                        }
                        int rgb = inPixels[ioffset+ix];
                        a += f * ((rgb >> 24) & 0xff);
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }
                int ia = alpha ? clamp((int)(a+0.5)) : 0xff;
                int ir = clamp((int)(r+0.5));
                int ig = clamp((int)(g+0.5));
                int ib = clamp((int)(b+0.5));
                outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                index += height;
            }
        }
    }


  //**************************************************************************
  //** getGaussianKernel
  //**************************************************************************
  /** Returns a Gaussian blur kernel
   *  @author Jerry Huxtable
   */
    public static Kernel getGaussianKernel(float radius) {
        int r = (int)Math.ceil(radius);
        int rows = r*2+1;
        float[] matrix = new float[rows];
        float sigma = radius/3;
        float sigma22 = 2*sigma*sigma;
        float sigmaPi2 = (float)(2*Math.PI*sigma);
        float sqrtSigmaPi2 = (float)Math.sqrt(sigmaPi2);
        float radius2 = radius*radius;
        float total = 0;
        int index = 0;
        for (int row = -r; row <= r; row++) {
            float distance = row*row;
            if (distance > radius2)
                matrix[index] = 0;
            else
                matrix[index] = (float)Math.exp(-(distance)/sigma22) / sqrtSigmaPi2;
            total += matrix[index];
            index++;
        }
        for (int i = 0; i < rows; i++)
            matrix[i] /= total;

        return new Kernel(rows, 1, matrix);
    }


  //**************************************************************************
  //** clamp
  //**************************************************************************
  /** Clamp a value to the range 0..255
   *  @author Jerry Huxtable
   */
    public static int clamp(int c) {
        if (c < 0) return 0;
        if (c > 255) return 255;
        return c;
    }


  //**************************************************************************
  //** getStroke
  //**************************************************************************
  /** Returns a BasicStroke pattern used to draw lines
   *  @param lineWidth Accepts positive values starting with 0.0f (smallest possible)
   *  @param dash Accepts array and null. Example: float dash[] = {10.0f};
   */
    private BasicStroke getStroke(float lineWidth, float[] dash){
        return new BasicStroke(
            lineWidth,
            BasicStroke.JOIN_ROUND, //corners
            BasicStroke.JOIN_MITER, //line (and dash) start/end
            10.0f, //miterlimit?
            dash, 0.0f //dash size and offset
        );
    };


  //**************************************************************************
  //** expand
  //**************************************************************************
    private void expand(java.awt.Shape shape, int blur){


      //Expanding the shape using Java's affine transformation appears to use
      //ratios. For example, scaling by 1.1 will expand the shape by approx 10%
      //The following code attempts to convert size into a percentage
        float size = (float) blur;
        float gx = 1f+ ((size/2f)/(float) getWidth())*10f;




      //Expand shape around the center
        double cx = shape.getBounds2D().getCenterX();
        double cy = shape.getBounds2D().getCenterY();
        AffineTransform old = g2d.getTransform();


        float scaleX = gx;
        //console.log(scaleX);
        double scaleY = scaleX;
        scaleY = scaleY*1.33; //Added this hack after a little trial and error...


        AffineTransform tr2 = AffineTransform.getTranslateInstance(-cx, -cy);
        AffineTransform tr = AffineTransform.getScaleInstance(scaleX,scaleY);
        tr.concatenate(tr2);
        tr2 = tr;

        tr = AffineTransform.getTranslateInstance(cx, cy);
        tr.concatenate(tr2);
        tr2 = tr;

        tr = new AffineTransform(old);
        tr.concatenate(tr2);
        tr2 = tr;

        g2d.setTransform(tr2);
        g2d.fill(shape);
        g2d.setTransform(old);
    }


    private void applyQualityRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }


  //**************************************************************************
  //** intersects
  //**************************************************************************
  /** Returns true if the tile intersects the given geometry.
   */
    public boolean intersects(String wkt) throws Exception {
        return new WKTReader().read(wkt).intersects(getGeometry());
    }


  //**************************************************************************
  //** validate
  //**************************************************************************
  /** Used to validate coordinates used to invoke this class
   */
    private boolean validate(double minX, double minY, double maxX, double maxY){
        if (minX > maxX || minY > maxY) return false;
        if (minX < -180 || maxX < -180 || maxX > 180 || minX > 180) return false;
        if (minY < -90 || maxY < -90 || maxY > 90 || minY > 90) return false;
        return true;
    }


  //**************************************************************************
  //** x
  //**************************************************************************
  /** Used to convert a geographic coordinate to a pixel coordinate
   *  @param pt Longitude value if the tile is in EPSG:4326. Otherwise,
   *  assumes value is in meters.
   */
    private double x(double pt){
        if (srid == 3857){
            double x = pt;
            double d = diff(ULx,x);
            if (x<ULx) d = -d;
            return d * resX;
        }
        else if (srid == 4326){
            pt += 180;
            double x = (pt - ULx) * resX;
            //System.out.println("X = " + x);
            return x;
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }
    }


  //**************************************************************************
  //** y
  //**************************************************************************
  /** Used to convert a geographic coordinate to a pixel coordinate
   *  @param pt Latitude value if the tile is in EPSG:4326. Otherwise,
   *  assumes value is in meters.
   */
    private double y(double pt){

        if (srid == 3857){
            double y = pt;
            double d = diff(ULy,y);
            if (y>ULy) d = -d;
            return  d * resY;
        }
        else if (srid == 4326){

            pt = -pt;
            if (pt<=0) pt = 90 + -pt;
            else pt = 90 - pt;

            pt = 180-pt;



            double y = (pt - ULy) * resY;

            if (cint(y)==0 || cint(y)==-0) y = 0;
            //else y = -y;


            return y;
        }
        else{
            throw new IllegalArgumentException("Unsupported projection");
        }
    }


    private static final double originShift = 2.0 * Math.PI * 6378137.0 / 2.0; //20037508.34

  //**************************************************************************
  //** getLat
  //**************************************************************************
  /** Converts y coordinate in EPSG:3857 to a latitude in EPSG:4326
   */
    public static double getLat(double y){
        //double lat = Math.log(Math.tan((90 + y) * Math.PI / 360.0)) / (Math.PI / 180.0);
        //return lat * originShift / 180.0;
        double lat = (y/originShift)*180.0;
        return 180.0 / Math.PI * (2 * Math.atan( Math.exp( lat * Math.PI / 180.0)) - Math.PI / 2.0);
    }



  //**************************************************************************
  //** getLon
  //**************************************************************************
  /** Converts x coordinate in EPSG:3857 to a longitude in EPSG:4326
   */
    public static double getLon(double x){
        //return x * originShift / 180.0;
        return (x/originShift)*180.0;
    }


  //**************************************************************************
  //** getX
  //**************************************************************************
  /** Converts longitude in EPSG:4326 to a x coordinate in EPSG:3857
   */
    public static double getX(double lon){
        //return (lon*180.0)/originShift;
        return lon * originShift / 180.0;
    }


  //**************************************************************************
  //** getY
  //**************************************************************************
  /** Converts latitude in EPSG:4326 to a y coordinate in EPSG:3857
   */
    public static double getY(double lat){
        //return Math.atan(Math.exp(lat * Math.PI / 20037508.34)) * 360 / Math.PI - 90;
        double y = Math.log( Math.tan((90 + lat) * Math.PI / 360.0 )) / (Math.PI / 180.0);
        return y * originShift / 180.0;
    }


  //**************************************************************************
  //** cint
  //**************************************************************************
    private int cint(Double d){
        return (int)Math.round(d);
    }


  //**************************************************************************
  //** diff
  //**************************************************************************
    public static double diff(double a, double b){
        double x = a-b;
        if (x<0) x = -x;
        return x;
    }
}