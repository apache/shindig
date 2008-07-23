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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.spi.AppDataService;

import com.google.inject.Inject;

import java.util.HashMap;
import java.util.concurrent.Future;

public class AppDataHandler extends DataRequestHandler {
  private AppDataService service;
  private static final String APP_DATA_PATH = "/people/{userId}/{groupId}/{appId}";

  @Inject
  public AppDataHandler(AppDataService service) {
    this.service = service;
  }

  /**
   * /people/{userId}/{groupId}/{appId}
   * - fields={field1, field2}
   *
   * examples:
   * /appdata/john.doe/@friends/app?fields=count
   * /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will
   * be pulled from the values and set on the person object. If there are no
   * fields vars then all of the data will be overridden.
   * @param request
   */
  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    request.parseUrlWithTemplate(APP_DATA_PATH);

    return service.deletePersonData(request.getUser(), request.getGroup(),
        request.getAppId(), request.getFields(), request.getToken());
  }

  /**
   * /appdata/{userId}/{groupId}/{appId}
   * - fields={field1, field2}
   *
   * examples:
   * /appdata/john.doe/@friends/app?fields=count
   * /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will
   * be pulled from the values and set on the person object. If there are no
   * fields vars then all of the data will be overridden.
   */
  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return handlePost(request);
  }

  /**
   * /appdata/{userId}/{groupId}/{appId}
   * - fields={field1, field2}
   *
   * examples:
   * /appdata/john.doe/@friends/app?fields=count
   * /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will
   * be pulled from the values and set. If there are no
   * fields vars then all of the data will be overridden.
   */
  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    request.parseUrlWithTemplate(APP_DATA_PATH);

    return service.updatePersonData(request.getUser(), request.getGroup(),
        request.getAppId(), request.getFields(), request.getPostData(HashMap.class),
        request.getToken());
  }

  /**
   * /appdata/{userId}/{groupId}/{appId}
   * - fields={field1, field2}
   *
   * examples:
   * /appdata/john.doe/@friends/app?fields=count
   * /appdata/john.doe/@self/app
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    request.parseUrlWithTemplate(APP_DATA_PATH);

    return service.getPersonData(request.getUser(), request.getGroup(),
        request.getAppId(), request.getFields(), request.getToken());
  }

}

