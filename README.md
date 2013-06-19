Features
--------

* Immediate search access to local and distributed geospatial catalogues
* Up- and downloading of data, graphics, documents, pdf files and any other content type
* An interactive Web Map Viewer to combine Web Map Services from distributed servers around the world
* Online editing of metadata with a powerful template system
* Scheduled harvesting and synchronization of metadata between distributed catalogs
* Support for OGC-CSW 2.0.2 ISO Profile, OAI-PMH, Z39.50 protocols
* Fine-grained access control with group and user management
* Multi-lingual user interface


Setup
-----

Be sure to include those options in your tomcat setup:

    CATALINA_OPTS="
    -Dgeonetwork.dir=/path/to/geonetwork-data-dir \
    -Dgeonetwork.schema.dir=/path/to/tomcat/webapps/geonetwork/WEB-INF/data/config/schema_plugins \
    -Dgeonetwork.jeeves.configuration.overrides.file=/path/to/tomcat/webapps/geonetwork/WEB-INF/config-overrides-georchestra.xml
    "


Migrating
---------

Here's a short procedure to follow if you wish to upgrade your existing geOrchestra GeoNetwork from 2.6.3 to 2.10

* upgrade the security proxy

* backup your existing geonetwork data dir and database

* stop the tomcat where geonetwork is installed

* update your geonetwork tomcat options according to the above setup

* upgrade your geonetwork database:
  * apply the following [SQL patch](https://github.com/georchestra/geonetwork/blob/georchestra-29/web/src/main/webapp/WEB-INF/classes/setup/sql-georchestra/migrate/1211/db-migrate-default.sql) 
  * apply this other [SQL patch](https://github.com/georchestra/geonetwork/blob/georchestra-29/web/src/main/webapp/WEB-INF/classes/setup/sql-georchestra/migrate/1302/db-migrate-default.sql)

* upgrade your geonetwork_data_dir:
  * unzip your new geonetwork.war in /tmp
  * then:
 
    rm -rf geonetwork_data_dir/config
    cp -r /tmp/geonetwork_war_unzipped/WEB-INF/data/config geonetwork_data_dir
    mkdir /tmp/mydata
    mv geonetwork_data_dir/data/* /tmp/mydata
    mkdir geonetwork_data_dir/data/metadata_data
    mv /tmp/mydata/* geonetwork_data_dir/data/metadata_data
    rm -f geonetwork_data_dir/config/schemaplugin-uri-catalog.xml

* check rights on your geonetwork data dir files (must be RW by tomcat user)

* edit your apache config and add the following proxypass :

    ProxyPass /geonetwork-private/ ajp://localhost:8009/geonetwork/
    ProxyPassReverse /geonetwork-private/ ajp://localhost.com:8009/geonetwork/

* restart apache + tomcat and you're done.
