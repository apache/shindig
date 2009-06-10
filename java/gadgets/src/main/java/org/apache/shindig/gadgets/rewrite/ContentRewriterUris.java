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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.config.ContainerConfig;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Provider of base URIs to HTMLContentRewriter and CSSContentRewriter.
 */
public class ContentRewriterUris {
  private final ContainerConfig config;
  private final String proxyBaseNoGadget;
  private final String concatBaseNoGadget;

  static final String PROXY_BASE_CONFIG_PROPERTY = "gadgets.rewriteProxyBase";
  static final String CONCAT_BASE_CONFIG_PROPERTY = "gadgets.rewriteConcatBase";

  @Inject
  public ContentRewriterUris(
      ContainerConfig config,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget,
      @Named("shindig.content-rewrite.concat-url")String concatBaseNoGadget) {
    this.config = config;
    this.proxyBaseNoGadget = proxyBaseNoGadget;
    this.concatBaseNoGadget = concatBaseNoGadget;
  }
  
  public String getProxyBase(String container) {
    container = firstNonNull(container, ContainerConfig.DEFAULT_CONTAINER);
    
    return firstNonNull(config.getString(container, PROXY_BASE_CONFIG_PROPERTY),
        proxyBaseNoGadget);
  }
  
  public String getConcatBase(String container) {
    container = firstNonNull(container, ContainerConfig.DEFAULT_CONTAINER);
    
    return firstNonNull(config.getString(container, CONCAT_BASE_CONFIG_PROPERTY),
        concatBaseNoGadget);
  }
  private static <T> T firstNonNull(T first, T second) {
    return first != null ? first : Preconditions.checkNotNull(second);
  }
}
