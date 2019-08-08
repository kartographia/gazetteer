if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  Gazetter Application
//******************************************************************************
/**
 *   Primary user interface to view and edit entries in the gazetter.
 *
 ******************************************************************************/

com.kartographia.gazetter.Application = function(parent, config) {

    var me = this;


    var map, mapDiv;
    var draw;

    var basemap = {};
    var layer = {};

    var mapMenu;



    var wktFormatter = new ol.format.WKT();
    var body = document.getElementsByTagName("body")[0];
    var fx = new javaxt.dhtml.Effects();


    var style = javaxt.dhtml.style.default;



  //The following variables are used to select features
    var cntrlIsPressed = false;
    var shiftIsPressed = false;
    var altIsPressed = false;



  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){


      //Create main table
        var table = createTable();
        var tbody = table.firstChild;
        var tr, td;


      //Create header
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.className = "header";
        createHeader(td);
        tr.appendChild(td);


      //Create body
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        tr.appendChild(td);
        parent.appendChild(table);


      //Populate body
        table = createTable();
        td.appendChild(table);
        tbody = table.firstChild;
        tr = document.createElement("tr");
        tbody.appendChild(tr);

        td = document.createElement("td");
        td.style.height = "100%";
        createMapToolbar(td);
        tr.appendChild(td);


        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        createMainPanel(td);
        tr.appendChild(td);


        td = document.createElement("td");
        td.style.height = "100%";
        createSidePanel(td);
        tr.appendChild(td);




      //Check whether the table has been added to the DOM
        var w = table.offsetWidth;
        if (w===0 || isNaN(w)){
            var timer;

            var checkWidth = function(){
                var w = table.offsetWidth;
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
    };

  //**************************************************************************
  //** onRender
  //**************************************************************************
    var onRender = function(){
        createMap(mapDiv);
    };


  //**************************************************************************
  //** createHeader
  //**************************************************************************
    var createHeader = function(parent){

        var searchBar = new com.kartographia.gazetter.SearchBar(parent);
        searchBar.onSelect = function(){

        };
    };


  //**************************************************************************
  //** createMainPanel
  //**************************************************************************
    var createMainPanel = function(parent){

        var table = createTable();
        var tbody = table.firstChild;
        var tr, td;


        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        createMapPanel(td);
        tr.appendChild(td);

        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.height = "200px";
        td.style.width = "100%";
        createGridPanel(td);
        tr.appendChild(td);

        parent.appendChild(table);
    };


  //**************************************************************************
  //** createMapPanel
  //**************************************************************************
    var createMapPanel = function(parent){

        var outerDiv = document.createElement('div');
        outerDiv.style.position = "relative";
        outerDiv.style.width = "100%";
        outerDiv.style.height = "100%";
        parent.appendChild(outerDiv);

        var innerDiv = document.createElement('div');
        innerDiv.style.position = "absolute";
        innerDiv.style.width = "100%";
        innerDiv.style.height = "100%";
        innerDiv.style.overflow = 'auto';
        innerDiv.style.overflowX = 'hidden';
        outerDiv.appendChild(innerDiv);

        mapDiv = document.createElement('div');
        mapDiv.style.width = "100%";
        mapDiv.style.height = "100%";
        innerDiv.appendChild(mapDiv);
    };


  //**************************************************************************
  //** createMapToolbar
  //**************************************************************************
    var createMapToolbar = function(parent){

        var div = document.createElement("div");
        div.className = "map-toolbar";
        div.style.position = "relative";
        parent.appendChild(div);


        var toolbar = document.createElement("div");
        toolbar.className = "map-toolbar";
        toolbar.style.position = "absolute";
        toolbar.style.zIndex = 2;
        div.appendChild(toolbar);


        var menuPanel = new com.kartographia.gazetter.MenuPanel(div, {
            fx: fx
        });

        var btn = document.createElement("div");
        btn.style.width = "40px";
        btn.style.height = "40px";
        btn.onclick = function(){

            if (!mapMenu){
                var div = document.createElement("div");
                mapMenu = new com.kartographia.gazetter.MapMenu(div, {
                    basemap: basemap
                });
            }
            else{
                mapMenu.update();
            }


            menuPanel.show(mapMenu, "Select Basemap");
        };
        toolbar.appendChild(btn);
    };


  //**************************************************************************
  //** createMapLayers
  //**************************************************************************
    var createMapLayers = function(){

        var idx = 0;
        var proj = ol.proj.get('EPSG:3857');

        var addBaseMap = function(key, name, layer, visible){
            if (!layer) return;

          //Add basemap to map
            layer.setVisible(visible);
            map.addLayer(layer, idx);
            idx++;


          //Generate preview url
            var tileUrlFunction = layer.getSource().getTileUrlFunction();
            var tileCoord = [3,2,3]; //Southeast coast of US, Carribean, and part of South America
            var preview = tileUrlFunction(tileCoord, 1, proj);
            if (preview){
              //The tileUrlFunction doesn't work correctly for some reason. For
              //most XYZ tile sources, the y value is off. For OSM, Google, and
              //our local VMAP tile, the "3" y-value is replaced with a "-4".
              //To compensate, we'll replace the "-4" with a "3"
                if (preview.indexOf("-4")>-1){
                    preview = preview.replace("-4", "3"); //add hack to replace wierd -4 y coordinate
                }
                else{ //bing?
                    tileCoord = [3,2,4];
                    preview = tileUrlFunction(tileCoord, 1, proj);
                }
            }


          //Update list of basemaps
            basemap[key] = {
                layer: layer,
                name: name,
                preview: preview,
                show: function(){
                    if (this.layer.getVisible()) return;

                    for (var key in basemap) {
                        if (basemap.hasOwnProperty(key)){
                            basemap[key].layer.setVisible(false);
                        }
                    }
                    this.layer.setVisible(true);

                },
                isVisible: function(){
                    return this.layer.getVisible();
                }
            };
        };






      //Add default country basemap
        var vmap = new ol.layer.Tile({
            source: new ol.source.XYZ({
                url: 'map/tile/countries/{z}/{x}/{y}/'
            })
        });
        addBaseMap("vmap", "VMAP", vmap, true);



      //Add Open Street Map (OSM)
        var osm = new ol.layer.Tile({
            source: new ol.source.OSM()
        });
        addBaseMap("osm", "OSM", osm, false);



      //Add additional basemaps
        get("map/layers/basemaps", {
            success: function(text){
                var basemaps = JSON.parse(text);

                var hereDay = getMapLayer("here_basemap", "normal.day", basemaps);
                addBaseMap("day", "Here", hereDay, false);

                /*
                var hereNight = getMapLayer("here_basemap", "normal.night.grey", basemaps);
                addBaseMap("night", "Here (Night)", hereNight, false);


                var hereGray = getMapLayer("here_basemap", "reduced.day", basemaps);
                addBaseMap("gray", "Here (Gray)", hereGray, false);
                */

                var googleMaps = getMapLayer("google", "google", basemaps);
                addBaseMap("google", "Google", googleMaps, false);

                /*
                var bingMaps = getMapLayer("bing", "Road", basemaps);
                addBaseMap("bing", "Bing", bingMaps, false);

                var aerial = getMapLayer("bing", "Aerial", basemaps);
                addBaseMap("aerial", "Aerial", aerial, false);
                */

                var bingMaps = getMapLayer("bing2", "r", basemaps);
                addBaseMap("bing", "Bing", bingMaps, false);

                var aerial = getMapLayer("bing2", "a", basemaps);
                addBaseMap("aerial", "Aerial", aerial, false);

                /*
                var blank = new ol.layer.Tile({
                    source: new ol.source.XYZ({
                       url: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWNgYGD4DwABBAEAHnOcQAAAAABJRU5ErkJggg=="
                    })
                });
                addBaseMap("blank", "Blank", blank, false);
                */
            }
        });






        layer.measurement = createLineLayer();
        layer.drawing = createLineLayer();
        layer.aoi = new ol.layer.Vector({
            source: new ol.source.Vector()
        });

    };


  //**************************************************************************
  //** createLineLayer
  //**************************************************************************
    var createLineLayer = function(color){
        var layer = new ol.layer.Vector({
            source: new ol.source.Vector({})
        });
        return layer;
    };




  //**************************************************************************
  //** createMap
  //**************************************************************************
    var createMap = function(parent){

      //Watch for resize events
        addResizeListener(parent, resize);



      //Instantiate map
        map = new com.kartographia.Map(parent,{
            basemap: null,
            style: {
                info: "map-info",
                coord: "map-coords"
            }
        });


      //Create layers and add them to the map
        createMapLayers();
        for (var layerName in layer) {
            if (layer.hasOwnProperty(layerName)){
                var lyr = layer[layerName];
                map.addLayer(lyr);
            }
        }



      //Watch for key events
        if (document.addEventListener) {
            document.addEventListener("keydown", function(e){
                if (e.keyCode===16){
                    shiftIsPressed = true;
                }

                if (e.keyCode===17){
                    cntrlIsPressed = true;
                    //if (map && layer.points.getVisible()) map.enableBoxSelect();
                }

                if (e.keyCode===18){
                    altIsPressed = true;
                }

              //Prevent native browser shortcuts (ctrl+a,h,o,p,s,...)
                if ((e.keyCode == 65 || e.keyCode == 72 || e.keyCode == 79 || e.keyCode == 80 || e.keyCode == 83) &&
                (navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)) {
                    e.preventDefault();
                    e.stopPropagation();
                }

            });
            document.addEventListener("keyup", function(e){

                if (e.keyCode===16){
                    shiftIsPressed = false;
                }

                if (e.keyCode===17){
                    cntrlIsPressed = false;
                    if (map) map.enableBoxSelect(false);
                }

                if (e.keyCode===18){
                    altIsPressed = false;
                }


                if (cntrlIsPressed){


                  //Select all (ctrl+a)
                    if (e.keyCode===65){
                        selectAll();
                    }


                  //Undo (ctrl+z)
                    if (e.keyCode===90){
                        if (altIsPressed){
                            if (map) map.next();
                        }
                        else{
                            if (map){
                                if (draw) draw.removeLastPoint();
                                else map.back();
                            }
                        }
                    }

                    if (e.keyCode===89){
                        if (map) map.next();
                    }

                }
            });
        }

        map.onMouseClick = function(lat, lon){
            //selectFeature(lat, lon);
        };

        map.onBoxSelect = function(wkt, coords){

        };
    };







  //**************************************************************************
  //** createFacet
  //**************************************************************************
    var createFacet = function(label, parent){

        var div = document.createElement("div");
        div.className = "facet-panel";
        parent.appendChild(div);

        var header = document.createElement("div");
        header.className = "facet-header";
        div.appendChild(header);

        var title = document.createElement("div");
        title.className = "facet-title";
        title.innerHTML = label;
        header.appendChild(title);

        var body = document.createElement("div");
        body.className = "facet-body";
        div.appendChild(body);
    };


  //**************************************************************************
  //** createToolbar
  //**************************************************************************
    var createToolbar = function(parent){
        var toolbar = document.createElement("div");
        toolbar.className = "panel-toolbar";
        parent.appendChild(toolbar);



      //Select button
        var selectButton = createButton(toolbar, {
            label: "Select",
            icon: "pointerIcon",
            toggle: true
        });
        selectButton.onClick = function(){

        };


      //Draw button
        var drawButton = createButton(toolbar, {
            label: "Draw Line",
            icon: "polylineIcon",
            toggle: true
        });
        drawButton.onClick = function(){
            draw = new ol.interaction.Draw({
                source: layer.drawing.getSource(),
                type: 'LineString' //vs Polygon
            });


            draw.on('drawend', function(evt) {
                //var geom = evt.feature.getGeometry();
                //var wkt = wktFormatter.writeGeometry(geom.clone().transform('EPSG:3857','EPSG:4326'));
                map.getMap().removeInteraction(draw);
                draw = null;
                drawButton.toggle();
            });

            map.getMap().addInteraction(draw);
        };


    };






  //**************************************************************************
  //** createButton
  //**************************************************************************
    var createButton = function(toolbar, btn){

        btn.style = JSON.parse(JSON.stringify(style.toolbarButton)); //<-- clone the style config!
        if (btn.icon){
            btn.style.icon = "toolbar-button-icon " + btn.icon;
            delete btn.icon;
        }


        if (btn.menu===true){
            btn.style.arrow = "toolbar-button-menu-icon";
            btn.style.menu = "menu-panel";
            btn.style.select = "panel-toolbar-menubutton-selected";
        }

        return new javaxt.dhtml.Button(toolbar, btn);
    };




  //**************************************************************************
  //** createSidePanel
  //**************************************************************************
    var createSidePanel = function(parent){

      //Create panel
        var panel = document.createElement('div');
        panel.className = "side-panel";
        panel.style.height = "100%";
        parent.appendChild(panel);


      //Create carousel
        var carousel = new javaxt.dhtml.Carousel(panel,{
            loop: true,
            animate: true,
            drag: false
        });


      //Add country list to the carousel
        var div = document.createElement("div");
        div.style.height = "100%";
        var countryList = new com.kartographia.gazetter.CountryList(div);
        carousel.add(div);


      //Add country details to the carousel
        div = div.cloneNode(false);
        var countryDetails = new com.kartographia.gazetter.CountryDetails(div);
        carousel.add(div);


        countryDetails.onClose = function(){
            carousel.back();
        };

        countryList.onSelect = function(countryCode){
            showDetails(countryCode);
        };

        countryList.onKeyPress = function(keyCode, countryCode){
            if (keyCode===13 || keyCode===39){ //right arrow or enter
                showDetails(countryCode);
            }
            else if (keyCode===38 || keyCode===40){ //up and down arrows
                zoomTo(countryCode);
            }
        };


        var showDetails = function(countryCode){
            get("country/names/" + countryCode, {
                success: function(text){

                    var country = {
                        countryCode: countryCode,
                        names: JSON.parse(text)
                    };

                    countryDetails.update(country, function(){
                        carousel.next();

                        setTimeout(function(){
                            zoomTo(countryCode);
                        }, 500);

                    });

                },
                failure: function(response){
                    alert(response);
                }
            });
        };

        var zoomTo = function(countryCode){
            get("country/extents/" + countryCode, {
                success: function(text){
                    var extents = JSON.parse(text);
                    layer.aoi.clear();
                    for (var i=0; i<extents.length; i++){
                        layer.aoi.addFeature(extents[i]);
                    }
                    map.setExtent(layer.aoi.getExtent());
                },
                failure: function(response){
                    alert(response);
                }
            });
        };


        /*
        carousel.onChange = function(currPanel, prevPanel){
            if (currPanel===countryDetails.el.parentNode){
                console.log("Zoom to Country!");
            }
            else{
            }
        };
        */
    };





  //**************************************************************************
  //** createGridPanel
  //**************************************************************************
    var createGridPanel = function(parent){



        var dataGrid = new javaxt.dhtml.DataGrid(parent, {
            style: style.table,
            url: "device/list",
            //payload: filter,
            columns: [
                //{header: 'ID', field: 'id', width:'90'},
                {header: 'Name', field: 'name', width:'100%'},
                {header: 'CC', field: 'country_code', width:'75'},
                {header: 'Type', field: 'type', width:'250'},
                {header: 'Source', field: 'source', width:'190'},
                {header: 'Rank', field: 'rank', width:'75'},
                {header: 'Last Update', field: 'last_modified', width:'190'}
            ],
            getResponse: function(url, payload, callback){
                var request = new XMLHttpRequest();
                request.open("POST", url);
                request.onreadystatechange = function(){
                    if (request.readyState === 4) {
                        callback.apply(me, [request]);
                    }
                };
                if (payload){
                    request.send(JSON.stringify(payload));
                }
                else{
                    request.send();
                }
            },
            parseResponse: function(request){
                var json = JSON.parse(request.responseText);
                var rows = json.locations;
                var devices = getMap(json.devices);
                var carriers = getMap(json.carriers);
                var applications = getMap(json.applications);
                var userAgents = getMap(json.userAgents);
                for (var i=0; i<rows.length; i++){
                    var row = rows[i];
                    row.device = devices[row.device+""];
                    row.carrier = carriers[row.carrier+""];
                    row.application = applications[row.application+""];
                    row.userAgent = userAgents[row.userAgent+""];
                }
                return rows;
            },
            update: function(row, deviceLocation){
                var device = deviceLocation.device;
            }
        });
    };


  //**************************************************************************
  //** resize
  //**************************************************************************
    var resize = function(){
        if (map) map.updateSize();
    };




  //**************************************************************************
  //** Utils
  //**************************************************************************
    var get = javaxt.dhtml.utils.get;
    var post = javaxt.dhtml.utils.post;
    var merge = javaxt.dhtml.utils.merge;
    var isDirty = javaxt.dhtml.utils.isDirty;
    var createTable = javaxt.dhtml.utils.createTable;
    var addResizeListener = javaxt.dhtml.utils.addResizeListener;


    var addOverflow = com.kartographia.utils.addOverflow;
    var getMapLayer = com.kartographia.utils.getMapLayer;

    init();
};