package io.vertx.starter.database;

public interface DatabaseConstants {
  String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
  String DEFAULT_WIKIDB_JDBC_URL = "jdbc:hsqldb:file:db/wiki";
  String DEFAULT_WIKIDB_JDBC_DRIVER_CLASS = "org.hsqldb.jdbcDriver";
  String DEFAULT_WIKIDB_MONGO_HOST = "127.0.0.1";
  Integer DEFAULT_WIKIDB_MONGO_PORT = 27017;
  int DEFAULT_JDBC_MAX_POOL_SIZE = 30;
}
