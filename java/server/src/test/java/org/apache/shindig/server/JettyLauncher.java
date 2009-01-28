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
package org.apache.shindig.server;

import com.google.common.base.Join;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.AuthenticationServletFilter;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.servlet.AuthenticationModule;
import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.ProxyServlet;
import org.apache.shindig.server.endtoend.EndToEndModule;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.opensocial.service.JsonRpcServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.util.Map;

/**
 * Simple programmatic initialization of Shindig using Jetty and common paths.
 */
public class JettyLauncher {

  private static final String GADGET_BASE = "/gadgets/ifr";
  private static final String PROXY_BASE = "/gadgets/proxy";
  private static final String REST_BASE = "/social/rest/*";
  private static final String JSON_RPC_BASE = "/social/rpc/*";
  private static final String CONCAT_BASE = "/gadgets/concat";

  private Server server;

  private JettyLauncher(int port) throws IOException {

    server = new Server(port);

    Context context = new Context(server, "/", Context.SESSIONS);
    context.addEventListener(new GuiceServletContextListener());

    Map<String, String> initParams = Maps.newHashMap();
    String modules = Join
        .join(":", EndToEndModule.class.getName(), DefaultGuiceModule.class.getName(),
            PropertiesModule.class.getName(), OAuthModule.class.getName(),
            AuthenticationModule.class.getName());

    initParams.put(GuiceServletContextListener.MODULES_ATTRIBUTE, modules);
    context.setInitParams(initParams);

    // Attach the ConcatProxyServlet - needed for rewriting
    ServletHolder concatHolder = new ServletHolder(new ConcatProxyServlet());
    context.addServlet(concatHolder, CONCAT_BASE);

    // Attach the Proxy
    ServletHolder proxyHolder = new ServletHolder(new ProxyServlet());
    context.addServlet(proxyHolder, PROXY_BASE);

    // Attach the gadget rendering servlet
    ServletHolder gadgetServletHolder = new ServletHolder(new GadgetRenderingServlet());
    context.addServlet(gadgetServletHolder, GADGET_BASE);

    // Attach DataServiceServlet
    ServletHolder restServletHolder = new ServletHolder(new DataServiceServlet());
    context.addServlet(restServletHolder, REST_BASE);
    context.addFilter(AuthenticationServletFilter.class, REST_BASE, 0);

    // Attach JsonRpcServlet
    ServletHolder rpcServletHolder = new ServletHolder(new JsonRpcServlet());
    context.addServlet(rpcServletHolder, JSON_RPC_BASE);
    context.addFilter(AuthenticationServletFilter.class, JSON_RPC_BASE, 0);
  }

  public void start() throws Exception {
    server.start();
    server.join();
  }

  public static void main(String[] argv) throws Exception {
    JettyLauncher server = new JettyLauncher(8080);
    server.start();
  }
}
