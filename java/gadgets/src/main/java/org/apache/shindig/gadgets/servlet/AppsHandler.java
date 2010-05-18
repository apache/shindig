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
package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.http.InvalidationService;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Handle Metadata requests via JSON-RPC
 *
 */
@Service(name = "apps")
public class AppsHandler {
  protected final ExecutorService executor;
  protected final Processor processor;
  protected final UrlGenerator urlGenerator;
  protected final ContainerConfig containerConfig;
  protected final SecurityTokenDecoder securityTokenDecoder;

  @Inject
  public AppsHandler(ExecutorService executor, Processor processor, UrlGenerator urlGenerator, ContainerConfig containerConfig,
                        SecurityTokenDecoder securityTokenDecoder) {
    this.executor = executor;
    this.processor = processor;
    this.urlGenerator = urlGenerator;
    this.containerConfig = containerConfig;
    this.securityTokenDecoder = securityTokenDecoder;
  }

  @Operation(httpMethods = {"POST","GET"}, path = "/get")
  public void get(BaseRequestItem request) {

  }
}