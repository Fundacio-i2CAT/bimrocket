BIMROCKET-Quarkus
=================

Start-up:
  <bimrocket-quarkus-home>/startup(.sh|.cmd)

Shutdown:
  <bimrocket-quarkus-home>/shutdown(.sh|.cmd)

Data directory:
  By default, bimrocket data is stored in the <user.home>/bimrocket directory.
  This directory can be changed via the BIMROCKET_DATA_PATH JVM property or
  environ variable.

Service configuration file:
  By default, the bimrocket service configuration parameters are specified in
  the bimrocket-server.yaml file located within the data directory.

Default access URL:
  http://localhost:8080

  The listening port (8080 by default) is defined in the tomcat.port property of
  the <bimrocket-quarkus-home>/conf/catalina.properties file.

Default user:
  User: admin
  Password: bimrocket

  This password is defined in the services.security.adminPassword property of
  the service configuration file (bimrocket-server.yaml).

More information:
  https://github.com/bimrocket/bimrocket

