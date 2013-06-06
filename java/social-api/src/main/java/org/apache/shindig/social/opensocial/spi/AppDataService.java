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
package org.apache.shindig.social.opensocial.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Data Service SPI interface. This interface represents is used to retrieve information bound to a
 * person, there are methods to update and delete data.
 */
public interface AppDataService {

  /**
   * Retrives app data for the specified user list and group.
   *
   * @param userIds A set of UserIds.
   * @param groupId The group
   * @param appId   The app
   * @param fields  The fields to filter the data by. Empty set implies all
   * @param token   The security token
   * @return The data fetched
   */
  Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId,
      String appId, Set<String> fields, SecurityToken token) throws ProtocolException;

  /**
   * Deletes data for the specified user and group.
   *
   * @param userId  The user
   * @param groupId The group
   * @param appId   The app
   * @param fields  The fields to delete. Empty set implies all
   * @param token   The security token
   * @return an error if one occurs
   */
  Future<Void> deletePersonData(UserId userId, GroupId groupId,
      String appId, Set<String> fields, SecurityToken token) throws ProtocolException;

  /**
   * Updates app data for the specified user and group with the new values.
   *
   * @param userId  The user
   * @param groupId The group
   * @param appId   The app
   * @param fields  The fields to filter the data by. Empty set implies all
   * @param values  The values to set
   * @param token   The security token
   * @return an error if one occurs
   */
  Future<Void> updatePersonData(UserId userId, GroupId groupId,
      String appId, Set<String> fields, Map<String, Object> values, SecurityToken token)
      throws ProtocolException;
}
