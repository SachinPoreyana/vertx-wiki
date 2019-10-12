package io.vertx.starter;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.starter.database.AuthInitializerVerticle;
import io.vertx.starter.database.WikiDatabaseVerticle;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);


  @Override
  public void start(Promise<Void> promise)
  {
    Promise<String> dbvirticalDeploymentPromise = Promise.promise();

    vertx.deployVerticle(new WikiDatabaseVerticle(), dbvirticalDeploymentPromise);

    Future<String> authDeploymentFuture = dbvirticalDeploymentPromise.future().compose(id -> {
      Promise<String> deployPromise = Promise.promise();
      vertx.deployVerticle(new AuthInitializerVerticle(), deployPromise);
      return deployPromise.future();
    });

    authDeploymentFuture.compose(id -> {
      Promise<String> httpVerticleDeploymentPromise = Promise.promise();
      vertx.deployVerticle("io.vertx.starter.http.HttpServerVerticle",
        new DeploymentOptions().setInstances(2),
        httpVerticleDeploymentPromise);
      return httpVerticleDeploymentPromise.future();
    }).setHandler(ar ->
    {
      if (ar.succeeded())
      {
        promise.complete();
      }
      else{
        promise.fail(ar.cause());
      }
    });
  }
}
