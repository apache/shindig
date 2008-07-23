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
package org.apache.shindig.social.sample.container;

import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.DataRequestHandler;
import org.apache.shindig.social.opensocial.service.HandlerProvider;
import org.apache.shindig.social.opensocial.service.PersonHandler;

import com.google.inject.Inject;

import java.util.Map;

public class SampleContainerHandlerProvider extends HandlerProvider {
  @Inject
  public SampleContainerHandlerProvider(PersonHandler peopleHandler,
      ActivityHandler activityHandler, AppDataHandler appDataHandler) {
    super(peopleHandler, activityHandler, appDataHandler);
    handlers.put("samplecontainer", SampleContainerHandler.class);
  }

  public Map<String, Class<? extends DataRequestHandler>> get() {
    return handlers;
  }
}
