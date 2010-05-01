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

class PersonHandler extends DataRequestHandler {
  
  private $personService;
  
  private static $PEOPLE_PATH = "/people/{userId}/{groupId}/{personId}";
  private static $DEFAULT_FIELDS = array('ID', 'NAME', 'GENDER', 'THUMBNAIL_URL');

  public function __construct() {
    $service = Config::get('person_service');
    $this->personService = new $service();
  }

  public function handleDelete(RequestItem $request) {
    throw new SocialSpiException("You can't delete people.", ResponseError::$BAD_REQUEST);
  }

  public function handlePut(RequestItem $request) {
    throw new SocialSpiException("You can't update right now.", ResponseError::$NOT_IMPLEMENTED);
  }

  public function handlePost(RequestItem $request) {
    throw new SocialSpiException("You can't add people right now.", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   * Allowed end-points /people/{userId}+/{groupId} /people/{userId}/{groupId}/{optionalPersonId}+
   *
   * examples: /people/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
   */
  public function handleGet(RequestItem $request) {
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
    // personId: Array (     [0] => 8 ) 
    if (count($userIds) == 1) {
      if (count($optionalPersonId) == 0) {
        if ($groupId->getType() == 'self') {
          return $this->personService->getPerson($userIds[0], $groupId, $fields, $request->getToken());
        } else {
          return $this->personService->getPeople($userIds, $groupId, $options, $fields, $request->getToken());
        }
      } elseif (count($optionalPersonId) == 1) {
        return $this->personService->getPerson($optionalPersonId[0], $groupId, $fields, $request->getToken());
      } else {
        $personIds = array();
        foreach ($optionalPersonId as $pid) {
          $personIds[] = new UserId('userId', $pid);
        }
        // Every other case is a collection response of optional person ids
        return $this->personService->getPeople($personIds, new GroupId('self', null), $options, $fields, $request->getToken());
      }
    }
    // Every other case is a collection response.
    return $this->personService->getPeople($userIds, $groupId, $options, $fields, $request->getToken());
  }
}
