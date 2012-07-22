/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.server.endtoend;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.BasicSecurityTokenCodec;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.crypto.BlobCrypterException;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Base class for end-to-end tests.
 */
public class EndToEndTest {
  private static final String[] EXPECTED_RESOURCES = {
    "fetchPersonTest.xml",
    "fetchPeopleTest.xml",
    "errorTest.xml",
    "jsonTest.xml",
    "viewLevelElementsTest.xml",
    "cajaTest.xml",
    "failCajaTest.xml",
    "failCajaUrlTest.xml",
    "osapi/personTest.xml",
    "osapi/peopleTest.xml",
    "osapi/activitiesTest.xml",
    "osapi/appdataTest.xml",
    "osapi/batchTest.xml",
    "testframework.js"
  };

  static private EndToEndServer server = null;

  private WebClient webClient;
  private CollectingAlertHandler alertHandler;
  private SecurityToken token;
  private String language;

  @Test
  public void checkResources() throws Exception {
    for ( String resource : EXPECTED_RESOURCES ) {
      String url = EndToEndServer.SERVER_URL + '/' + resource;
      Page p = webClient.getPage(url);
      assertEquals("Failed to load test resource " + url, 200, p.getWebResponse().getStatusCode());
    }
  }

  @Test
  public void fetchPerson() throws Exception {
    executeAllPageTests("fetchPersonTest");
  }

  @Test
  public void fetchPeople() throws Exception {
    executeAllPageTests("fetchPeopleTest");
  }

  @Test
  public void messageBundles() throws Exception {
    executeAllPageTests("messageBundle");
  }

  @Test
  public void jsonParse() throws Exception {
    executeAllPageTests("jsonTest");
  }

