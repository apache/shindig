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
package org.apache.shindig.social.opensocial.jpa.spi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class SPIUtils {

  /**
   * @param userIds
   * @param token
   * @return
   */
  public static List<String> getUserList(Set<UserId> userIds, SecurityToken token) {
    // TODO What's the use of userIdMap?
    HashMap<String, String> userIdMap = Maps.newHashMap();
    List<String> paramList = Lists.newArrayList();
    for (UserId u : userIds) {
      try {
        String uid = u.getUserId(token);
        if (uid != null) {
          userIdMap.put(uid, uid);
          paramList.add(uid);
        }
      } catch (IllegalStateException istate) {
        // ignore the user id.
      }
    }
    return paramList;
  }

  /**
   * @param userId
   * @param token
   * @return
   */
  public static String getUserList(UserId userId, SecurityToken token) {
    return userId.getUserId(token);
  }

  public static <T> List<T> toList(Set<T> s) {
    List<T> l = new ArrayList<T>(s.size());
    l.addAll(s);
    return l;
  }

}
