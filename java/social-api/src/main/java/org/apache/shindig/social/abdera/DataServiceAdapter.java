/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.shindig.social.abdera;

import org.apache.shindig.social.opensocial.DataService;

import com.google.inject.Inject;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * All "data" requests are processed here.
 *
 */
@SuppressWarnings("unchecked")
public class DataServiceAdapter extends RestServerCollectionAdapter {
  private DataService dataService;

  // TODO get these from the config files like in feedserver
  private static final String TITLE = "Data Collection title";
  private static final String AUTHOR = "TODO";

  @Inject
  public DataServiceAdapter(DataService dataService) {
    this.dataService = dataService;
  }

  /**
   * Handles the following URL
   *    /appdata/{uid}/@friends/{aid}
   */
  public ResponseContext getFeed(RequestContext request) {
    String uid = request.getTarget().getParameter("uid");
    String aid = request.getTarget().getParameter("aid");
    List<String> ids = getFriendIds(request, uid);

    Map<String, Map<String, String>> dataMap = dataService.getPersonData(ids,
        getKeys(request), getSecurityToken(request, uid)).getResponse();

    // TODO: This return type is not quite right. We should fix this to
    // match the spec once we have a full json format in place.
    List dataList = new ArrayList();
    dataList.add(dataMap);

    return returnFeed(request, TITLE, AUTHOR, dataList);
  }

  /**
   *  Handles the following URL
   *    /appdata/{uid}/@self/{aid}
   *  TODO: This is wrong. Getting data for @self is a collection too.
   */
  public ResponseContext getEntry(RequestContext request) {
    String uid = request.getTarget().getParameter("uid");
    String appId = request.getTarget().getParameter("aid");

    List<String> ids = new ArrayList<String>();
    ids.add(uid);

    Map<String, String> data = dataService.getPersonData(ids,
        getKeys(request), getSecurityToken(request, uid)).getResponse().get(uid);

    // TODO: how is entry id determined. check.
    String entryId = request.getUri().toString();
    return returnEntry(request, data == null ? new HashMap() : data,
        entryId, new Date());
  }

  private List<String> getKeys(RequestContext request) {
    String[] keyArray = request.getTarget().getParameter("fields").split(",");
    return Arrays.asList(keyArray);
  }
}