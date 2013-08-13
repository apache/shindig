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
package org.apache.shindig.gadgets.js;

import com.google.inject.Inject;

/**
 * Default implementation of {@link JsServingPipeline}.
 *
 * The processing steps are executed by a {@link JsProcessorRegistry}, which can
 * be configured or replaced to add and remove processing steps, or to execute
 * different processing steps depending on the context.
 */
public class DefaultJsServingPipeline implements JsServingPipeline {

  private final JsProcessorRegistry jsProcessorRegistry;

  @Inject
  public DefaultJsServingPipeline(JsProcessorRegistry jsProcessorRegistry) {
    this.jsProcessorRegistry = jsProcessorRegistry;
  }

  public JsResponse execute(JsRequest jsRequest) throws JsException {
    JsResponseBuilder resp = new JsResponseBuilder();
    jsProcessorRegistry.process(jsRequest, resp);
    final JsResponse response = resp.build();
    if (response.isError()) {
      throw new JsException(response.getStatusCode(), response.toErrorString());
    }
    return response;
  }
}
