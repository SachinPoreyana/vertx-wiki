package io.vertx.starter.database;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class WikiDatabaseServiceTest {

  private Vertx vertx;
  private WikiDatabaseService wikiDatabaseService;

  @Before
  public void setup(TestContext testContext) throws InterruptedException
  {
    vertx = Vertx.vertx();

    JsonObject conf = new JsonObject()
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

    vertx.deployVerticle(new WikiDatabaseVerticle(), new DeploymentOptions().setConfig(conf),
      testContext.asyncAssertSuccess(id -> wikiDatabaseService = WikiDatabaseService.createProxy(vertx, WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE)));
  }

  @After
  public void finish(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testDbCrub(TestContext testContext) {
    Async async = testContext.async();
    wikiDatabaseService.createPage("Test", "Some Content", testContext.asyncAssertSuccess(v1 -> {
      wikiDatabaseService.fetchPage("Test", testContext.asyncAssertSuccess(jsondata -> {
        testContext.assertTrue(jsondata.getBoolean("found"));
        testContext.assertTrue(jsondata.containsKey("id"));
        testContext.assertEquals("Some Content", jsondata.getString("rawContent"));

        wikiDatabaseService.savePage(jsondata.getInteger("id"), "Yo!", testContext.asyncAssertSuccess(v2 -> {
          wikiDatabaseService.fetchAllPages(testContext.asyncAssertSuccess(array1 -> {
            testContext.assertEquals(1, array1.size());
            wikiDatabaseService.fetchPage("Test", testContext.asyncAssertSuccess(json2 -> {
              testContext.assertEquals("Yo!", json2.getString("rawContent"));

              wikiDatabaseService.deletePage(jsondata.getInteger("id"), v3 -> {
                wikiDatabaseService.fetchAllPages(testContext.asyncAssertSuccess(array2 -> {
                  testContext.assertTrue(array2.isEmpty());
                  async.complete();
                }));
              });
            }));
          }));
        }));
      }));
    }));
    async.awaitSuccess(5000);
  }
}
