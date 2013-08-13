<?php
namespace apache\shindig\social\service;
use apache\shindig\common\IllegalArgumentException;
use apache\shindig\social\spi\CollectionOptions;
use apache\shindig\social\spi\UserId;
use apache\shindig\social\spi\GroupId;
use apache\shindig\social\spi\RestfulCollection;
use apache\shindig\common\SecurityToken;

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

class PersonHandler extends DataRequestHandler {

  protected static $PEOPLE_PATH = "/people/{userId}/{groupId}/{personId}";
  protected static $DEFAULT_FIELDS = array('id', 'displayName', 'gender', 'thumbnailUrl');

  protected static $ANONYMOUS_ID_TYPE = array('viewer', 'me');
  protected static $ANONYMOUS_VIEWER = array(
      'isOwner' => false,
      'isViewer' => true,
      'name' => 'anonymous_user',
      'displayName' => 'Guest'
  );

  public function __construct() {
    parent::__construct('person_service');
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handleDelete(RequestItem $request) {
    throw new SocialSpiException("You can't delete people.", ResponseError::$BAD_REQUEST);
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handlePut(RequestItem $request) {
    throw new SocialSpiException("You can't update right now.", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   *
   * @param RequestItem $requestItem
   * @throws SocialSpiException
   */
  public function handlePost(RequestItem $request) {
    throw new SocialSpiException("You can't add people right now.", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   * Allowed end-points /people/{userId}+/{groupId} /people/{userId}/{groupId}/{optionalPersonId}+
   *
   * examples: /people/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
   *
   * @param RequestItem $request
   * @return ResponseItem
   */
  public function handleGet(RequestItem $request) {
    $this->checkService();
    $request->applyUrlTemplate(self::$PEOPLE_PATH);

    $groupId = $request->getGroup();
    $optionalPersonId = $request->getListParameter("personId");
    $fields = $request->getFields(self::$DEFAULT_FIELDS);
    $userIds = $request->getUsers();

    // Preconditions
    if (count($userIds) < 1) {
      throw new IllegalArgumentException("No userId specified");
    } elseif (count($userIds) > 1 && count($optionalPersonId) != 0) {
      throw new IllegalArgumentException("Cannot fetch personIds for multiple userIds");
    }

    $options = new CollectionOptions();
    $options->setSortBy($request->getSortBy());
    $options->setSortOrder($request->getSortOrder());
    $options->setFilterBy($request->getFilterBy());
    $options->setFilterOperation($request->getFilterOperation());
    $options->setFilterValue($request->getFilterValue());
    $options->setStartIndex($request->getStartIndex());
    $options->setCount($request->getCount());

    $token = $request->getToken();
    $groupType = $groupId->getType();
    // handle Anonymous Viewer exceptions
    $containAnonymousUser = false;
    if ($token->isAnonymous()) {
      // Find out whether userIds contains
      // a) @viewer, b) @me, c) SecurityToken::$ANONYMOUS
      foreach ($userIds as $key=>$id) {
        if (in_array($id->getType(), self::$ANONYMOUS_ID_TYPE) ||
            (($id->getType() == 'userId') && ($id->getUserId($token) == SecurityToken::$ANONYMOUS))) {
          $containAnonymousUser = true;
          unset($userIds[$key]);
        }
      }
      if ($containAnonymousUser) {
        $userIds = array_values($userIds);
        // Skip any requests if groupId is not @self or @all, since anonymous viewer won't have friends.
        if (($groupType != 'self') && ($groupType != 'all')) {
          throw new \Exception("Can't get friend from an anonymous viewer.");
        }
      }
    }
    if ($containAnonymousUser && (count($userIds) == 0)) {
      return self::$ANONYMOUS_VIEWER;
    }
    $service = $this->service;
    $ret = null;
    if (count($userIds) == 1) {
      if (count($optionalPersonId) == 0) {
        if ($groupType == 'self') {
          $ret = $service->getPerson($userIds[0], $groupId, $fields, $token);
        } else {
          $ret = $service->getPeople($userIds, $groupId, $options, $fields, $token);
        }
      } elseif (count($optionalPersonId) == 1) {
        $ret = $service->getPerson($optionalPersonId[0], $groupId, $fields, $token);
      } else {
        $personIds = array();
        foreach ($optionalPersonId as $pid) {
          $personIds[] = new UserId('userId', $pid);
        }
        // Every other case is a collection response of optional person ids
        $ret = $service->getPeople($personIds, new GroupId('self', null), $options, $fields, $token);
      }
    } else {
      // Every other case is a collection response.
      $ret = $service->getPeople($userIds, $groupId, $options, $fields, $token);
    }
    // Append anonymous viewer
    if ($containAnonymousUser) {
      if (is_array($ret)) {
        // Single user
        $people = array($ret, self::$ANONYMOUS_VIEWER);
        $ret = new RestfulCollection($people, $options->getStartIndex(), 2);
        $ret->setItemsPerPage($options->getCount());
      } else {
        // Multiple users
        $ret->entry[] = self::$ANONYMOUS_VIEWER;
        $ret->totalResults += 1;
      }
    }
    return $ret;
  }
}
