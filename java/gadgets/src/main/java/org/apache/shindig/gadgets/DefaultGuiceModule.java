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

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.PreloadModule;
import org.apache.shindig.gadgets.render.RenderModule;
import org.apache.shindig.gadgets.rewrite.RewriteModule;
import org.apache.shindig.gadgets.templates.TemplateModule;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;

/**
 * Creates a module to supply all of the core gadget classes.
 */
public class DefaultGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {

    ExecutorService service = Executors.newCachedThreadPool();
    bind(Executor.class).toInstance(service);
    bind(ExecutorService.class).toInstance(service);

    install(new ParseModule());
    install(new PreloadModule());
    install(new RenderModule());
    install(new RewriteModule());
    install(new TemplateModule());

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);
  }
}
