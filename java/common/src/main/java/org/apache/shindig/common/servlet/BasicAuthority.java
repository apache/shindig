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
package org.apache.shindig.common.servlet;

import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.servlet.ServletRequestContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * Basic implementation for Authority Interface.
 *
 * Authority information is calculated based on following procedure.
 * 1. Optionally default host and default port can be provided as ServletContext Parameters in web.xml. Once it's provided,
 *    default host and default port are used to construct authority information.
 * 2. If default host and default port are not provided, host and port from current HttpServletRequest will be used if available.
 * 3. If HttpServletRequest is not available, jetty host/port will be used. This is required for junit tests.
 */
public class BasicAuthority implements Authority {
  private String host;
  private String port;
  public final static String JETTY_HOST = "jetty.host";
  public final static String JETTY_PORT = "jetty.port";

  @Inject
  public BasicAuthority(@Nullable @Named("shindig.host") String defaultHost,
      @Nullable @Named("shindig.port") String defaultPort) {
    if (!Strings.isNullOrEmpty(defaultHost)) {
      this.host = defaultHost;
    }
    if (!Strings.isNullOrEmpty(defaultPort)) {
      this.port = defaultPort;
    }
  }

  public String getAuthority() {
    return Joiner.on(':').join(
        Objects.firstNonNull(host, getServerHostname()),
        Objects.firstNonNull(port, getServerPort()));
  }

  public String getScheme(){
    return Objects.firstNonNull(ServletRequestContext.getScheme(), "http");
  }
  
  public String getOrigin(){
	return getScheme() + "://" + getAuthority();
  }

  private String getServerPort() {
    return Objects.firstNonNull(ServletRequestContext.getPort(),
        Objects.firstNonNull(System.getProperty(JETTY_PORT), "8080"));
  }

  private String getServerHostname() {
    return Objects.firstNonNull(ServletRequestContext.getHost(),
        Objects.firstNonNull(System.getProperty(JETTY_HOST), "localhost"));
  }
}
