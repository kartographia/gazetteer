if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  MenuPanel
//******************************************************************************
/**
 *   General purpose panel used to display settings, filters, etc
 *
 ******************************************************************************/

com.kartographia.gazetter.MenuPanel = function(parent, config) {

    var me = this;
    var menu, menuContainer;
    var title, body;
    var menuItems = [];

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){

        if (!config) config = {};
        var fx = config.fx;
        if (!fx) fx = new javaxt.dhtml.Effects();


        menuContainer = document.createElement("div");
        menuContainer.style.position = "relative";
        menuContainer.style.width = "0px";
        menuContainer.style.height = "100%";
        menuContainer.style.display = "inline-block";
        menuContainer.style.float = "right";
        menuContainer.style.zIndex = -1;
        parent.appendChild(menuContainer);


        menu = document.createElement("div");
        menu.className = "menu-panel";
        menu.style.width = "220px";
        menu.style.left = "-220px";
        menu.style.height = "100%";
        menu.style.position = "absolute";
        menuContainer.appendChild(menu);
        fx.setTransition(menu, "easeInOutCubic", 600);


      //Create table with 2 rows
        var table = createTable();
        var tbody = table.firstChild;
        var tr, td;


      //Create header (row 1)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        tr.appendChild(td);
        title = createHeader(td);


      //Create body (row 2)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        tr.appendChild(td);
        body = addOverflow(td);


        menu.appendChild(table);
        me.el = menuContainer;

    };


  //**************************************************************************
  //** show
  //**************************************************************************
  /** Used to display the menu
   *  @param obj A component to display in the menu (optional)
   *  @param str The title for the menu (optional)
   */
    this.show = function(obj, str){
        menu.style.left = "0px";
        menuContainer.style.zIndex = 1;

        if (obj){
            var addComponent = true;
            for (var i=0; i<menuItems.length; i++){
                if (menuItems[i]==obj){
                    menuItems[i].show();
                    addComponent = false;
                }
                else menuItems[i].hide();
            }

            if (addComponent){
                me.add(obj);
            }
            else{
                body.update();
            }

            me.setTitle(str);
        }
    };


  //**************************************************************************
  //** hide
  //**************************************************************************
    this.hide = function(){
        menu.style.left = "-220px";
        //menuContainer.style.zIndex = -1;
    };


  //**************************************************************************
  //** setTitle
  //**************************************************************************
    this.setTitle = function(str){
        if (str==null) str = "";
        title.innerHTML = str;
    };


  //**************************************************************************
  //** add
  //**************************************************************************
  /** Used to add an item to the menu. Assumes the item is a DHTML component
   *  with a valid DOM element exposed via the "el" property, along with show
   *  and hide methods.
   */
    this.add = function(obj){
        menuItems.push(obj);
        body.innerDiv.appendChild(obj.el);
        body.update();
    };


  //**************************************************************************
  //** createHeader
  //**************************************************************************
    var createHeader = function(parent){

        var headerDiv = document.createElement('div');
        headerDiv.className = "menu-panel-header";
        parent.appendChild(headerDiv);

        var titleDiv = document.createElement('div');
        titleDiv.className = "menu-panel-title";
        titleDiv.innerHTML = "Menu";
        headerDiv.appendChild(titleDiv);


        var closeButton = document.createElement('div');
        closeButton.className = "menu-panel-close";
        headerDiv.appendChild(closeButton);
        closeButton.onclick = function(){
            me.hide();
        };

        return titleDiv;
    };





  //**************************************************************************
  //** Utils
  //**************************************************************************
    var createTable = javaxt.dhtml.utils.createTable;
    var addOverflow = com.kartographia.utils.addOverflow;


    init();

};