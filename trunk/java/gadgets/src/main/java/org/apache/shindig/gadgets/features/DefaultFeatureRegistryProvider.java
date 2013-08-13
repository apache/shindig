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
package org.apache.shindig.gadgets.features;

import com.google.inject.Inject;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.http.HttpResponse;

/**
 * Default feature registry provider that support only default (null) repository
 */
public class DefaultFeatureRegistryProvider implements FeatureRegistryProvider {

  private final FeatureRegistry registry;

  @Inject
  public DefaultFeatureRegistryProvider(FeatureRegistry registry) {
    this.registry = registry;
  }

  public FeatureRegistry get(String repository) throws GadgetException {
    if (repository == null) {
      return registry;
    }
    throw new GadgetException(Code.INVALID_PARAMETER, "Repository is not supported",
        HttpResponse.SC_BAD_REQUEST);
  }
}
