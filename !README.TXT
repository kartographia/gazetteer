-------------------------------------------------
Introduction
-------------------------------------------------

This directory contains sources for the Gazetter App.


config.json is a json file with configuration information required to run the app. 




-------------------------------------------------
Models
-------------------------------------------------

Several classes in this project were generated using javaxt-orm and rely on the javaxt.sql.Model class

The models directory contains model specs (gazetter.js) and database schema (gazetter.sql)

The updates.sql file contains updates to gazetter.sql

The audit.sql file contains an audit trigger (see Auditing section)




-------------------------------------------------
Updating max id after deletes
-------------------------------------------------

SELECT setval('place_name_id_seq', (SELECT MAX(id) FROM place_name));
SELECT setval('place_id_seq', (SELECT MAX(id) FROM place));


-------------------------------------------------
Auditing
-------------------------------------------------

Auditing is made possible by the audit trigger found here:
https://github.com/2ndQuadrant/audit-trigger


To enable auditing, run the following 2 commands:

SELECT audit.audit_table('place');
SELECT audit.audit_table('place_name');


To disable auditing, run the following commands:

DROP TRIGGER audit_trigger_row ON place;
DROP TRIGGER audit_trigger_stm ON place;


DROP TRIGGER audit_trigger_row ON place_name;
DROP TRIGGER audit_trigger_stm ON place_name;


To view the audit log, query the audit.logged_actions table. Example:

select * from audit.logged_actions;



Reference:
https://wiki.postgresql.org/wiki/Audit_trigger_91plus




-------------------------------------------------
Tablespace
-------------------------------------------------


sudo mkdir -p /mnt/nvme/postgresql/k2
sudo chown postgres.postgres /mnt/nvme/postgresql/k2
sudo -i -u postgres psql -c "CREATE TABLESPACE k2 LOCATION '/mnt/nvme/postgresql/k2';"
sudo -i -u postgres psql -c "create database k2 tablespace k2;"
