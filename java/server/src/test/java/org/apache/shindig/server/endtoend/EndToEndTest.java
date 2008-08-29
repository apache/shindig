/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.server.endtoend;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;

/**
 * Base class for end-to-end tests.
 */
public class EndToEndTest {
  private static final String[] EXPECTED_RESOURCES = {
    "fetchPersonTest.xml",
    "fetchPeopleTest.xml",
    "errorTest.xml",
    "testframework.js"
  };

  static private EndToEndServer server = null;

  private WebClient webClient;
  private CollectingAlertHandler alertHandler;
  private SecurityToken token;
  
  @Test
  public void checkResources() throws Exception {
    for ( String resource : EXPECTED_RESOURCES ) {
      String url = EndToEndServer.SERVER_URL + "/" + resource;
      Page p = webClient.getPage(url);
      assertEquals("Failed to load test resource "+url,200,p.getWebResponse().getStatusCode());
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
    alertHandler = new CollectingAlertHandler();
    webClient.setAlertHandler(alertHandler);
    token = createToken("canonical", "john.doe");
  }

  @After
  public void tearDown() {
    server.clearDataServiceError();
  }

  /**
   * Verify that the Javascript completed running.  This ensures that
   * logic errors or exceptions don't get treated as success.
   */
  @After
  public void verifyTestsFinished() {
    // Verify the format of the alerts - test method names followed by "finished"
    String testMethod = null;
    for (String alert : alertHandler.getCollectedAlerts()) {
      if (testMethod == null) {
        assertFalse("Test method omitted", "FINISHED".equals(alert));
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
    String gadgetUrl = EndToEndServer.SERVER_URL + "/" + testName + ".xml";
    String url = EndToEndServer.GADGET_BASEURL + "?url=" + URLEncoder.encode(gadgetUrl, "UTF-8");
    BasicSecurityTokenDecoder decoder = new BasicSecurityTokenDecoder();
    url += "&st=" + URLEncoder.encode(decoder.encodeToken(token), "UTF-8");
    url += "&testMethod=" + URLEncoder.encode(testMethod, "UTF-8");
    return (HtmlPage) webClient.getPage(url);
  }

  /**
   * Executes all page test in a single XML file.
   * @param testName name of the test, which must match a gadget XML file
   *     name in test/resources/endtoend (minus .xml).
   * @throws IOException
   */
  private void executeAllPageTests(String testName) throws IOException {
    executePageTest(testName, "all");
  }

  private BasicSecurityToken createToken(String owner, String viewer)
      throws BlobCrypterException {
    return new BasicSecurityToken(owner, viewer, "test", "domain", "appUrl", "1");
  }
}
