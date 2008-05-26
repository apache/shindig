/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social;

import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.social.abdera.SocialApiProvider;

import org.apache.abdera.protocol.server.ServiceManager;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;

public class JettyServer {
  private Server server = null;

  public void start(int port, String mapBase) throws Exception {
    server = new Server(port);
    Context context = new Context(server, "/", Context.SESSIONS);
    context.addEventListener(new GuiceServletContextListener());

    Map<String, String> initParams = Maps.newHashMap();
    initParams.put(GuiceServletContextListener.MODULES_ATTRIBUTE,
        SocialApiTestsGuiceModule.class.getName());
    context.setInitParams(initParams);

    ServletHolder servletHolder = new ServletHolder(new RestServerServlet());
    servletHolder.setInitParameter(ServiceManager.PROVIDER,
        SocialApiProvider.class.getName());
    context.addServlet(servletHolder, mapBase);

    server.start();
  }

  public void stop() throws Exception {
    server.stop();
  }

}
