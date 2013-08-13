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
import com.google.inject.name.Named;

import java.util.List;

/**
 * Default implementation of {@link JsProcessorRegistry}, using an injected list
 * of processors.
 */
public class DefaultJsProcessorRegistry implements JsProcessorRegistry {

  private final List<JsProcessor> preProcessors;
  private final List<JsProcessor> optionalProcessors;
  private final List<JsProcessor> requiredProcessors;

  @Inject
  public DefaultJsProcessorRegistry(
      @Named("shindig.js.pre-processors") List<JsProcessor> preProcessors,
      @Named("shindig.js.optional-processors") List<JsProcessor> optionalProcessors,
      @Named("shindig.js.required-processors") List<JsProcessor> requiredProcessors) {
    this.preProcessors = preProcessors;
    this.optionalProcessors = optionalProcessors;
    this.requiredProcessors = requiredProcessors;
  }

  public void process(JsRequest request, JsResponseBuilder response) throws JsException {
    // JsProcessor defined in preProcessors can determine whether the js process really need to happen
    // Typically, IfModifiedSinceProcessor is one of the preProcessors, if it sets a 304 status code,
    // all the remaining JsProcessors in optional and required won't be started.
    for (JsProcessor processor : preProcessors) {
      if (!processor.process(request, response)){
        return;
      }
    }
    for (JsProcessor processor : optionalProcessors) {
      if (!processor.process(request, response)) {
        break;
      }
    }
    // This pipeline sequentially executes JsProcessor, and can stop on any, bypassing
    // the actual compilation process. This is put here so generated JS will still be
    // compiled.
    for (JsProcessor processor : requiredProcessors) {
      processor.process(request, response);
    }
  }
}
