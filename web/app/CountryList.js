if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  CountryList
//******************************************************************************
/**
 *   Panel used to render a list countries. Features a custom list view with
 *   that highlights rows using mouse movements and arrow keys.
 *
 ******************************************************************************/

com.kartographia.gazetter.CountryList = function(parent, config) {

    var me = this;
    var menuOptions;
    var hover = true;
    var position = {
        x: 0,
        y: 0
    };



  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){

      //Create table with 3 rows
        var table = createTable();
        var tbody = table.firstChild;
        var tr, td;


      //Create header (row 1)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        tr.appendChild(td);
        createHeader(td);


      //Create search (row 2)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        tr.appendChild(td);
        createSearch(td);


      //Create body (row 2)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        tr.appendChild(td);
        createBody(td);

        parent.appendChild(table);
        me.el = table;
    };


  //**************************************************************************
  //** onSelect
  //**************************************************************************
  /** Called whenever a user clicks on a row or presses a either the enter or
   *  right arrow key when a row is highlighted.
   */
    this.onSelect = function(countryCode){};

    this.onKeyPress = function(keyCode, countryCode){};


  //**************************************************************************
  //** createHeader
  //**************************************************************************
    var createHeader = function(parent){

        var headerDiv = document.createElement('div');
        headerDiv.className = "side-panel-header";
        parent.appendChild(headerDiv);

        var titleDiv = document.createElement('div');
        titleDiv.className = "side-panel-title";
        titleDiv.innerHTML = "Select Country";
        headerDiv.appendChild(titleDiv);

    };


  //**************************************************************************
  //** createSearch
  //**************************************************************************
    var createSearch = function(parent){
        var div = document.createElement('div');
        div.className = "side-panel-search-row";
        parent.appendChild(div);

        var input = document.createElement("input");
        input.className = "side-panel-search-input";
        input.placeholder = "Search...";
        input.setAttribute("autocomplete", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("autocapitalize", "off");
        input.setAttribute("spellcheck", "false");
        div.appendChild(input);


        input.oninput = function(){
            var filter = input.value.replace(/^\s*/, "").replace(/\s*$/, "").toLowerCase();
            for (var i=0; i<menuOptions.childNodes.length; i++){
                var div = menuOptions.childNodes[i];

                if (filter.length==0){
                    div.style.display = "";
                    div.style.visibility = "";
                }
                else{
                    var txt = div.innerHTML.toLowerCase();

                    var idx = txt.indexOf(filter);
                    if (idx>0){
                        idx = txt.indexOf(" " + filter);
                    }

                    if (idx===-1){
                        div.style.display = "none";
                        div.style.visibility = "hidden";
                    }
                    else{
                        div.style.display = "";
                        div.style.visibility = "";
                    }
                }

                menuOptions.update();
            }

        };
        input.onpaste = input.oninput;
        input.onpropertychange = input.oninput;



      //Watch for down arrow events
        document.addEventListener("keyup", function(e){
            if (document.activeElement!==input) return;
            if (e.keyCode===40){

              //Focus on the first item in the list. Add slight timeout so the
              //other event listener doesn't fire
                setTimeout(function(){
                    if (menuOptions){
                        for (var i=0; i<menuOptions.childNodes.length; i++){
                            var div = menuOptions.childNodes[i];
                            if (div.style.display!=="none"){
                                div.focus();
                                break;
                            }
                        }

                    }
                },50);
            }
        });

    };


  //**************************************************************************
  //** createBody
  //**************************************************************************
    var createBody = function(td){

      //Add overflow to the body
        var div = addOverflow(td);
        menuOptions = div.innerDiv;
        menuOptions.update = div.update;

      //Populate overflow div with a list of countries
        get("countries", {
            success: function(text){
                var countries = JSON.parse(text);
                for (var i=0; i<countries.length; i++){
                    var country = countries[i];
                    var row = document.createElement("div");
                    row.className = "side-panel-row";
                    row.innerHTML = country.name;
                    row.countryCode = country.countryCode;
                    row.tabIndex = -1; //required for focus
                    row.onclick = function(e){
                        this.focus();
                        me.onSelect(this.countryCode);

                        /*
                      //Check if the client click on the psedo element
                        var after = window.getComputedStyle(this, ':after');
                        var width = parseInt(after.width);
                        width += parseInt(after.getPropertyValue('padding-left'));
                        width += parseInt(after.getPropertyValue('padding-right'));
                        width += parseInt(after.getPropertyValue('margin-left'));
                        width += parseInt(after.getPropertyValue('margin-right'));
                        if (e.offsetX>this.offsetWidth-width){
                            me.onSelect(this.countryCode);
                        }
                        */
                    };


                    row.onfocus = function(){

                      //Highlight selected row
                        for (var i=0; i<this.parentNode.childNodes.length; i++){
                            var el = this.parentNode.childNodes[i];
                            if (el!==this){
                                el.className = "side-panel-row";
                            }
                        }
                        if(this.className === "side-panel-row")
                        this.className += " selected";



                      //Fire scroll event as needed. Focus events will make the
                      //overflow div to scroll to the focused element if the
                      //element is at the bottom of the list. However, the
                      //overflow div will not scroll up if the focused element
                      //is at the top of the list (or above)
                        var el = this;
                        var overflowDiv = el.offsetParent.parentNode;
                        var top = javaxt.dhtml.utils.getRect(overflowDiv).top;
                        var bottom = javaxt.dhtml.utils.getRect(el).bottom;
                        if (bottom<=top){
                            dispatchEvent("scroll", overflowDiv);
                        }
                    };


                    row.onmousemove = function(e){

                        if (e.clientX!=position.x || e.clientY!=position.y){
                            position.x = e.clientX;
                            position.y = e.clientY;
                            hover = true;
                        }

                        if (hover) this.focus();

                    };



                    div.innerDiv.appendChild(row);
                }
                div.update();

            },
            failure: function(response){
                alert(response);
            }
        });



      //Watch for scroll events on the overflow div. This is particularly
      //important if iScroll is installed. Otherwise the scroll indicator will
      //disappear.
        div.innerDiv.parentNode.onscroll = function(){
            hover = false;

            for (var i=0; i<div.innerDiv.childNodes.length; i++){
                var el = div.innerDiv.childNodes[i];
                if (el===document.activeElement){
                    div.scrollToElement(el);
                    break;
                }
            }
        };


        var getSelectedRow = function(){
            for (var i=0; i<div.innerDiv.childNodes.length; i++){
                var el = div.innerDiv.childNodes[i];
                if (el.className==="side-panel-row selected"){
                    return el;
                }
            }
            return null;
        };




      //Watch for arrow up/down events and update the selection
        document.addEventListener("keyup", function(e){

            if (document.activeElement.parentNode!==div.innerDiv) return;

            if (e.keyCode===38){ //up arrow
                var el = getSelectedRow();
                if (el.previousSibling) el.previousSibling.focus();
            }
            else if (e.keyCode===40){ //down arrow
                var el = getSelectedRow();
                if (el.nextSibling) el.nextSibling.focus();
            }
            else{
                var el = getSelectedRow();
                me.onKeyPress(e.keyCode, el.countryCode);
            }
        });


      //Disable up/down listener when the client moves the mouse away from the panel
        td.onmouseout = function(e){

            var el = e.toElement || e.relatedTarget;
            if (el==null) return;
            if (el.parentNode === this || el === this) {
                return;
            }
            while (el.parentNode){
                if (el.parentNode === this.firstChild) return;
                el = el.parentNode;
            }

            el = getSelectedRow();
            if (el) el.blur();
        };


      //Re-enable up/down listener when the client moves over the panel
        td.onmouseover = function(e){
            var el = getSelectedRow();
            if (el) el.focus();
        };


    };


  //**************************************************************************
  //** dispatchEvent
  //**************************************************************************
  /** Fires a given window event (e.g. "load")
   */
    var dispatchEvent = function(name, target){
        var evt;
        try{
            evt = new Event(name);
        }
        catch(e){ //e.g. IE
            evt = document.createEvent('Event');
            evt.initEvent(name, false, false);
        }

        if (!target) target = window;

        setTimeout(function(){
            target.dispatchEvent(evt);
        },50);
    };


  //**************************************************************************
  //** Utils
  //**************************************************************************
    var get = javaxt.dhtml.utils.get;
    var createTable = javaxt.dhtml.utils.createTable;
    var addOverflow = com.kartographia.utils.addOverflow;

    init();

};