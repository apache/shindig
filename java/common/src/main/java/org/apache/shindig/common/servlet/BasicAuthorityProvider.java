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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.common.Nullable;


/**
 * Simple provider of Authority information.
 * Optionally default host and default port can be provided as ServletContext Parameters in web.xml.
 * If default host/port are not provided, host/port from Current HttpServletRequest will be used if available.
 * If HttpServletRequest is not available, jetty host/port will be used.
 */
@Singleton
public  class BasicAuthorityProvider implements Provider<Authority> {

  private  final BasicAuthority basicAuthority;

  @Inject
  public BasicAuthorityProvider(BasicAuthority basicAuthority) {
    this.basicAuthority = basicAuthority;
  }

  /**
   * Constructor for test purpose
   *
   */
  public BasicAuthorityProvider(String host, String port) {
    this.basicAuthority = new BasicAuthority(host, port);
  }

  public Authority get() {
    return basicAuthority;
  }
}
