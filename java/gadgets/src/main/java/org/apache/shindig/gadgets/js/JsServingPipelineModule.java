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

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import java.util.List;

/**
 * Guice configuration for the Javascript serving pipeline.
 */
public class JsServingPipelineModule extends AbstractModule {

  @Override
  protected void configure() {
    // nothing to configure here
  }

  @Provides
  @Inject
  @Named("shindig.js.pre-processors")
  public List<JsProcessor> provideProcessors(
      IfModifiedSinceProcessor ifModifiedSinceProcessor) {
    return ImmutableList.<JsProcessor>of(ifModifiedSinceProcessor);
  }

  @Provides
  @Inject
  @Named("shindig.js.optional-processors")
  public List<JsProcessor> provideProcessors(
      AddJslInfoVariableProcessor addJslInfoVariableProcessor,
      DeferJsProcessor deferJsProcessor,
      JsLoadProcessor jsLoaderGeneratorProcessor,
      GetJsContentProcessor getJsContentProcessor,
      CajaJsSubtractingProcessor cajaJsSubtractingProcessor,
      ExportJsProcessor exportJsProcessor,
      SeparatorCommentingProcessor separatorCommentingProcessor,
      ConfigInjectionProcessor configInjectionProcessor,
      AddJslLoadedVariableProcessor addJslLoadedVariableProcessor,
      AddOnloadFunctionProcessor addOnloadFunctionProcessor) {
    jsLoaderGeneratorProcessor.setUseAsync(true);
    return ImmutableList.of(
        addJslInfoVariableProcessor,
        deferJsProcessor,
        jsLoaderGeneratorProcessor,
        getJsContentProcessor,
        cajaJsSubtractingProcessor,
        exportJsProcessor,
        separatorCommentingProcessor,
        configInjectionProcessor,
        addJslLoadedVariableProcessor,
        addOnloadFunctionProcessor);
  }

  @Provides
  @Inject
  @Named("shindig.js.required-processors")
  public List<JsProcessor> provideProcessors(
      CompilationProcessor compilationProcessor,
      AnonFuncWrappingProcessor anonFuncProcessor) {
    return ImmutableList.of(
        compilationProcessor,
        anonFuncProcessor);
  }
}
