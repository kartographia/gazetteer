# Introduction
The gazetteer library and command line tools are used to download, extract,
transform, load, and lookup place names.

## Library
The gazetteer library includes data loaders and search tools. It also includes
a brute force entity extraction capability used to find country names in text.


## Command Line Tools
The gazetteer command line tools are used to download data and load records
into a database. The command line tools require a config file (see below).


### Downloading Data
To download data, simply specify a source. Example:
```
java -jar gazetteer.jar -download NGA
```

### Loading Data
To load records into a database, you need to specify a path to a file and a source.
Here are a few examples:
```
java -jar gazetteer.jar -load /path/to/ne_10m_admin_0_countries.shp -source NaturalEarth
java -jar gazetteer.jar -load /path/to/DomesticNames.zip -source USGS
java -jar gazetteer.jar -load /path/to/geonames.zip -source NGA
```

### Config File

Some command line tools (e.g. -import) require a config file with database
connection information and paths to files and folders. Example:

```json
{

    "database" : {
        "driver" : "PostgreSQL",
        "host" : "localhost:5432",
        "name" : "kartographia",
        "username" : "postgres",
        "password" : "***************",
        "maxConnections" : 50,
        "schema" : "models/gazetteer.sql",
        "updates" : "models/updates.sql"
    },

    "data" : "data/",

    "downloads" : "/path/to/downloads/",

}
```

Note that paths can be relative (relative to the config file) or absolute.
If you are on Windows, you can use either `/` or `\\` path separators.

The application will try to find a `config.json` file next to the `gazetteer.jar` file.
You can also specify a path to a `config.json` file using the `-config` argument.


## Database and Models

The gazetteer library relies on a relational database (PostgreSQL) to store place names.
The models and associated schema are in the `models` directory. The data is stored
in a `gazetteer` schema.

### Models Directory
The models directory contains the following
- `gazetteer.js` file contains model specs
- `gazetteer.sql`file contains the schema generated from the model
- `updates.sql` file contains updates to gazetteer.sql
- `audit.sql` file contains an audit trigger (see Auditing section)


### Auditing
Auditing is made possible by the audit trigger found here:
https://github.com/2ndQuadrant/audit-trigger


To enable auditing, run the following 2 commands:
```sql
SELECT audit.audit_table('place');
SELECT audit.audit_table('place_name');
```

To disable auditing, run the following commands:
```sql
DROP TRIGGER audit_trigger_row ON place;
DROP TRIGGER audit_trigger_stm ON place;

DROP TRIGGER audit_trigger_row ON place_name;
DROP TRIGGER audit_trigger_stm ON place_name;
```

To view the audit log, query the audit.logged_actions table. Example:
```sql
select * from audit.logged_actions;
```

Reference:
https://wiki.postgresql.org/wiki/Audit_trigger_91plus

### ORM

Several classes in this project were generated using javaxt-orm and rely on the javaxt.sql.Model class

