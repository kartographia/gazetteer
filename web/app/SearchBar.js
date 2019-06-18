if(!com) var com={};
if(!com.kartographia) com.kartographia={};
if(!com.kartographia.gazetter) com.kartographia.gazetter={};

//******************************************************************************
//**  SearchBar
//******************************************************************************
/**
 *   Search box with drop-down list
 *
 ******************************************************************************/

com.kartographia.gazetter.SearchBar = function(parent, config) {

    var me = this;

  //**************************************************************************
  //** Constructor
  //**************************************************************************
    var init = function(){
        var input = document.createElement("input");
        input.type = "text";
        input.className = "header-search-input";
        input.placeholder = "Find City...";
        input.setAttribute("autocomplete", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("autocapitalize", "off");
        input.setAttribute("spellcheck", "false");

        input.oninput = function(){
            var filter = input.value.replace(/^\s*/, "").replace(/\s*$/, "").toLowerCase();

        };
        input.onpaste = input.oninput;
        input.onpropertychange = input.oninput;


      //Watch for down arrow events
        document.addEventListener("keyup", function(e){
            if (document.activeElement!==input) return;
            if (e.keyCode===40){

              //Focus on the first item in the list
                setTimeout(function(){

                },50);
            }
        });


        parent.appendChild(input);
        me.el = input;
    };


  //**************************************************************************
  //** onSelect
  //**************************************************************************
    this.onSelect = function(){};

    init();
};