package org.apache.shindig.gadgets.http;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.shindig.gadgets.http.EchoServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Holds test cases that all HttpFetcher implementations should pass.  This
 * starts up an HTTP server and runs tests against it.
 * 
 * TODO simulate fake POST requests, headers, options, etc.
 */
public class AbstractHttpFetcherTest {
  private static final int ECHO_PORT = 9003;
  private static final String BASE_URL = "http://localhost:9003/";
  private static EchoServer server;
  protected HttpFetcher fetcher = null;
  
  private String encode(String content) throws Exception {
    return URLEncoder.encode(content, "UTF-8");
  }
  
  @BeforeClass
  public static void setUpOnce() throws Exception {
    server = new EchoServer();
    server.start(ECHO_PORT);
  }
  
  @AfterClass
  public static void tearDownOnce() throws Exception {
    if (server != null) {
      server.stop();
    }
  }
  
  @Test public void testHttpFetch() throws Exception {
    String content = "Hello, world!";
    HttpRequest request = new HttpRequest(new URI(
        BASE_URL + "?body=" + encode(content)));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(200, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }
  
  @Test public void testHttp404() throws Exception {
    String content = "Hello, world!";
    HttpRequest request = new HttpRequest(new URI(
        BASE_URL + "?body=" + encode(content) + "&status=404"));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(404, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());    
  }
  
  @Test public void testHttp403() throws Exception {
    String content = "Hello, world!";
    HttpRequest request = new HttpRequest(new URI(
        BASE_URL + "?body=" + encode(content) + "&status=403" +
        "&header=" + encode("WWW-Authenticate=some auth data")));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));
  }
  
  @Test public void testHttp403NoBody() throws Exception {
    String content = "";
    HttpRequest request = new HttpRequest(new URI(
        BASE_URL + "?body=" + encode(content) + "&status=403" +
        "&header=" + encode("WWW-Authenticate=some auth data")));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));    
  }
  
  @Test public void testHttp401NoBody() throws Exception {
    String content = "";
    HttpRequest request = new HttpRequest(new URI(
        BASE_URL + "?body=" + encode(content) + "&status=401" +
        "&header=" + encode("WWW-Authenticate=some auth data")));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(401, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));    
  }
}
