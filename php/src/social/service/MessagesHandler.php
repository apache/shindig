<?php
/**
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

class MessagesHandler extends DataRequestHandler {
  
  private static $MESSAGES_PATH = "/messages/userId/outbox/msgId";
  private $service;

  public function __construct() {
    $service = Config::get('messages_service');
    $this->service = new $service();
  }

  public function handleDelete(RequestItem $requestItem) {
    throw new SocialSpiException("You can't delete messages", ResponseError::$NOT_IMPLEMENTED);
  }

  public function handleGet(RequestItem $requestItem) {
    throw new SocialSpiException("You can't retrieve messages", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   * /messages/{groupId}/outbox/{msgId}
   * /messages/{groupId}/outbox
   *
   * @param RequestItem $requestItem
   * @return responseItem
   */
  public function handlePost(RequestItem $requestItem) {
    $requestItem->applyUrlTemplate(self::$MESSAGES_PATH);
    $userIds = $requestItem->getUsers();
    $message = $requestItem->getParameter('message');
    $optionalMessageId = $requestItem->getParameter('msgId');
    return $this->service->createMessage($userIds[0], $requestItem->getAppId(), $message, $optionalMessageId, $requestItem->getToken());
  }

  public function handlePut(RequestItem $requestItem) {
    $this->handlePost($requestItem);
  }
}
