if(!gypsy) var gypsy={};

//******************************************************************************
//**  SQLEditor
//******************************************************************************
/**
 *   Panel used to execute queries and view results in a grid
 *
 ******************************************************************************/

gypsy.SQLEditor = function(parent, config) {
    this.className = "gypsy.SQLEditor";

    var me = this;
    var tree, sql, grid, gridContainer;
    var runButton, cancelButton;
    var jobID;

    var defaultConfig = {

        queryService: "sql/job/",
        getTables: "sql/tables/",
        pageSize: 50,

        style:{
            container: {
                width: "100%",
                height: "100%"
            },
            leftPanel: {

            }
        }
    };


    var border = "1px solid #383b41";
    var background = "#272a31";


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){

        if (typeof parent === "string"){
            parent = document.getElementById(parent);
        }
        if (!parent) return;


      //Clone the config so we don't modify the original config object
        config = clone(config);


      //Merge clone with default config
        config = merge(config, defaultConfig);


      //Create main div
        var div = document.createElement('div');
        setStyle(div, config.style["container"]);


      //Create table with 2 columns
        var table = createTable();
        var tbody = table.firstChild;
        var tr = document.createElement('tr');
        tbody.appendChild(tr);
        var td;


      //Left column
        var td = document.createElement('td');
        td.style.height = "100%";
        td.style.background = background;
        td.style.borderRight = border;
        tr.appendChild(td);
        createTableView(td);


      //Right column
        td = document.createElement('td');
        td.style.height = "100%";
        td.style.width = "100%";
        td.style.background = "#1c1e23";
        tr.appendChild(td);
        createQueryView(td);


        div.appendChild(table);
        parent.appendChild(div);
        me.el = div;





      //Check whether the panel has been added to the DOM
        var w = me.el.offsetWidth;
        if (w===0 || isNaN(w)){
            var timer;

            var checkWidth = function(){
                var w = me.el.offsetWidth;
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
  /** Called when the panael has been added to the DOM
   */
    var onRender = function(){

      //Get list of tables and update the tree view
        get(config.getTables, {
            success: function(text){

              //Parse response
                var tables = JSON.parse(text).tables;


              //Add nodes to the tree
                tree.addNodes(tables);

            }
        });
    };


  //**************************************************************************
  //** createTableView
  //**************************************************************************
  /** Creates a panel used to render a list of tables in the database. Tables
   *  are rendered in a tree view.
   */
    var createTableView = function(parent){


        var outerDiv = document.createElement("div");
        outerDiv.style.position = "relative";
        outerDiv.style.height = "100%";
        outerDiv.style.width = "210px";
        outerDiv.style.overflow = "auto";
        parent.appendChild(outerDiv);

        var div = document.createElement('div');
        div.style.position = "absolute";
        div.style.width = "100%";
        outerDiv.appendChild(div);



      //Create tree
        tree = new javaxt.dhtml.Tree(div, {
            style:{
                padding: "7px 0 7px 0px",
                backgroundColor: "",
                li: "tree-node"
            }
        });


        tree.onClick = function(item){
            sql.value = "select * from " + item.name;
        };

    };



  //**************************************************************************
  //** createQueryView
  //**************************************************************************
    var createQueryView = function(parent){
        var table = createTable();
        var tbody = table.firstChild;
        var tr, td;

      //Create toolbar
        tr = document.createElement('tr');
        tbody.appendChild(tr);
        td = document.createElement('td');
        tr.appendChild(td);
        td.className = "button-toolbar";
        addButtons(td);


      //Create sql textbox
        tr = document.createElement('tr');
        tbody.appendChild(tr);
        td = document.createElement('td');
        td.style.borderBottom = border;
        tr.appendChild(td);
        sql = document.createElement('textarea');
        //sql.className = "form-input form-input-nofocus";
        sql.style.width = "100%";
        sql.style.height = "240px";
        sql.style.margin = 0;
        sql.style.padding = "5px 10px";
        sql.style.border = "0px none";
        sql.style.resize = "none";
        sql.style.fontFamily = '"Consolas", "Bitstream Vera Sans Mono", "Courier New", Courier, monospace';

        sql.style.color = "#97989c";
        sql.style.background = "inherit";
        td.appendChild(sql);


      //Create results table
        tr = document.createElement('tr');
        tbody.appendChild(tr);
        td = document.createElement('td');
        td.style.height = "100%";
        td.style.verticalAlign = "top";
        td.style.fontFamily = '"Consolas", "Bitstream Vera Sans Mono", "Courier New", Courier, monospace';
        td.style.color = "#97989c";

        tr.appendChild(td);



        gridContainer = td;



        parent.appendChild(table);
    };



  //**************************************************************************
  //** addButtons
  //**************************************************************************
  /** Adds buttons to the toolbar
   */
    var addButtons = function(toolbar){


      //Add button
        runButton = createButton(toolbar, {
            label: "Run",
            //menu: true,
            icon: "runIcon",
            disabled: false
        });
        runButton.onClick = function(){

          //Cancel any current queries
            cancel();

          //Remove current grid
            gridContainer.innerHTML = "";
            grid = null;

            var url = config.queryService;

            var payload = {
                query: sql.value,
                limit: config.pageSize
            };




            getResponse(url, JSON.stringify(payload), function(request){
                var json = JSON.parse(request.responseText);
                var data = parseResponse(json);
                render(data.records, data.columns);
            });

        };





      //Cancel button
        cancelButton = createButton(toolbar, {
            label: "Cancel",
            icon: "deleteIcon",
            disabled: true
        });
        cancelButton.onClick = function(){
            cancel();
        };
    };

  //**************************************************************************
  //** cancel
  //**************************************************************************
  /** Used to cancel the current query
   */
    var cancel = function(){
        if (jobID){
            javaxt.dhtml.utils.delete(config.queryService + jobID,{
                success : function(){
                    cancelButton.disable();
                },
                failure: function(request){
                    //mainMask.hide();
                    cancelButton.disable();

                    if (request.status!==404){
                        showError(request);
                    }
                }
            });
        }
        jobID = null;
    };


  //**************************************************************************
  //** showError
  //**************************************************************************
  /** Used to render error messages returned from the server
   */
    var showError = function(msg){
        cancelButton.disable();

        if (msg.responseText){
            msg = (msg.responseText.length>0 ? msg.responseText : msg.statusText);
        }
        gridContainer.innerHTML = msg;
    };


  //**************************************************************************
  //** getResponse
  //**************************************************************************
  /** Used to execute a sql api request and get a response. Note that queries
   *  are executed asynchronously. This method will pull the sql api until
   *  the query is complete.
   */
    var getResponse = function(url, payload, callback){
        post(url, payload, {
            success : function(text){

                jobID = JSON.parse(text).job_id;
                cancelButton.enable();


              //Periodically check job status
                var timer;
                var checkStatus = function(){
                    if (jobID){
                        var request = get(config.queryService + jobID, {
                            success : function(text){
                                if (text==="pending" || text==="running"){
                                    timer = setTimeout(checkStatus, 250);
                                }
                                else{
                                    clearTimeout(timer);
                                    callback.apply(me, [request]);
                                }
                            },
                            failure: function(response){
                                clearTimeout(timer);
                                showError(response);
                            }
                        });
                    }
                    else{
                        clearTimeout(timer);
                    }
                };
                timer = setTimeout(checkStatus, 250);

            },
            failure: function(response){
                //mainMask.hide();
                showError(response);
            }
        });
    };


  //**************************************************************************
  //** parseResponse
  //**************************************************************************
  /** Used to parse the query response from the sql api
   */
    var parseResponse = function(json){

      //Get rows
        var rows = json.rows;


      //Generate list of columns
        var record = rows[0];
        var columns = [];
        for (var key in record) {
            if (record.hasOwnProperty(key)) {
                columns.push(key);
            }
        }


      //Generate records
        var records = [];
        for (var i=0; i<rows.length; i++){
            var record = [];
            var row = rows[i];
            for (var j=0; j<columns.length; j++){
                var key = columns[j];
                var val = row[key];
                record.push(val);
            }
            records.push(record);
        }


        return {
            columns: columns,
            records: records
        };
    };


  //**************************************************************************
  //** render
  //**************************************************************************
  /** Used to render query results in a grid
   */
    var render = function(records, columns){

        //mainMask.hide();



      //Render results in the grid
        if (grid){
            var currPage = grid.getCurrPage();
            grid.load(records, currPage+1);
        }
        else{


          //Convert list of column names into column definitions
            var arr = [];
            var colWidth = (100/columns.length)+"%";
            for (var i=0; i<columns.length; i++){
                arr.push({
                   header: columns[i],
                   width: colWidth,
                   sortable: false
                });
            }



          //Create grid
            grid = new javaxt.dhtml.DataGrid(gridContainer, {
                columns: arr,
                style: gypsy.style.table,


                url: config.queryService,
                payload: JSON.stringify({
                    query: sql.value
                }),
                limit: config.pageSize,
                getResponse: getResponse,
                parseResponse: function(request){
                    return parseResponse(JSON.parse(request.responseText)).records;
                }
            });


            grid.beforeLoad = function(page){
                //mainMask.show();
                //cancelButton.disable();
            };

            grid.afterLoad = function(){
                //mainMask.hide();
                cancelButton.disable();
            };

            grid.onSelectionChange = function(){

            };

            //grid.load();
            grid.load(records, 1);
        }
    };


  //**************************************************************************
  //** createButton
  //**************************************************************************
    var createButton = function(toolbar, btn){

        btn.style = JSON.parse(JSON.stringify(gypsy.style.toolbarButton)); //<-- clone the style config!
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
  //** Utilites
  //**************************************************************************
  /** Common functions found in Utils.js
   */
    var get = javaxt.dhtml.utils.get;
    var post = javaxt.dhtml.utils.post;
    var del = javaxt.dhtml.utils.delete;
    var clone = javaxt.dhtml.utils.clone;
    var merge = javaxt.dhtml.utils.merge;
    var setStyle = javaxt.dhtml.utils.setStyle;
    var createTable = javaxt.dhtml.utils.createTable;


    init();
};