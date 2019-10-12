package io.vertx.starter.http;

import com.github.rjeschke.txtmark.Processor;
import com.sun.deploy.util.StringUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.starter.database.DatabaseConstants;
import io.vertx.starter.database.WikiDatabaseService;
import io.vertx.starter.geolocation.DistanceCalculator;
import org.hsqldb.lib.StringUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle implements DatabaseConstants{

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private String wikiDbQueue = "wikidb.queue";
  public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";


  public static final String CONFIG_WIKIDB_MONGO_HOST = "wikidb.mongo.host";
  public static final String CONFIG_WIKIDB_MONGO_PORT = "wikidb.mongo.port";

  private FreeMarkerTemplateEngine templateEngine;

  private static final String EMPTY_PAGE_MARKDOWN =  "# A new page\n" + "\n" +"Feel-free to write in Markdown!\n";
  private WikiDatabaseService dbService;
  MongoClient mongoClient;

  @Override
  public void start(Promise<Void> promise) throws Exception
  {
    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);

    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);

    HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(new JksOptions()
        .setPath("server-keystore.jks")
        .setPassword("secret")));

    JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getString(CONFIG_WIKIDB_JDBC_URL, DEFAULT_WIKIDB_JDBC_URL))
      .put("driver_class", config().getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, DEFAULT_WIKIDB_JDBC_DRIVER_CLASS))
      .put("max_pool_size", config().getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, DEFAULT_JDBC_MAX_POOL_SIZE)));
    JDBCAuth auth = JDBCAuth.create(vertx, dbClient);

    JsonObject mongoConfig = new JsonObject()
      .put("host", config().getString(CONFIG_WIKIDB_MONGO_HOST, DEFAULT_WIKIDB_MONGO_HOST))
      .put("port", config().getInteger(CONFIG_WIKIDB_MONGO_PORT, DEFAULT_WIKIDB_MONGO_PORT))
      .put("db_name","wiki");

    mongoClient = MongoClient.createShared(vertx,mongoConfig, "wiki" );




    Router router = Router.router(vertx);

    router.route().handler(CookieHandler.create());
    router.route().handler(BodyHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(UserSessionHandler.create(auth));
    AuthHandler authHandler = RedirectAuthHandler.create(auth, "/login");

    router.get("/login").handler(this::loginHandler);
    router.post("/login-auth").handler(FormLoginHandler.create(auth));

    router.get("/logout").handler(this::logoutHandler);

    router.route("/").handler(authHandler);
    router.route("/wiki/*").handler(authHandler);
    router.route("/action/*").handler(authHandler);

    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/wiki/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    Router apiRouter = Router.router(vertx);

    JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setType("jceks")
        .setPassword("secret")));
    apiRouter.route().handler(JWTAuthHandler.create(jwtAuth, "/api/token"));

    apiRouter.get("/token").handler(context -> {
      LOGGER.error("##########################");
      JsonObject creds = new JsonObject()
        .put("username", context.request().getHeader("login"))
        .put("password", context.request().getHeader("password"));
      auth.authenticate(creds, authResult -> {
        LOGGER.error("reached");
        if (authResult.succeeded()) {
          LOGGER.error("Successs");
          User user = authResult.result();
          user.isAuthorized("create", canCreate -> {
            user.isAuthorized("delete", canDelete -> {
              user.isAuthorized("update", canUpdate -> {
                LOGGER.error("Reached");
                try {
                  String token = jwtAuth.generateToken(
                    new JsonObject()
                      .put("username", context.request().getHeader("login"))
                      .put("canCreate", canCreate.succeeded() && canCreate.result())
                      .put("canDelete", canDelete.succeeded() && canDelete.result())
                      .put("canUpdate", canUpdate.succeeded() && canUpdate.result()),
                    new JWTOptions()
                      .setSubject("Wiki API")
                      .setIssuer("Vert.x"));

                  LOGGER.error(token);
                  context.response().putHeader("Content-Type", "text/plain").end(token);
                }catch (Exception e)
                {
                  LOGGER.error("print ex" + e);
                }

              });
            });
          });
        } else {
          context.fail(401);
        }
      });
    });

    apiRouter.get("/pages").handler(this::apiRoot);
    apiRouter.get("/pages/:id").handler(this::apiGetPage);
    apiRouter.post().handler(BodyHandler.create());
    apiRouter.post("/pages").handler(this::apiCreatePage);
    apiRouter.put().handler(BodyHandler.create());
    apiRouter.put("/pages/:id").handler(this::apiUpdatePage);
    apiRouter.delete("/pages/:id").handler(this::apiDeletePage);

    apiRouter.post("/wiki").handler(this::apiCreateWiki);
    apiRouter.get("/wiki/").handler(this::apiGetWiki);

    apiRouter.post("/wiki/geolocate").handler(this::apiGeolocate);
    apiRouter.put("/wiki/:id").handler(this::apiUpdateWiki);
    apiRouter.delete("/wiki/:id").handler(this::apiDeleteWiki);
    apiRouter.get("/wiki/nearfour/:id").handler(this::apinearFourWiki);
    router.mountSubRouter("/api", apiRouter);

    httpServer.requestHandler(router)
      .listen(portNumber, ar -> {
        if(ar.succeeded())
        {
          LOGGER.error("Http server successfully running on port " + portNumber);
          promise.complete();
        }
        else{
          LOGGER.error("Could not start HTTP server on port "+ portNumber + " for reason : " +ar.cause());
          promise.fail(ar.cause());
        }
      });
  }

  private void apiGetWiki(RoutingContext context)
  {
    try{
      String id = context.request().getParam("id");
      JsonObject query = new JsonObject().put("_id", id);

      mongoClient.find("wiki", query, ar -> {
        if(ar.succeeded())
        {
          JsonObject result = ar.result().stream().findFirst().get();
          LOGGER.error("success");
          context.response().setStatusCode(200);
          context.response().end(result.encodePrettily());
        }
        else {
          LOGGER.error("failure");
          context.response().setStatusCode(500);
          context.response().end(new JsonObject()
            .put("success", false)
            .put("error", ar.cause().getMessage()).encodePrettily());
        }
      });
    }
    catch (Exception e)
    {
      LOGGER.error("Hellooo + "+ e);
    }
  }

  private void apinearFourWiki(RoutingContext context)
  {
    try{
      String id = context.request().getParam("id");
      JsonObject query = new JsonObject().put("_id", id);

      mongoClient.find("wiki", query, ar -> {
        if(ar.succeeded())
        {
          JsonObject result = ar.result().stream().findFirst().get();
          Double latitude = result.getJsonObject("location").getDouble("latitude");
          LOGGER.error("LAT is " + latitude);
          Double longitude = result.getJsonObject("location").getDouble("longitude");
          LOGGER.error("Long is " + latitude);

          JsonObject body = new JsonObject().put("latitude", latitude).put("longitude", longitude);

          context.setBody(body.toBuffer());

          apiGeolocate(context);
        }
        else {
          LOGGER.error("failure");
          context.response().setStatusCode(500);
          context.response().end(new JsonObject()
            .put("success", false)
            .put("error", ar.cause().getMessage()).encodePrettily());
        }
      });
    }
    catch (Exception e)
    {
      LOGGER.error("Hellooo + "+ e);
    }

  }

  private void apiUpdateWiki(RoutingContext context)
  {
    String id = context.request().getParam("id");

    JsonObject query = new JsonObject().put("_id" , id);

    JsonObject data = new JsonObject().put("$set",context.getBodyAsJson());

    mongoClient.updateCollection("wiki",query,data, ar -> {

      if(ar.succeeded()) {
        LOGGER.error("Wiki with id updated for id " + id);
        context.response().setStatusCode(200);
        context.response().end(data.encodePrettily());
      }
      else{
        LOGGER.error("Wiki with id update failed for id " + id);

        context.response().setStatusCode(500);
        context.response().end(new JsonObject()
          .put("success", false)
          .put("error", ar.cause().getMessage()).encodePrettily());
      }

    });
  }

  private void apiDeleteWiki(RoutingContext context)
  {
    String id = context.request().getParam("id");

    JsonObject query = new JsonObject().put("_id" , id);

    JsonObject data = context.getBodyAsJson();

    mongoClient.removeDocument("wiki",query, ar -> {

      if(ar.succeeded()) {
        LOGGER.error("Wiki deleted for id " + id);
        context.response().setStatusCode(200);
        context.response().end(data.encodePrettily());
      }
      else{
        LOGGER.error("Wiki with id deleted failed for id " + id);
        context.response().setStatusCode(500);
        context.response().end(new JsonObject()
          .put("success", false)
          .put("error", ar.cause().getMessage()).encodePrettily());
      }

    });
  }


  private void apiGeolocate(RoutingContext context)
  {
    try{
      JsonObject data = context.getBodyAsJson();
      Double latitude = data.getDouble("latitude");
      LOGGER.error("LAT is " + latitude);
      Double longitude = data.getDouble("longitude");
      Double radiusFromEpicenter = data.getDouble("radiusFromEpicenterinMeter");
      Double radiusFromEpicenterInKm = radiusFromEpicenter == null  ? .5D : radiusFromEpicenter/1000;
      LOGGER.error("radius from epi in KM " +radiusFromEpicenterInKm);
      LOGGER.error("LAT is " + latitude);
      LOGGER.error("Lon is " + longitude);
      JsonObject query = new JsonObject();
      JsonObject first = new JsonObject();
      JsonObject second = new JsonObject();
      JsonObject third = new JsonObject();
      JsonObject fourth = new JsonObject();
      //TODO: dirty shit right here
      Double firstshortdist = 10000000000D;
      Double secondshortdist = 10000000000D;
      Double thirdShortdist = 10000000000D;
      Double fourthShortdist = 10000000000D;
      final AtomicReference<Double> firstshortdistRef = new AtomicReference<>(firstshortdist);
      final AtomicReference<Double> secondshortdistRef = new AtomicReference<>(secondshortdist);
      final AtomicReference<Double> thirdShortdistRef = new AtomicReference<>(thirdShortdist);
      final AtomicReference<Double> fourthShortdistRef = new AtomicReference<>(fourthShortdist);

      final AtomicReference<JsonObject> firstObjectreference = new AtomicReference<>(first);
      final AtomicReference<JsonObject> secondObjectreference = new AtomicReference<>(second);
      final AtomicReference<JsonObject> thirdObjectreference = new AtomicReference<>(third);
      final AtomicReference<JsonObject> fouthObjectreference = new AtomicReference<>(fourth);

      JsonArray fourNearestPlaces = new JsonArray();


      mongoClient.findBatch("wiki", query)
        .exceptionHandler(throwable -> LOGGER.error(throwable.getMessage()))
        .endHandler(v -> {
          context.response().setStatusCode(200);
          context.response().end(enrich(firstObjectreference.get(), secondObjectreference.get(), thirdObjectreference.get(), fouthObjectreference.get()).encodePrettily());})
        .handler(doc -> {
          Double docLat = doc.getJsonObject("location").getDouble("latitude");
          LOGGER.error("doc lat is  " + docLat);
          Double docLong = doc.getJsonObject("location").getDouble("longitude");
          LOGGER.error("doc long is " + docLong);


          Double distance = DistanceCalculator.geoDistance(latitude, longitude,docLat ,docLong );
          LOGGER.error("distance is :" + distance);
          if (distance > radiusFromEpicenterInKm)
          {
            return;
          }

          if(distance < firstshortdistRef.get() )
          {
            LOGGER.error("lesser than first :" + distance);

            JsonObject swapobject = thirdObjectreference.get();
            Double swapdist = thirdShortdistRef.get();

            thirdShortdistRef.set(secondshortdistRef.get());
            thirdObjectreference.set(secondObjectreference.get());

            secondObjectreference.set(firstObjectreference.get());
            secondshortdistRef.set(firstshortdistRef.get());

            firstshortdistRef.set(distance);
            firstObjectreference.set(doc);

            fourthShortdistRef.set(swapdist);
            fouthObjectreference.set(swapobject);

          }
          else if(distance< secondshortdistRef.get())
          {
            LOGGER.error("lesser than second :" + distance);
            fourthShortdistRef.set(thirdShortdistRef.get());
            fouthObjectreference.set(thirdObjectreference.get());

            thirdShortdistRef.set(secondshortdistRef.get());
            thirdObjectreference.set(secondObjectreference.get());

            secondObjectreference.set(doc);
            secondshortdistRef.set(distance);
          }
          else if(distance < thirdShortdistRef.get())
          {
            LOGGER.error("lesser than third :" + distance);
            fourthShortdistRef.set(thirdShortdistRef.get());
            fouthObjectreference.set(thirdObjectreference.get());

            thirdShortdistRef.set(distance);
            thirdObjectreference.set(doc);
          }
          else if(distance < fourthShortdistRef.get())
          {
            LOGGER.error("lesser than fourth :" + distance);
            fourthShortdistRef.set(thirdShortdistRef.get());
            fouthObjectreference.set(thirdObjectreference.get());

            thirdShortdistRef.set(distance);
            thirdObjectreference.set(doc);
          }
        });

    }
    catch (Exception e)
    {
      LOGGER.error("Hellooo + "+ e);
    }

  }

  private JsonArray enrich(JsonObject one,JsonObject two, JsonObject three, JsonObject four)
  {
    //TODO cleanup
    JsonArray fourNearestPlaces = new JsonArray();
    fourNearestPlaces.add(one);
    fourNearestPlaces.add(two);
    fourNearestPlaces.add(three);
    fourNearestPlaces.add(four);

    return  fourNearestPlaces;

  }

  private void apiCreateWiki(RoutingContext context)
  {
    try{
      JsonObject data = context.getBodyAsJson();

      if(!validateCreateWiki(context))
      {
        return;
      }

      mongoClient.save("wiki", data, ar -> {
        if(ar.succeeded())
        {
          LOGGER.error("success");
          context.response().setStatusCode(201);
          context.response().end(data.encodePrettily());
        }
        else {
          LOGGER.error("failure");
          context.response().setStatusCode(500);
          context.response().end(new JsonObject()
            .put("success", false)
            .put("error", ar.cause().getMessage()).encodePrettily());
        }
      });
    }
    catch (Exception e)
    {
      LOGGER.error("Hellooo + "+ e);
    }

  }

  //TODO:Next major
  private void validateCreateWiki(JsonObject data) {
    return;
  }


  private void apiRoot(RoutingContext context) {
    LOGGER.error("DB service" + dbService);
    try {
      dbService.fetchDataOfAllPages(reply -> {
        JsonObject response = new JsonObject();
        if (reply.succeeded()) {
          List<JsonObject> pages = reply.result()
            .stream()
            .map(obj -> new JsonObject()
              .put("id", obj.getInteger("ID"))
              .put("name", obj.getString("NAME")))
            .collect(Collectors.toList());
          response
            .put("success", true)
            .put("pages", pages);
          context.response().setStatusCode(200);
          context.response().putHeader("Content-Type", "application/json");
          context.response().end(response.encode());
        } else {
          LOGGER.error("Some error " + reply.cause().getMessage());
          response
            .put("success", false)
            .put("error", reply.cause().getMessage());
          context.response().setStatusCode(500);
          context.response().putHeader("Content-Type", "application/json");
          context.response().end(response.encode());
        }
      });
    }
    catch (Exception e)
    {
      LOGGER.error("exception occired :" + e);
    }
  }

  private void apiGetPage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    LOGGER.error("DB service" + dbService);
    dbService.fetchPageById(id, reply -> {
      JsonObject response = new JsonObject();
      if (reply.succeeded()) {
        JsonObject dbObject = reply.result();
        if (dbObject.getBoolean("found")) {
          JsonObject payload = new JsonObject()
            .put("name", dbObject.getString("name"))
            .put("id", dbObject.getInteger("id"))
            .put("markdown", dbObject.getString("content"))
            .put("html", Processor.process(dbObject.getString("content")));
          response
            .put("success", true)
            .put("page", payload);
          context.response().setStatusCode(200);
        } else {
          context.response().setStatusCode(404);
          response
            .put("success", false)
            .put("error", "There is no page with ID " + id);
        }
      } else {
        response
          .put("success", false)
          .put("error", reply.cause().getMessage());
        context.response().setStatusCode(500);
      }
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(response.encode());
    });
  }

  private void apiCreatePage(RoutingContext context) {
    LOGGER.error("Reached here");
    JsonObject page = context.getBodyAsJson();
    if (!validateJsonPageDocument(context, page, "name", "markdown")) {
      return;
    }
    dbService.createPage(page.getString("name"), page.getString("markdown"), reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(201);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(new JsonObject().put("success", true).encode());
      } else {
        context.response().setStatusCode(500);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(new JsonObject()
          .put("success", false)
          .put("error", reply.cause().getMessage()).encode());
      }
    });
  }

  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.error("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().
        remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encode());
      return false;
    }
    return true;
  }

  private boolean validateCreateWiki(RoutingContext context)
  {

    JsonObject requestBody = context.getBodyAsJson();
    if(!requestBody.containsKey("name") || StringUtil.isEmpty(requestBody.getString("name")) )
    {
       return addValidationError(context, "Name is null or empty");
    }
    if(!requestBody.containsKey("content") || requestBody.getJsonObject("content").isEmpty() )
    {
       return addValidationError(context, "Content is not present");

    }
    if(!requestBody.getJsonObject("content").containsKey("imageUrls") || requestBody.getJsonObject("content").getJsonArray("imageUrls").isEmpty() )
    {
      return addValidationError(context, "Image urls are empty");
    }
    if(!requestBody.getJsonObject("content").containsKey("descriptions") || requestBody.getJsonObject("descriptions").getJsonArray("imageUrls").isEmpty() )
    {
      return addValidationError(context, "descriptions are empty");
    }
    if(!requestBody.containsKey("location") || requestBody.getJsonObject("location").isEmpty() )
    {
      return addValidationError(context, "Location not present");
    }
    if(!requestBody.getJsonObject("location").containsKey("latitude") || requestBody.getJsonObject("location").getDouble("latitude") == null )
    {
      return addValidationError(context, "latitude is null");
    }
    if(!requestBody.getJsonObject("location").containsKey("longitude") || requestBody.getJsonObject("location").getDouble("longitude") == null )
    {
      return addValidationError(context, "longitude is null");
    }

    return true;
  }

  private boolean addValidationError(RoutingContext context, String errormessage)
  {
    context.response().setStatusCode(400);
    context.response().putHeader("Content-Type", "application/json");
    context.response().end(new JsonObject()
      .put("success", false)
      .put("error", "Bad request payload:" +  errormessage).encode());
    return false;
  }
  private void apiUpdatePage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    JsonObject page = context.getBodyAsJson();
    if (!validateJsonPageDocument(context, page, "markdown")) {
      return;
    }
    dbService.savePage(id, page.getString("markdown"), reply -> {
      handleSimpleDbReply(context, reply);
    });
  }

  private void handleSimpleDbReply(RoutingContext context, AsyncResult<Void> reply) {
    if (reply.succeeded()) {
      context.response().setStatusCode(200);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject().put("success", true).encode());
    } else {
      context.response().setStatusCode(500);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", reply.cause().getMessage()).encode());
    }
  }

  private void apiDeletePage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    dbService.deletePage(id, reply -> {
      handleSimpleDbReply(context, reply);
    });
  }

  private void indexHandler(RoutingContext context) {
    context.user().isAuthorized("create", result ->{
      boolean canCreatePage = result.succeeded() && result.result();

    dbService.fetchAllPages(reply -> {
      if (reply.succeeded()) {
        context.put("title", "VIRTUAL TRAVEL GUIDE");
        context.put("pages", reply.result().getList());
        context.put("canCreatePage", canCreatePage);
        context.put("username", context.user().principal().getString("username"));
        templateEngine.render(context.data(), "templates/index.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }
        });
      } else {
        context.fail(reply.cause());
      }
    });
    });
  }

  private void pageRenderingHandler(RoutingContext context) {
    String requestedPage = context.request().getParam("page");
    dbService.fetchPage(requestedPage, reply -> {
      if (reply.succeeded()) {
        JsonObject payLoad = reply.result();
        boolean found = payLoad.getBoolean("found");
        String rawContent = payLoad.getString("rawContent", EMPTY_PAGE_MARKDOWN);
        context.put("title", requestedPage);
        context.put("id", payLoad.getInteger("id", -1));
        context.put("newPage", found ? "no" : "yes");
        context.put("rawContent", rawContent);
        context.put("content", Processor.process(rawContent));
        context.put("timestamp", new Date().toString());
        templateEngine.render(context.data(), "templates/page.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }
        });
      } else {
        context.fail(reply.cause());
      }
    });
  }
  private void pageUpdateHandler(RoutingContext context) {
    String title = context.request().getParam("title");
    Handler<AsyncResult<Void>> handler = reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/wiki/" + title);
        context.response().end();
      } else {
        context.fail(reply.cause());
      }
    };
    String markdown = context.request().getParam("markdown");
    if ("yes".equals(context.request().getParam("newPage"))) {
      dbService.createPage(title, markdown, handler);
    } else {
      dbService.savePage(Integer.valueOf(context.request().getParam("id")), markdown, handler);
    }
  }

  private  RoutingContext logoutHandler(RoutingContext context)
  {
      context.clearUser();
      context.response()
        .setStatusCode(302)
        .putHeader("Location", "/")
        .end();
      return context;
  }


  private void pageCreateHandler(RoutingContext context) {
    context.user().isAuthorized("create", res -> {
      if (res.succeeded() && res.result()) {
        String pageName = context.request().getParam("name");
        String location = "/wiki/" + pageName;
        if (pageName == null || pageName.isEmpty()) {
          location = "/";
        }
        context.response().setStatusCode(303);
        context.response().putHeader("Location", location);
        context.response().end();
      } else {
        context.response().setStatusCode(403).end();
      }
    });
  }


  private void pageDeletionHandler(RoutingContext context) {
    context.user().isAuthorized("delete", res -> {
      if(res.succeeded() && res.result())
      {
        dbService.deletePage(Integer.valueOf(context.request().getParam("id")), reply -> {
          if (reply.succeeded()) {
            context.response().setStatusCode(303);
            context.response().putHeader("Location", "/");
            context.response().end();
          } else {
            context.fail(reply.cause());
          }
        });
      }
      else{
        context.response().setStatusCode(403).end();
      }
    });
  }

  private void loginHandler(RoutingContext context) {
    context.put("title", "Login");
    templateEngine.render(context.data(), "templates/login.ftl", ar -> {
      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html");
        context.response().end(ar.result());
      } else {
        context.fail(ar.cause());
      }
    });
  }
}
