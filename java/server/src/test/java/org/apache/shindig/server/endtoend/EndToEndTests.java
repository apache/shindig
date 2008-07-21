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

import com.google.common.base.Join;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.BasicSecurityTokenDecoder;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.HttpGuiceModule;
import org.apache.shindig.social.dataservice.DataServiceServlet;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Base class for end-to-end tests.
 */
public class EndToEndTests {

  private static final int JETTY_PORT = 9003;
  private static final String GADGET_BASE = "/gadgets/ifr";
  private static final String JSON_BASE = "/social/rest/*";
  private static final String CONCAT_BASE = "/gadgets/concat";
  private static final String SERVER_URL = "http://localhost:" + JETTY_PORT;
  private static final String GADGET_BASEURL = SERVER_URL + GADGET_BASE;

  static private Server server = null;

  protected WebClient webClient;
  protected CollectingAlertHandler alertHandler;

  @Before
  public void setUp() {
    webClient = new WebClient();
    // NicelyResynchronizingAjaxController changes XHR calls from asynchronous
    // to synchronous, saving the test from needing to wait or sleep for XHR
    // completion.
    webClient.setAjaxController(new NicelyResynchronizingAjaxController());
    alertHandler = new CollectingAlertHandler();
    webClient.setAlertHandler(alertHandler);
  }

  @After
  public void verifyTestFinished() {
    // Verify that the Javascript completed running.  This ensures that
    // logic errors or exceptions don't get treated as success
    assertEquals(ImmutableList.of("FINISHED"), alertHandler.getCollectedAlerts());
  }

  @BeforeClass
  public static void setUpOnce() throws Exception {
    server = createServer(JETTY_PORT);
    server.start();
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    server.stop();
  }

  /**
   * Executes a page test by loading the HTML page.
   * @param testName name of the test, which must match a gadget XML file
   *     name in test/resources/endtoend (minus .xml).
   * @param testMethod name of the javascript method to execute
   * @param token the security token
   * @return the parsed HTML page
   */
  protected HtmlPage executePageTest(String testName, String testMethod, BasicSecurityToken token)
      throws IOException {
    String gadgetUrl = SERVER_URL + "/" + testName + ".xml";
    String url = GADGET_BASEURL + "?url=" + URLEncoder.encode(gadgetUrl, "UTF-8");
    BasicSecurityTokenDecoder decoder = new BasicSecurityTokenDecoder();
    url += "&st=" + URLEncoder.encode(decoder.encodeToken(token), "UTF-8");
    url += "&testMethod=" + URLEncoder.encode(testMethod, "UTF-8");
    return (HtmlPage) webClient.getPage(url);
  }

  /**
   * Starts the server for end-to-end tests.
   */
  private static Server createServer(int port) throws Exception {
    Server newServer = new Server(port);

    // Attach the test resources in /endtoend as static content for the test
    ResourceHandler resources = new ResourceHandler();
    URL resource = EndToEndTests.class.getResource("/endtoend");
    resources.setBaseResource(Resource.newResource(resource));
    newServer.addHandler(resources);

    Context context = new Context(newServer, "/", Context.SESSIONS);
    context.addEventListener(new GuiceServletContextListener());

    Map<String, String> initParams = Maps.newHashMap();
    String modules = Join.join(":",
        EndToEndModule.class.getName(), HttpGuiceModule.class.getName());

    initParams.put(GuiceServletContextListener.MODULES_ATTRIBUTE, modules);
    context.setInitParams(initParams);

    // Attach the gadget rendering servlet
    ServletHolder gadgetServletHolder = new ServletHolder(new GadgetRenderingServlet());
    context.addServlet(gadgetServletHolder, GADGET_BASE);

    // Attach DataServiceServlet
    ServletHolder jsonServletHolder = new ServletHolder(new DataServiceServlet());
    context.addServlet(jsonServletHolder, JSON_BASE);

    // Attach the ConcatProxyServlet - needed for
    ServletHolder concatHolder = new ServletHolder(new ConcatProxyServlet());
    context.addServlet(concatHolder, CONCAT_BASE);

    return newServer;
  }

  protected BasicSecurityToken createToken(String owner, String viewer)
      throws BlobCrypterException {
    return new BasicSecurityToken(owner, viewer, "test", "domain", "appUrl", "1");
  }
}
