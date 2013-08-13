<?php
namespace apache\shindig\social\service;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

class GroupHandler extends DataRequestHandler {
  private static $GROUPS_PATH = "/groups/{userId}";

  public function __construct() {
    parent::__construct('group_service');
  }

  /**
   * /groups/{userId}
   *
   * examples:
   * /groups/john.doe?fields=count
   * /groups/@me
   *
   * @param RequestItem $requestItem
   * @return ResponseItem
   */
  public function handleGet(RequestItem $requestItem) {
    $this->checkService();
    $requestItem->applyUrlTemplate(self::$GROUPS_PATH);
    $userIds = $requestItem->getUsers();
    if (count($userIds) < 1) {
      throw new \InvalidArgumentException("No userId(s) specified");
    }
    return $this->service->getPersonGroups($userIds[0], $requestItem->getGroup(), $requestItem->getToken());
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handleDelete(RequestItem $requestItem) {
    throw new SocialSpiException("You can't delete groups.", ResponseError::$BAD_REQUEST);
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handlePut(RequestItem $requestItem) {
    throw new SocialSpiException("You can't update groups.", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handlePost(RequestItem $requestItem) {
    throw new SocialSpiException("You can't add groups.", ResponseError::$NOT_IMPLEMENTED);
  }

}
