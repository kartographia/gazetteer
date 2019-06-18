if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  MapMenu
//******************************************************************************
/**
 *   Used to generate a list of map tiles that a user can select and use as a
 *   basemap
 *
 ******************************************************************************/

com.kartographia.gazetter.MapMenu = function(parent, config) {

    var me = this;
    var menu;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){
        menu = document.createElement("div");
        parent.appendChild(menu);
        me.update();
        me.el = menu;
    };


  //**************************************************************************
  //** show
  //**************************************************************************
    this.show = function(){
        me.el.style.display = '';
    };


  //**************************************************************************
  //** hide
  //**************************************************************************
    this.hide = function(){
        me.el.style.display = 'none';
    };


  //**************************************************************************
  //** update
  //**************************************************************************
    this.update = function(){
        for (var key in config.basemap) {
            if (config.basemap.hasOwnProperty(key)){
                var basemap = config.basemap[key];
                var div = basemap.div;
                if (!div){
                    basemap.div = createTile(basemap);
                }
            }
        }
    };


  //**************************************************************************
  //** createTile
  //**************************************************************************
    var createTile = function(basemap){

        var div = document.createElement("div");
        div.className = "map-tile";
        menu.appendChild(div);

        var label = document.createElement("div");
        label.className = "map-tile-label map-info";
        label.style.zIndex = 1;
        label.innerHTML = basemap.name;
        div.appendChild(label);

        var preview = document.createElement("div");
        preview.style.position = "absolute";
        div.appendChild(preview);

        var img = document.createElement("img");
        img.src = basemap.preview;
        preview.appendChild(img);


        div.onclick = function(){
            basemap.show();
        };

        return div;
    };

    init();

};