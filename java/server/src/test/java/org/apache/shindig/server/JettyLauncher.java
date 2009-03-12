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

import org.apache.shindig.auth.AuthenticationServletFilter;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.JsServlet;
import org.apache.shindig.gadgets.servlet.MakeRequestServlet;
import org.apache.shindig.gadgets.servlet.ProxyServlet;
import org.apache.shindig.gadgets.servlet.RpcServlet;
import org.apache.shindig.protocol.DataServiceServlet;
import org.apache.shindig.protocol.JsonRpcServlet;
import org.apache.shindig.server.endtoend.EndToEndModule;

import com.google.common.base.Join;
import com.google.common.collect.Maps;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

import java.io.IOException;
import java.util.Map;

/**
 * Simple programmatic initialization of Shindig using Jetty and common paths.
 */
public class JettyLauncher {

  private static final String GADGET_BASE = "/gadgets/ifr";
  private static final String PROXY_BASE = "/gadgets/proxy";
  private static final String MAKEREQUEST_BASE = "/gadgets/makeRequest";
  private static final String GADGETS_RPC_BASE = "/gadgets/rpc/*";
  private static final String REST_BASE = "/social/rest/*";
  private static final String JSON_RPC_BASE = "/social/rpc/*";
  private static final String CONCAT_BASE = "/gadgets/concat";
  private static final String GADGETS_FILES = "/gadgets/files/*";
  private static final String JS_BASE = "/gadgets/js/*";
  private static final String METADATA_BASE = "/gadgets/metadata/*";

  private Server server;

  private JettyLauncher(int port, final String trunk) throws IOException {

    server = new Server(port);

    Context context = new Context(server, "/", Context.SESSIONS);
    //context.setBaseResource(Resource.newClassPathResource("/endtoend"));
    context.setResourceBase(Resource.newClassPathResource("/endtoend").getFile().getAbsolutePath());

    ServletHolder defaultHolder = new ServletHolder(new DefaultServlet());
    context.addServlet(defaultHolder, "/");

    context.addEventListener(new GuiceServletContextListener());

    Map<String, String> initParams = Maps.newHashMap();
    String modules = Join
        .join(":", EndToEndModule.class.getName(), DefaultGuiceModule.class.getName(),
            PropertiesModule.class.getName(), OAuthModule.class.getName());

    initParams.put(GuiceServletContextListener.MODULES_ATTRIBUTE, modules);
    context.setInitParams(initParams);

    // Attach the ConcatProxyServlet - needed for rewriting
    ServletHolder concatHolder = new ServletHolder(new ConcatProxyServlet());
    context.addServlet(concatHolder, CONCAT_BASE);

    // Attach the JS
    ServletHolder jsHolder = new ServletHolder(new JsServlet());
    context.addServlet(jsHolder, JS_BASE);

    // Attach the metatdata handler
    ServletHolder metadataHolder = new ServletHolder(new RpcServlet());
    context.addServlet(metadataHolder, METADATA_BASE);

    // Attach the Proxy
    ServletHolder proxyHolder = new ServletHolder(new ProxyServlet());
    context.addServlet(proxyHolder, PROXY_BASE);

    // Attach the gadget rendering servlet
    ServletHolder gadgetServletHolder = new ServletHolder(new GadgetRenderingServlet());
    context.addServlet(gadgetServletHolder, GADGET_BASE);
    context.addFilter(AuthenticationServletFilter.class, GADGET_BASE, 0);

    // Attach the make-request servlet
    ServletHolder makeRequestHolder = new ServletHolder(new MakeRequestServlet());
    context.addServlet(makeRequestHolder, MAKEREQUEST_BASE);
    context.addFilter(AuthenticationServletFilter.class, MAKEREQUEST_BASE, 0);

    // Attach the gadgets rpc servlet
    ServletHolder gadgetsRpcServletHolder = new ServletHolder(new JsonRpcServlet());
    gadgetsRpcServletHolder.setInitParameter("handlers", "org.apache.shindig.gadgets.handlers");
    context.addServlet(gadgetsRpcServletHolder, GADGETS_RPC_BASE);
    context.addFilter(AuthenticationServletFilter.class, GADGETS_RPC_BASE, 0);
    
    // Attach DataServiceServlet
    ServletHolder restServletHolder = new ServletHolder(new DataServiceServlet());
    restServletHolder.setInitParameter("handlers", "org.apache.shindig.social.handlers");
    context.addServlet(restServletHolder, REST_BASE);
    context.addFilter(AuthenticationServletFilter.class, REST_BASE, 0);

    // Attach JsonRpcServlet
    ServletHolder rpcServletHolder = new ServletHolder(new JsonRpcServlet());
    rpcServletHolder.setInitParameter("handlers", "org.apache.shindig.social.handlers");
    context.addServlet(rpcServletHolder, JSON_RPC_BASE);
    context.addFilter(AuthenticationServletFilter.class, JSON_RPC_BASE, 0);

    DefaultServlet defaultServlet = new DefaultServlet() {
      public Resource getResource(String s) {
        // Skip Gzip
        if (s.endsWith(".gz")) return null;
        
        String stripped = s.substring("/gadgets/files/".length());
        try {
          return Resource.newResource(trunk + "/javascript/" + stripped);
        } catch (IOException ioe) {
          return Resource.newClassPathResource(s);
        }
      }
    };
    ServletHolder gadgetFiles = new ServletHolder(defaultServlet);
    context.addServlet(gadgetFiles, GADGETS_FILES);
  }

  public void start() throws Exception {
    server.start();
    server.join();
  }

  /**
   * Takes a single path which is the trunk root directory. Uses
   * current root otherwise
   */
  public static void main(String[] argv) throws Exception {
    String trunk = argv.length == 0 ? System.getProperty("user.dir") : argv[0];
    JettyLauncher server = new JettyLauncher(8080, trunk);
    server.start();
  }
}
