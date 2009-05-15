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
package org.apache.shindig.gadgets;

import com.google.inject.ImplementedBy;

import java.util.Collection;

/**
 * Generates urls for various public entrypoints
 */
@ImplementedBy(DefaultUrlGenerator.class)
public interface UrlGenerator {
  /**
   * @param features The list of features that js is needed for.
   * @return The url for the bundled javascript that includes all referenced feature libraries.
   */
  String getBundledJsUrl(Collection<String> features, GadgetContext context);

  /**
   * @param features
   * @param context
   * @return The bundled js parameter for type=url gadgets.
   */
  String getBundledJsParam(Collection<String> features, GadgetContext context);

  /**
   * Generates iframe urls for meta data service.
   * Use this rather than generating your own urls by hand.
   *
   * @return The generated iframe url.
   */
  String getIframeUrl(Gadget gadget);
  
  /**
   * @return the oauthcallback URL on the gadget domain.  The returned URL may be absolute or
   * it may be scheme relative.
   */
  String getGadgetDomainOAuthCallback(String container, String gadgetHost);
}
