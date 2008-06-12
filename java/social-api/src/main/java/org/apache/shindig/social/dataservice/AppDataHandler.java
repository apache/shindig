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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class AppDataHandler extends DataRequestHandler {
  private AppDataService service;

  @Inject
  public AppDataHandler(AppDataService service, BeanJsonConverter converter) {
    super(converter);
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
   */
  ResponseItem handleDelete(HttpServletRequest servletRequest,
      SecurityToken token) {
    String[] segments = getParamsFromRequest(servletRequest);

    String userId = segments[0];
    DataServiceServlet.GroupId groupId
        = DataServiceServlet.GroupId.fromJson(segments[1]);
    String appId = segments[2];

    List<String> fields = getListParam(servletRequest, "fields",
        Lists.<String>newArrayList());

    return service.deletePersonData(userId, groupId, fields,
        appId, token);
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
   */
  ResponseItem handlePut(HttpServletRequest servletRequest,
      SecurityToken token) {
    return handlePost(servletRequest, token);
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
   */
  ResponseItem handlePost(HttpServletRequest servletRequest,
      SecurityToken token) {
    String[] segments = getParamsFromRequest(servletRequest);

    String userId = segments[0];
    DataServiceServlet.GroupId groupId
        = DataServiceServlet.GroupId.fromJson(segments[1]);
    String appId = segments[2];

    List<String> fields = getListParam(servletRequest, "fields",
        Lists.<String>newArrayList());

    String jsonAppData = servletRequest.getParameter("entry");
    Map<String, String> values = Maps.newHashMap();
    values = converter.convertToObject(jsonAppData,
        (Class<Map<String, String>>) values.getClass());

    return service.updatePersonData(userId, groupId, fields, values,
        appId, token);
  }

  /**
   * /appdata/{userId}/{groupId}/{appId}
   * - fields={field1, field2}
   *
   * examples:
   * /appdata/john.doe/@friends/app?fields=count
   * /appdata/john.doe/@self/app
   */
  ResponseItem handleGet(HttpServletRequest servletRequest,
      SecurityToken token) {
    String[] segments = getParamsFromRequest(servletRequest);

    String userId = segments[0];
    DataServiceServlet.GroupId groupId
        = DataServiceServlet.GroupId.fromJson(segments[1]);
    String appId = segments[2];

    List<String> fields = getListParam(servletRequest, "fields",
        Lists.<String>newArrayList());

    return service.getPersonData(userId, groupId, fields, appId, token);
  }

}

