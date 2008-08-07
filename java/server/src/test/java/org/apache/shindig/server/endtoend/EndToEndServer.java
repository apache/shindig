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
import com.google.common.collect.Maps;

import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.HttpGuiceModule;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.core.oauth.AuthenticationServletFilter;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Suite for running the end-to-end tests. The suite is responsible for starting up and shutting
 * down the server.
 */
public class EndToEndServer {
  private static final int JETTY_PORT = 9003;
  private static final String GADGET_BASE = "/gadgets/ifr";
  private static final String JSON_BASE = "/social/rest/*";
  private static final String CONCAT_BASE = "/gadgets/concat";
  public static final String SERVER_URL = "http://localhost:" + JETTY_PORT;
  public static final String GADGET_BASEURL = SERVER_URL + GADGET_BASE;

  private final Server server;

  /** Fake error code for data service servlet request */
  private int errorCode;

  /** Fake error message for data service servlet request */
  private String errorMessage;

  public EndToEndServer() throws Exception {
    server = createServer(JETTY_PORT);
  }

  public void start() throws Exception {
    server.start();
  }

  public void stop() throws Exception {
    server.stop();
  }

  public void clearDataServiceError() {
    errorCode = 0;
  }

  public void setDataServiceError(int errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  /**
   * Starts the server for end-to-end tests.
   */
  private Server createServer(int port) throws Exception {
    Server newServer = new Server(port);

    // Attach the test resources in /endtoend as static content for the test
    ResourceHandler resources = new ResourceHandler();
    URL resource = EndToEndTest.class.getResource("/endtoend");
    resources.setBaseResource(Resource.newResource(resource));
    newServer.addHandler(resources);

    Context context = new Context(newServer, "/", Context.SESSIONS);
    context.addEventListener(new GuiceServletContextListener());

    Map<String, String> initParams = Maps.newHashMap();
    String modules = Join
        .join(":", EndToEndModule.class.getName(), HttpGuiceModule.class.getName());

    initParams.put(GuiceServletContextListener.MODULES_ATTRIBUTE, modules);
    context.setInitParams(initParams);

    // Attach the gadget rendering servlet
    ServletHolder gadgetServletHolder = new ServletHolder(new GadgetRenderingServlet());
    context.addServlet(gadgetServletHolder, GADGET_BASE);

    // Attach DataServiceServlet, wrapped in a proxy to fake errors
    ServletHolder jsonServletHolder = new ServletHolder(new ForceErrorServlet(
        new DataServiceServlet()));
    context.addServlet(jsonServletHolder, JSON_BASE);
    context.addFilter(AuthenticationServletFilter.class, JSON_BASE, 0);

    // Attach the ConcatProxyServlet - needed for
    ServletHolder concatHolder = new ServletHolder(new ConcatProxyServlet());
    context.addServlet(concatHolder, CONCAT_BASE);

    return newServer;
  }

  private class ForceErrorServlet implements Servlet {
    private final Servlet proxiedServlet;

    public ForceErrorServlet(Servlet proxiedServlet) {
      this.proxiedServlet = proxiedServlet;
    }

    public void init(ServletConfig servletConfig) throws ServletException {
      proxiedServlet.init(servletConfig);
    }

    public ServletConfig getServletConfig() {
      return proxiedServlet.getServletConfig();
    }

    public void service(ServletRequest servletRequest, ServletResponse servletResponse)
        throws ServletException, IOException {
      if (errorCode > 0) {
        ((HttpServletResponse) servletResponse).sendError(errorCode, errorMessage);
      } else {
        proxiedServlet.service(servletRequest, servletResponse);
      }
    }

    public String getServletInfo() {
      return proxiedServlet.getServletInfo();
    }

    public void destroy() {
      proxiedServlet.destroy();
    }
  }
}
