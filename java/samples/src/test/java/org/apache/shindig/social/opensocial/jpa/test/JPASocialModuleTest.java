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
package org.apache.shindig.social.opensocial.jpa.test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.shindig.social.opensocial.jpa.spi.JPASocialModule;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.PersonService;

import org.junit.Test;

/**
 * 
 */
public class JPASocialModuleTest {

  @Test
  public void bootstrap() {
    Injector injector = Guice.createInjector(new JPASocialModule());
    @SuppressWarnings("unused")
    PersonService personService = injector.getInstance(PersonService.class);
    @SuppressWarnings("unused")
    ActivityService activityService = injector
        .getInstance(ActivityService.class);
    @SuppressWarnings("unused")
    AppDataService appDataService = injector.getInstance(AppDataService.class);
  }

}
