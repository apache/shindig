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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Subclass with social specific extensions
 */
public class SocialRequestItem extends BaseRequestItem {

  String USER_ID = "userId";
  String GROUP_ID = "groupId";

  public SocialRequestItem(Map<String, String[]> parameters, 
      SecurityToken token, BeanConverter converter, BeanJsonConverter jsonConverter) {
    super(parameters, token, converter, jsonConverter);
  }

  public SocialRequestItem(JSONObject parameters, Map<String, FormDataItem> formItems,
      SecurityToken token, BeanConverter converter, BeanJsonConverter jsonConverter) {
    super(parameters, formItems, token, converter, jsonConverter);
  }

  public Set<UserId> getUsers() {
    List<String> ids = getListParameter(USER_ID);
    if (ids.isEmpty()) {
      if (token.getViewerId() != null) {
        // Assume @me
        ids = Lists.newArrayList("@me");
      } else {
        throw new IllegalArgumentException("No userId provided and viewer not available");
      }
    }
    Set<UserId> userIds = Sets.newLinkedHashSet();
    for (String id : ids) {
      userIds.add(UserId.fromJson(id));
    }
    return userIds;
  }

  public GroupId getGroup() {
    return GroupId.fromJson(getParameter(GROUP_ID, "@self"));
  }

  public String getSortBy() {
    String sortBy = super.getSortBy();
    return sortBy == null ? PersonService.TOP_FRIENDS_SORT : sortBy;
  }

}
