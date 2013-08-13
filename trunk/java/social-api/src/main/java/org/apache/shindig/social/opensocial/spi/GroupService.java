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

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Group;

/**
 * A service for gathering group information for specific users.
 *
 * @since 2.0.0
 */
public interface GroupService {
  /**
   * @param userId  a userId object
   * @param options search/sort/filtering options
   * @param fields  Field search/sort
   * @param token   a valid security token
   * @return a collection of groups for a specific userId
   * @throws org.apache.shindig.protocol.ProtocolException
   */
  public Future<RestfulCollection<Group>> getGroups(UserId userId, CollectionOptions options,
    Set<String> fields, SecurityToken token) throws ProtocolException;

}
