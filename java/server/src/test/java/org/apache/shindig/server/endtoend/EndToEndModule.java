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
package org.apache.shindig.server.endtoend;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.canonical.JsonDbOpensocialService;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.AppDataService;
import org.apache.shindig.social.dataservice.DataServiceServletFetcher;
import org.apache.shindig.social.dataservice.PersonService;

/**
 * Guice module for the end-to-end tests.
 */
public class EndToEndModule extends AbstractModule {

  protected void configure() {
    bind(ActivityService.class).to(JsonDbOpensocialService.class);
    bind(PersonService.class).to(JsonDbOpensocialService.class);
    bind(AppDataService.class).to(JsonDbOpensocialService.class);
    bind(String.class).annotatedWith(Names.named("canonical.json.db"))
        .toInstance("sampledata/canonicaldb.json");
    bind(ParameterFetcher.class).annotatedWith(Names.named("DataServiceServlet"))
        .to(DataServiceServletFetcher.class);
  }
}
