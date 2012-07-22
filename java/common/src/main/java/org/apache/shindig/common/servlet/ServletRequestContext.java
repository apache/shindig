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
package org.apache.shindig.common.servlet;

import javax.servlet.ServletRequest;

public class ServletRequestContext {

  public final static String HOST = "host";
  public final static String PORT = "port";
  public final static String SCHEME = "scheme";

  public static void setRequestInfo(ServletRequest req) {
    host.set(req.getServerName());
    port.set("" + req.getServerPort());
    scheme.set(req.getScheme());

    // Temporary solution since variables are not available in forked thread during js processing
    System.setProperty(HOST, req.getServerName());
    System.setProperty(PORT, "" + req.getServerPort());
    System.setProperty(SCHEME, req.getScheme());
  }

  /**
   * A Thread Local holder for host
   */
  private static ThreadLocal<String> host = new ThreadLocal<String>();

  /**
   * A Thread Local holder for port
   */
  private static ThreadLocal<String> port = new ThreadLocal<String>();

  /**
   * A Thread Local holder for scheme
   */
  private static ThreadLocal<String> scheme = new ThreadLocal<String>();


  public static String getHost(){
    return host.get() != null ? host.get() : System.getProperty(HOST);
  }

  public static String getPort(){
    return port.get() != null ? port.get() : System.getProperty(PORT);
  }

  public static String getScheme(){
    return scheme.get() != null ? scheme.get() : System.getProperty(SCHEME);
  }
}
