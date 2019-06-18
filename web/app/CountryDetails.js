if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  CountryDetails
//******************************************************************************
/**
 *   Panel used to render country details
 *
 ******************************************************************************/

com.kartographia.gazetter.CountryDetails = function(parent, config) {

    var me = this;
    var countryCode;
    var countryNames;
    var groups;
    var languageCodes = {};

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){

        if (!config) config = {};
        if (!config.style) config.style = javaxt.dhtml.style.default;


      //Create table with 2 rows
        var table = createTable();
        table.style.color = "inherit";
        var tbody = table.firstChild;
        var tr, td;


      //Create header (row 1)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        tr.appendChild(td);
        createHeader(td);


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


      //Get language codes
        get("../reference/LanguageCodes.txt", {
            success: function(text){
                var rows = text.split("\n");
                for (var i=0; i<rows.length; i++){
                    var row = rows[i].trim();
                    if (row.length>0){
                        var col = row.split("\t");
                        languageCodes[col[0].trim()] = col[1].trim();
                    }
                }
            }
        });

    };

  //**************************************************************************
  //** onClose
  //**************************************************************************
  /** Called whenever the client clicks on the X close button in the header
   */
    this.onClose = function(){};




  //**************************************************************************
  //** update
  //**************************************************************************
    this.update = function(country, callback){

      //Update country "logo"
        countryCode.innerHTML = country.countryCode;



      //Update list of country names (grouped by language codes)
        countryNames.innerHTML = "";
        groups = {};
        if (country.names){

            for (var i=0; i<country.names.length; i++){
                var name = country.names[i];
                var group = groups[name.languageCode];
                if (!group){
                    group = [];
                    groups[name.languageCode] = group;
                }
                group.push(name);
            }


          //Render english names
            addNames(groups["eng"]);


          //Render names in other languages
            for (var languageCode in groups) {
                if (groups.hasOwnProperty(languageCode)){
                    if (languageCode!=="eng"){
                        var group = groups[languageCode];
                        addNames(group);
                    }
                }
            }

            countryNames.update();
        }

        if (callback) callback.apply(me, []);
    };



  //**************************************************************************
  //** createHeader
  //**************************************************************************
    var createHeader = function(parent){

        var headerDiv = document.createElement('div');
        headerDiv.className = "side-panel-header";
        parent.appendChild(headerDiv);

        var titleDiv = document.createElement('div');
        titleDiv.className = "side-panel-title";
        titleDiv.innerHTML = "Country Details";
        headerDiv.appendChild(titleDiv);


        var closeButton = document.createElement('div');
        closeButton.className = "side-panel-close";
        headerDiv.appendChild(closeButton);
        closeButton.onclick = function(){
            me.onClose();
        };

    };



  //**************************************************************************
  //** createBody
  //**************************************************************************
    var createBody = function(parent){

      //Create table with 2 rows
        var table = createTable();
        table.style.color = "inherit";
        var tbody = table.firstChild;
        var tr, td;
        parent.appendChild(table);


      //Create (row 1)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "200px";
        td.style.textAlign = "center";
        createLogo(td);
        tr.appendChild(td);



      //Create body (row 2)
        tr = document.createElement("tr");
        tbody.appendChild(tr);
        td = document.createElement("td");
        td.style.width = "100%";
        td.style.height = "100%";
        tr.appendChild(td);


      //Add overflow to the body
        var div = addOverflow(td);
        countryNames = div.innerDiv;
        countryNames.update = div.update;

    };


  //**************************************************************************
  //** addNames
  //**************************************************************************
    var addNames = function(names){
        if (!names) return;

        names.sort(function(a, b) {
            return b.name.length-a.name.length;
        });

        var group = document.createElement("div");
        group.className = "country-names";
        countryNames.appendChild(group);


        var div = document.createElement("div");
        div.className = "country-name-header";
        div.innerHTML = getLanguageName(names[0].languageCode) + " Names";
        group.appendChild(div);


        var ul = document.createElement("div");
        group.appendChild(ul);
        for (var i=0; i<names.length; i++){
            var li = document.createElement("div");
            li.className = "country-name-list";
            li.innerHTML = names[i].name;
            ul.appendChild(li);


            li.onclick = function(){
                for (var i=0; i<this.parentNode.childNodes.length; i++){
                    var el = this.parentNode.childNodes[i];
                    if (el!==this){
                        el.className = "country-name-list";
                    }
                }
                if(this.className === "country-name-list")
                this.className += " selected";
            };
        }

    };


  //**************************************************************************
  //** getLanguageName
  //**************************************************************************
    var getLanguageName = function(languageCode){
        var name = languageCodes[languageCode];
        if (name) return name;
        else return languageCode;
    };


  //**************************************************************************
  //** createLogo
  //**************************************************************************
    var createLogo = function(parent){

        var div = document.createElement("div");
        div.className = "country-logo";
        parent.appendChild(div);

        var outline = document.createElement("div");
        outline.className = "country-logo-shadow";
        div.appendChild(outline);

        countryCode = document.createElement("span");
        div.appendChild(countryCode);
    };


  //**************************************************************************
  //** Utils
  //**************************************************************************
    var get = javaxt.dhtml.utils.get;
    var createTable = javaxt.dhtml.utils.createTable;
    var addOverflow = com.kartographia.utils.addOverflow;

    init();
};