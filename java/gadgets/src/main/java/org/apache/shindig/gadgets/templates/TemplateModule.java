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
package org.apache.shindig.gadgets.templates;

import org.apache.shindig.gadgets.templates.tags.FlashTagHandler;
import org.apache.shindig.gadgets.templates.tags.HtmlTagHandler;
import org.apache.shindig.gadgets.templates.tags.IfTagHandler;
import org.apache.shindig.gadgets.templates.tags.RenderTagHandler;
import org.apache.shindig.gadgets.templates.tags.RepeatTagHandler;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.apache.shindig.gadgets.templates.tags.VariableTagHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.Set;

/**
 * Guice Module to provide Template-specific classes 
 */
public class TemplateModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TemplateProcessor.class).to(DefaultTemplateProcessor.class);
    // TODO: switch to Guice multibindings when that JAR becomes available
    // in a Maven repository
    bind(new TypeLiteral<Set<TagHandler>>(){}).toProvider(TagHandlersProvider.class); 
  }
   
  public static class TagHandlersProvider implements Provider<Set<TagHandler>> {
    
    private final Set<TagHandler> handlers;
    
    @Inject
    public TagHandlersProvider(HtmlTagHandler htmlHandler, 
        IfTagHandler ifHandler, RepeatTagHandler repeatHandler, 
        RenderTagHandler renderHandler, FlashTagHandler flashHandler,
        VariableTagHandler variableHandler) {
      handlers = ImmutableSet.of((TagHandler) htmlHandler, ifHandler,
          repeatHandler, renderHandler, flashHandler,
          variableHandler);
    }
    
    public Set<TagHandler> get() {
      return handlers;
    }
  }
}
