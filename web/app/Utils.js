if(!com) var com={};
if(!com.kartographia) com.kartographia={};

com.kartographia.utils = {


  //**************************************************************************
  //** addOverflow
  //**************************************************************************
  /** Used to add an overflow div to another element. Automatically inserts
   *  iScroll if available. Returns an object with pointers to the outer and
   *  inner divs along with an update() method which should be called after
   *  adding or removing elements from the inner div.
   */
    addOverflow: function(parent, config){

        if (!config) config = {};
        var defaultConfig = {
            style: {}
        };
        javaxt.dhtml.utils.merge(config, defaultConfig);


        var div = document.createElement("div");
        div.style.position = "relative";
        div.style.width = "100%";
        div.style.height = "100%";
        parent.appendChild(div);

        var overflowDiv = document.createElement("div");
        overflowDiv.style.position = "absolute";
        overflowDiv.style.width = "100%";
        overflowDiv.style.height = "100%";
        overflowDiv.style.overflow = "hidden";
        div.appendChild(overflowDiv);


        var innerDiv = document.createElement("div");
        innerDiv.style.position = "relative";
        innerDiv.style.width = "100%";
        innerDiv.style.height = "100%";
        overflowDiv.appendChild(innerDiv);


        var ret = {
            outerDiv: div,
            innerDiv: innerDiv,
            update: function(){},
            scrollToElement: function(el){
                el.scrollIntoView(false);
            }
        };



        if (typeof IScroll !== 'undefined'){

            var onRender = function(){
                var iscroll = new IScroll(overflowDiv, {
                    scrollbars: config.style.iscroll ? "custom" : true,
                    mouseWheel: true,
                    fadeScrollbars: false,
                    hideScrollbars: false
                });
                if (config.style.iscroll) {
                    javaxt.dhtml.utils.setStyle(iscroll, config.style.iscroll);
                }


              //Create custom update function to return to the client so they
              //can update iscroll as needed (e.g. after adding/removing elements)
                ret.update = function(){
                    var h = 0;
                    for (var i=0; i<ret.innerDiv.childNodes.length; i++){
                        var el = ret.innerDiv.childNodes[i];
                        h = Math.max(javaxt.dhtml.utils.getRect(el).bottom, h);
                    }
                    h = h - (javaxt.dhtml.utils.getRect(ret.innerDiv).top);
                    ret.innerDiv.style.height = h + "px";
                    iscroll.refresh();
                };
                ret.update();


              //Create custom scrollToElement function
                ret.scrollToElement = function(el){
                    overflowDiv.scrollTop = 0;
                    iscroll.scrollToElement(el);
                };


                if (config.onRender) config.onRender.apply(this, [ret]);
            };


          //Check whether the table has been added to the DOM
            var w = overflowDiv.offsetWidth;
            if (w===0 || isNaN(w)){
                var timer;

                var checkWidth = function(){
                    var w = overflowDiv.offsetWidth;
                    if (w===0 || isNaN(w)){
                        timer = setTimeout(checkWidth, 100);
                    }
                    else{
                        clearTimeout(timer);
                        onRender();
                    }
                };

                timer = setTimeout(checkWidth, 100);
            }
            else{
                onRender();
            }
        }
        else{
            overflowDiv.style.overflowY = 'scroll';
        }


        return ret;
    },


  //**************************************************************************
  //** getMapLayer
  //**************************************************************************
  /** Used to iterate through a list of basemaps and return an ol.layer for
   *  a given layer group and layer name.
   *  @param name Name of a basemap
   *  @param layer Name of a layer found in a basemap
   *  @param basemaps JSON object with one or more basemaps
   */
    getMapLayer: function(name, layer, basemaps){
        var getLayer = com.kartographia.utils.getLayer;
        for (var i=0; i<basemaps.length; i++){
            var basemap = basemaps[i];
            if (basemap.name===name){
                var layers = basemap.layers;
                for (var j=0; j<layers.length; j++){
                    var layerName = layers[j];
                    if (layerName===layer){
                        return getLayer(basemap, layer);
                    }
                }
            }
        }
        return null;
    },

    getLayer: function(basemap, layer){
        var key = basemap.key;

        var url = basemap.url;
        if (url.indexOf("bing")>-1){
            return new ol.layer.Tile({
                source: new ol.source.BingMaps({
                    key: key,
                    imagerySet: layer
                })
            });
        }
        else if (url.indexOf("virtualearth.net")>-1){
            //http://ecn.{subdomain}.tiles.virtualearth.net/tiles/{layer}{quadkey}.jpeg?g=7150&mkt={culture}&shading=hill

            return new ol.layer.Tile({
                source: new ol.source.XYZ({
                    url: url,
                    tileUrlFunction: function(tileCoord, pixelRatio, projection){

                        var str = url.replace("{layer}", layer);
                        str = str.replace("{subdomain}", "t0");
                        str = str.replace("{culture}", "en-US");


                      //Replace tileCoords with a quadKey
                      //https://docs.microsoft.com/en-us/bingmaps/articles/bing-maps-tile-system
                        var quadKey = "";
                        var levelOfDetail = tileCoord[0];
                        var tileX = tileCoord[1];
                        var tileY = (-tileCoord[2])-1; //<-- This is a hack! OpenLayers is passing in tileCoords with a wierd y-offset (e.g. -4)
                        for (var i = levelOfDetail; i > 0; i--)
                        {
                            var digit = 48; // 48 is digit for 0
                            var mask = 1 << (i - 1);
                            if ((tileX & mask) != 0)
                            {
                                digit++;
                            }
                            if ((tileY & mask) != 0)
                            {
                                digit++;
                                digit++;
                            }
                            quadKey += String.fromCharCode(digit);
                        }
                        str = str.replace("{quadkey}", quadKey);



                        if (key){
                            if (str.indexOf("?")==-1) str += "?";
                            str+="&key="+key;
                        }

                        return str;
                    }
                }),
                visible: false
            });

        }
        else if (url.indexOf("google.com")>-1){
            //http://maps.google.com/maps/vt?pb=!1m5!1m4!1i{z}!2i{x}!3i{y}!4i256!2m3!1e0!2sm!3i375060738!3m9!2s{language}!3s{country}!5e18!12m1!1e47!12m3!1e37!2m1!1ssmartmaps!4e0

            var language = 'en';
            var country = 'US';
            url = url.replace("{language}", language);
            url = url.replace("{country}", country);

            return new ol.layer.Tile({
                source: new ol.source.XYZ({
                    url:url
                }),
                visible: true
            });
        }
        else{


            url = url.replace("{layer}", layer);
            if (key){
                if (url.indexOf("here.com")>-1){
                    if (url.indexOf("?")==-1) url += "?";
                    else url += "&";
                    url += key;
                }
            }

            return new ol.layer.Tile({
                source: new ol.source.XYZ({
                    url:url
                }),
                visible: true
            });
        }

    }

};