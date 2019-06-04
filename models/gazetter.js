var package = "com.kartographia.gazetter";
var schema = "gazetter";
var models = {


  //**************************************************************************
  //** Source
  //**************************************************************************
  /** Data provider
   */
    Source: {
        fields: [
            {name: 'name',          type: 'string'}, //USGS, NGA, OSM, etc
            {name: 'description',   type: 'string'},
            {name: 'info',          type: 'json'}
        ],
        constraints: [
            {name: 'name', required: true,  length: 25,  unique: true}
        ]
    },


  //**************************************************************************
  //** Place
  //**************************************************************************
  /** Used to represent a populated place (e.g. city, town, village, etc) or
   *  a significant landmark within a populated place (e.g. building, square,
   *  statue, etc).
   */
    Place: {
        fields: [
            {name: 'countryCode',   type: 'char'},
            {name: 'admin1',        type: 'char'},
            {name: 'admin2',        type: 'string'},
            {name: 'geom',          type: 'geo'},
            {name: 'type',          type: 'string'}, //residential, cultural, administrative, industrial, military, etc
            {name: 'subtype',       type: 'string'}, //city, town, village, building, park, monument, etc
            {name: 'rank',          type: 'int'},    //1-5 for populated places
            {name: 'source',        type: 'Source'},
            {name: 'sourceKey',     type: 'long'},
            {name: 'sourceDate',    type: 'int'}, //YYYYMMDD in utc
            {name: 'info',          type: 'json'},
            {name: 'lastModified',  type: 'date'}
        ],
        hasMany: [
            {model: 'Name',     name: 'names'}
        ],
        constraints: [
            {name: 'countryCode',   required: true,  length: 2},
            {name: 'admin1',        length: 2},
            {name: 'geom',          required: true},
            {name: 'source',        required: true}
        ]
    },


  //**************************************************************************
  //** Name
  //**************************************************************************
  /** Used to represent a name of a place
   */
    Name: {
        fields: [
            {name: 'name',          type: 'string'},
            {name: 'uname',         type: 'string'}, //uppercase varient
            {name: 'languageCode',  type: 'char'}, //eng (English)
            {name: 'type',          type: 'int'}, //1 (Common name), 2 (Formal name), 3 (Varient)

            {name: 'source',        type: 'Source'},
            {name: 'sourceKey',     type: 'long'},
            {name: 'sourceDate',    type: 'int'}, //YYYYMMDD in utc

            {name: 'info',          type: 'json'},
            {name: 'lastModified',  type: 'date'}
        ],
        constraints: [
            {name: 'name',          required: true},
            {name: 'languageCode',  required: true, length: 3},
            {name: 'type',          required: true},
            {name: 'source',        required: true}
        ]
    },


  //**************************************************************************
  //** Edit
  //**************************************************************************
  /** Used in conjunction with the audit_trigger to document changes to the
   *  gazetter
   */
    Edit: {
        fields: [
            {name: 'summary',       type: 'string'},
            {name: 'details',       type: 'string'},
            {name: 'info',          type: 'json'},
            {name: 'startDate',     type: 'date'},
            {name: 'endDate',       type: 'date'}
        ],
        constraints: [
            {name: 'summary',       required: true},
            {name: 'startDate',     required: true}
        ]
    },


  //**************************************************************************
  //** ChangeRequest
  //**************************************************************************
  /** Used
   */
    ChangeRequest: {
        fields: [
            {name: 'place',         type: 'Place'},
            {name: 'info',          type: 'json'},
            {name: 'status',        type: 'string'}, //PENDING, REJECTED, ACCEPTED
            {name: 'lastModified',  type: 'date'}
        ],
        constraints: [
            {name: 'place',         required: true},
            {name: 'info',          required: true},
            {name: 'status',        required: true, length: 10}
        ]
    }
};