  @Test
  public void viewLevelElements() throws Exception {
    executeAllPageTests("viewLevelElementsTest");
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void cajaJsonParse() throws Exception {
    executeAllPageTests("jsonTest", true /* caja */);
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void cajaFetchPerson() throws Exception {
    executeAllPageTests("fetchPersonTest", true /* caja */);
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void cajaFetchPeople() throws Exception {
    executeAllPageTests("fetchPeopleTest", true /* caja */);
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void cajaTestMakeRequest() throws Exception {
      executeAllPageTests("makeRequestTest", true /* caja */);
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void caja() throws Exception {
    executeAllPageTests("cajaTest.xml");
  }

  @Test
  public void testMakeRequest() throws Exception {
    executeAllPageTests("makeRequestTest");
  }

  @Test
  public void messageBundlesRtl() throws Exception {
    // Repeat the messageBundle tests, but with the language set to "ar"
    language = "ar";

    executeAllPageTests("messageBundle");
  }

  @Test
  public void notFoundError() throws Exception {
    server.setDataServiceError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
    executePageTest("errorTest", "notFoundError");
  }

  @Test
  public void badRequest() throws Exception {
    server.setDataServiceError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
    executePageTest("errorTest", "badRequestError");
  }

  @Test
  public void internalError() throws Exception {
    server.setDataServiceError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
    executePageTest("errorTest", "internalError");
  }

  @Test
  public void testTemplates() throws Exception {
    executeAllPageTests("opensocial-templates/ost_test");
  }

  @Test
  public void testFailCaja() throws Exception {
    HtmlPage page = executePageTest("failCajaTest", null);
    NodeList bodyList = page.getElementsByTagName("body");

    // Result should contain just one body
    assertEquals(1, bodyList.getLength());
    DomNode body = (DomNode) bodyList.item(0);

    // Failed output contains only an error block
    assertEquals(1, body.getChildNodes().getLength());
    assertEquals("ul", body.getFirstChild().getNodeName());
  }

  @Test
  public void testCajaFailUrlGadgets() throws Exception {
    try {
      executePageTest("failCajaUrlTest", null, /* caja */ true);
    } catch (FailingHttpStatusCodeException e) {
      assertEquals(HttpServletResponse.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testPipelining() throws Exception {
    HtmlPage page = executePageTest("pipeliningTest", null);
    JSONArray array = new JSONArray(page.asText());
    assertEquals(3, array.length());
    Map<String, JSONObject> jsonObjects = Maps.newHashMap();
    for (int i = 0; i < array.length(); i++) {
      JSONObject jsonObj = array.getJSONObject(i);
      assertTrue(jsonObj.has("id"));

      jsonObjects.put(jsonObj.getString("id"), jsonObj);
    }

    JSONObject me = jsonObjects.get("me").getJSONObject("result");
    assertEquals("Digg", me.getJSONObject("name").getString("familyName"));

    JSONObject json = jsonObjects.get("json").getJSONObject("result");
    JSONObject expected = new JSONObject("{content: {key: 'value'}, status: 200}");
    JsonAssert.assertJsonObjectEquals(expected, json);

    JsonAssert.assertObjectEquals("{id: 'var', result: 'value'}", jsonObjects.get("var"));
  }

  // TODO PML - convert this to use junit 4 Theories to simplify this.

  @Test
  public void testOsapiPeople() throws Exception {
    executeAllPageTests("osapi/peopleTest");
  }

  @Test
  public void testOsapiPerson() throws Exception {
    executeAllPageTests("osapi/personTest");
  }

  @Test
  public void testOsapiActivities() throws Exception {
    executeAllPageTests("osapi/activitiesTest");
  }

  @Test
  public void testOsapiAppdataFetchId() throws Exception {
    executePageTest("osapi/appdataTest", "fetchId");
  }

  @Test
  public void testOsapiAppdataAppDataWrite() throws Exception {
    executePageTest("osapi/appdataTest", "appdataWrite");
  }

  @Test
  public void testOsapiBatch() throws Exception {
    executeAllPageTests("osapi/batchTest");
  }


  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void testCajaOsapiAppdata() throws Exception {
    executeAllPageTests("osapi/appdataTest", true /* caja */);
  }

  @Test
  @Ignore("Issues with passing the neko dom to caja") // FIXME
  public void testCajaOsapiBatch() throws Exception {
    executeAllPageTests("osapi/batchTest", true /* caja */);
  }

  @Test
  public void testTemplateRewrite() throws Exception {
    HtmlPage page = executePageTest("templateRewriter", null);

    // Verify that iteration attributes were processed
    HtmlElement attrs = page.getElementById("attrs");
    List<HtmlElement> attrsList = attrs.getElementsByTagName("li");
    assertEquals(3, attrsList.size());

    Element element = page.getElementById("id0");
    assertNotNull(element);
    assertEquals("Jane", element.getTextContent().trim());

    element = page.getElementById("id2");
    assertNotNull(element);
    assertEquals("Maija", element.getTextContent().trim());

    // Verify that the repeatTag was processed
    HtmlElement repeat = page.getElementById("repeatTag");
    List<HtmlElement> repeatList = repeat.getElementsByTagName("li");
    assertEquals(1, repeatList.size());
    assertEquals("George", repeatList.get(0).getTextContent().trim());

    // Verify that the ifTag was processed
    HtmlElement ifTag = page.getElementById("ifTag");
    List<HtmlElement> ifList = ifTag.getElementsByTagName("li");
    assertEquals(3, ifList.size());
    assertEquals(1, page.getElementsByTagName("b").getLength());
    assertEquals(1, ifList.get(2).getElementsByTagName("b").size());

    Element jsonPipeline = page.getElementById("json");
    assertEquals("value", jsonPipeline.getTextContent().trim());

    Element textPipeline = page.getElementById("text");
    assertEquals("{\"key\": \"value\"}", textPipeline.getTextContent().trim());

    // Test of oncreate
    Element oncreateSpan = page.getElementById("mutate");
    assertEquals("mutated", oncreateSpan.getTextContent().trim());

    assertEquals("45", page.getElementById("sum").getTextContent().trim());
    assertEquals("25", page.getElementById("max").getTextContent().trim());
  }

  @Test
  public void testTemplateLibrary() throws Exception {
    HtmlPage page = executeAllPageTests("templateLibrary");
    String pageXml = page.asXml();
    assertTrue(pageXml.replaceAll("[\n\r ]", "").contains("p{color:red}"));

    Node paragraph = page.getElementsByTagName("p").item(0);
    assertEquals("Hello world", paragraph.getTextContent().trim());
  }


  @Test
  public void testJavaScriptCompile() throws Exception {
    // AllJsFilter will redirect to a url with all features being requested
    webClient.setRedirectEnabled(true);

    String containerJsUrl = EndToEndServer.SERVER_URL + "/gadgets/js/all-features-please.js?container=default&c=1";
    String gadgetJsUrl = EndToEndServer.SERVER_URL + "/gadgets/js/all-features-please.js?container=default&c=0";

    Page containerJsPage = webClient.getPage(containerJsUrl);
    assertEquals(containerJsPage.getWebResponse().getStatusCode(), 200);

    Page gadgetJsPage = webClient.getPage(gadgetJsUrl);
    assertEquals(gadgetJsPage.getWebResponse().getStatusCode(), 200);
  }

  @BeforeClass
  public static void setUpOnce() throws Exception {
    server = new EndToEndServer();
    server.start();
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    server.stop();
  }

  @Before
  public void setUp() throws Exception {
    webClient = new WebClient();
    // NicelyResynchronizingAjaxController changes XHR calls from asynchronous
    // to synchronous, saving the test from needing to wait or sleep for XHR
    // completion.
    webClient.setAjaxController(new NicelyResynchronizingAjaxController());
    webClient.waitForBackgroundJavaScript(120000);  // Closure can take a long time...
    webClient.setHTMLParserListener(HTMLParserListener.LOG_REPORTER);
    webClient.setTimeout(120000);  // Closure can take a long time...

    alertHandler = new CollectingAlertHandler();
    webClient.setAlertHandler(alertHandler);
    token = createToken("canonical", "john.doe");
    language = null;
    server.clearDataServiceError();
  }

//  @After
//  public void tearDown() {
//    server.clearDataServiceError();
//  }

  /**
   * Verify that the Javascript completed running.  This ensures that
   * logic errors or exceptions don't get treated as success.
   */
  @After
  public void verifyTestsFinished() {
    // Verify the format of the alerts - test method names followed by "finished"
    String testMethod = null;

    //System.out.println("=== All results " + alertHandler.getCollectedAlerts());
    for (String alert : alertHandler.getCollectedAlerts()) {
      if (testMethod == null) {
        assertFalse("Test method omitted - '" + alert + '"', "FINISHED".equals(alert));
        testMethod = alert;
      } else {
        assertEquals("test method " + testMethod + " did not finish", "FINISHED", alert);
        testMethod = null;
      }
    }

    assertNull("test method " + testMethod + " did not finish", testMethod);
  }

  /**
   * Executes a page test by loading the HTML page.
   * @param testName name of the test, which must match a gadget XML file
   *     name in test/resources/endtoend (minus .xml).
   * @param testMethod name of the javascript method to execute
   * @return the parsed HTML page
   */
  private HtmlPage executePageTest(String testName, String testMethod)
      throws IOException {
    return executePageTest(testName, testMethod, false /* caja */);
  }

  private HtmlPage executePageTest(String testName, String testMethod, boolean caja)
      throws IOException {
    if (!testName.endsWith(".xml")) {
      testName = testName + ".xml";
    }

    String gadgetUrl = EndToEndServer.SERVER_URL + '/' + testName;
    String url = EndToEndServer.GADGET_BASEURL + "?url=" + URLEncoder.encode(gadgetUrl, "UTF-8");
    BasicSecurityTokenCodec codec = new BasicSecurityTokenCodec();
    url += "&st=" + URLEncoder.encode(codec.encodeToken(token), "UTF-8");
    if (testMethod != null) {
      url += "&testMethod=" + URLEncoder.encode(testMethod, "UTF-8");
    }
    if (caja) {
      url += "&caja=1&libs=caja";
    }

    url += "&nocache=1";
    if (language != null) {
      url += "&lang=" + language;
    }

    Page page = webClient.getPage(url);
    // wait for window.setTimeout, window.setInterval or asynchronous XMLHttpRequest
    webClient.waitForBackgroundJavaScript(10000);
    if (!(page instanceof HtmlPage)) {
      fail("Got wrong page type. Was: " + page.getWebResponse().getContentType());
    }
    return (HtmlPage) page;
  }

  /**
   * Executes all page test in a single XML file.
   * @param testName name of the test, which must match a gadget XML file
   *     name in test/resources/endtoend (minus .xml).
   * @throws IOException
   */
  private HtmlPage executeAllPageTests(String testName) throws IOException {
      return executePageTest(testName, "all", false);
  }

  /**
   * Executes all page test in a single XML file injecting a flag to cajole the test first.
   * @param testName name of the test, which must match a gadget XML file
   *     name in test/resources/endtoend (minus .xml).
   * @throws IOException
   */
    private HtmlPage executeAllPageTests(String testName, boolean caja) throws IOException {
        return executePageTest(testName, "all", caja);
  }

  private BasicSecurityToken createToken(String owner, String viewer)
      throws BlobCrypterException {
    return new BasicSecurityToken(owner, viewer, "test", "domain", "appUrl", "1", "default", null, null);
  }
}
