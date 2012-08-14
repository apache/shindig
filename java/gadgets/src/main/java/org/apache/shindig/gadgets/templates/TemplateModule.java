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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.apache.shindig.gadgets.templates.tags.FlashTagHandler;
import org.apache.shindig.gadgets.templates.tags.HtmlTagHandler;
import org.apache.shindig.gadgets.templates.tags.IfTagHandler;
import org.apache.shindig.gadgets.templates.tags.RenderTagHandler;
import org.apache.shindig.gadgets.templates.tags.RepeatTagHandler;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.apache.shindig.gadgets.templates.tags.VarTagHandler;
import org.apache.shindig.gadgets.templates.tags.VariableTagHandler;

/**
 * Guice Module to provide Template-specific classes
 */
public class TemplateModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TemplateProcessor.class).to(DefaultTemplateProcessor.class);
    bindTagHandlers();
  }

  /* No need to subclass.
     You can add the same construct in your own modules to register your own tag handler.. */
  protected void bindTagHandlers() {
    Multibinder<TagHandler> tagBinder = Multibinder.newSetBinder(binder(), TagHandler.class);
    tagBinder.addBinding().to(HtmlTagHandler.class);
    tagBinder.addBinding().to(IfTagHandler.class);
    tagBinder.addBinding().to(RenderTagHandler.class);
    tagBinder.addBinding().to(RepeatTagHandler.class);
    tagBinder.addBinding().to(FlashTagHandler.class);
    tagBinder.addBinding().to(VariableTagHandler.class);
    tagBinder.addBinding().to(VarTagHandler.class);
  }
}
