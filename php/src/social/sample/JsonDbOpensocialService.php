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

/**
 * Implementation of supported services backed by a JSON DB
 */
class JsonDbOpensocialService implements ActivityService, PersonService, AppDataService, MessagesService {
  
  /**
   * The DB
   */
  private $db;
  
  /**
   * db["activities"] -> Array<Person>
   */
  private static $PEOPLE_TABLE = "people";
  
  /**
   * db["people"] -> Map<Person.Id, Array<Activity>>
   */
  private static $ACTIVITIES_TABLE = "activities";
  
  /**
   * db["data"] -> Map<Person.Id, Map<String, String>>
   */
  private static $DATA_TABLE = "data";
  
  /**
   * db["friendLinks"] -> Map<Person.Id, Array<Person.Id>>
   */
  private static $FRIEND_LINK_TABLE = "friendLinks";
  
  /**
   * db["userApplications"] -> Map<Person.Id, Array<Application Ids>>
   */
  private static $USER_APPLICATIONS_TABLE = "userApplications";
  
  private $allPeople = null;
  
  private $allData = null;
  
  private $allActivities = null;
  
  private $jsonDbFileName = 'ShindigDb.json';

  public function getDb() {
    try {
      $fileName = sys_get_temp_dir() . '/' . $this->jsonDbFileName;
      if (file_exists($fileName)) {
        if (! is_readable($fileName)) {
          throw new SocialSpiException("Could not read temp json db file: $fileName, check permissions", ResponseError::$INTERNAL_ERROR);
        }
        $cachedDb = file_get_contents($fileName);
        $jsonDecoded = json_decode($cachedDb, true);
        if ($jsonDecoded == $cachedDb) {
          throw new SocialSpiException("Failed to decode the json db", ResponseError::$INTERNAL_ERROR);
        }
        return $jsonDecoded;
      } else {
        $jsonDb = Config::get('jsondb_path');
        if (! file_exists($jsonDb) || ! is_readable($jsonDb)) {
          throw new SocialSpiException("Could not read json db file: $jsonDb, check if the file exists & has proper permissions", ResponseError::$INTERNAL_ERROR);
        }
        $dbConfig = @file_get_contents($jsonDb);
        $contents = preg_replace('/[^http:\/\/|^https:\/\/]\/\/.*$/m', '', preg_replace('@/\\*(?:.|[\\n\\r])*?\\*/@', '', $dbConfig));
        $jsonDecoded = json_decode($contents, true);
        if ($jsonDecoded == $contents) {
          throw new SocialSpiException("Failed to decode the json db", ResponseError::$INTERNAL_ERROR);
        }
        $this->saveDb($jsonDecoded);
        return $jsonDecoded;
      }
    } catch (Exception $e) {
      throw new SocialSpiException("An error occured while reading/writing the json db: " . $e->getMessage(), ResponseError::$INTERNAL_ERROR);
    }
  }

  private function saveDb($db) {
    if (! @file_put_contents(sys_get_temp_dir() . '/' . $this->jsonDbFileName, json_encode($db))) {
      throw new Exception("Could not save json db: " . sys_get_temp_dir() . '/' . $this->jsonDbFileName);
    }
  }

  private function getAllPeople() {
    $db = $this->getDb();
    $peopleTable = $db[self::$PEOPLE_TABLE];
    foreach ($peopleTable as $people) {
      $this->allPeople[$people['id']] = $people;
    }
    $db[self::$PEOPLE_TABLE] = $this->allPeople;
    return $this->allPeople;
  }

  private function getAllData() {
    $db = $this->getDb();
    $dataTable = $db[self::$DATA_TABLE];
    foreach ($dataTable as $key => $value) {
      $this->allData[$key] = $value;
    }
    $db[self::$DATA_TABLE] = $this->allData;
    return $this->allData;
  }

  private function getAllActivities() {
    $db = $this->getDb();
    $activitiesTable = $db[self::$ACTIVITIES_TABLE];
    foreach ($activitiesTable as $key => $value) {
      $this->allActivities[$key] = $value;
    }
    $db[self::$ACTIVITIES_TABLE] = $this->allActivities;
    return $this->allActivities;
  }

  private function getPeopleWithApp($appId) {
    $peopleWithApp = array();
    $db = $this->getDb();
    $userApplicationsTable = $db[self::$USER_APPLICATIONS_TABLE];
    foreach ($userApplicationsTable as $key => $value) {
      if (in_array($appId, $userApplicationsTable[$key])) {
        $peopleWithApp[] = $key;
      }
    }
    return $peopleWithApp;
  }

  public function getPerson($userId, $groupId, $fields, SecurityToken $token) {
    if (! is_object($groupId)) {
      throw new SocialSpiException("Not Implemented", ResponseError::$NOT_IMPLEMENTED);
    }
    $person = $this->getPeople($userId, $groupId, new CollectionOptions(), $fields, $token);
    if (is_array($person->getEntry())) {
      $person = $person->getEntry();
      if (is_array($person) && count($person) == 1) {
        return array_pop($person);
      }
    }
    throw new SocialSpiException("Person not found", ResponseError::$BAD_REQUEST);
  }

  public function getPeople($userId, $groupId, CollectionOptions $options, $fields, SecurityToken $token) {
    
    $sortOrder = $options->getSortOrder();
    $filter = $options->getFilterBy();
    $first = $options->getStartIndex();
    $max = $options->getCount();
    $networkDistance = $options->getNetworkDistance();
    $ids = $this->getIdSet($userId, $groupId, $token);
    $allPeople = $this->getAllPeople();
    if (! $token->isAnonymous() && $filter == "hasApp") {
      $appId = $token->getAppId();
      $peopleWithApp = $this->getPeopleWithApp($appId);
    }
    $people = array();
    foreach ($ids as $id) {
      if ($filter == "hasApp" && ! in_array($id, $peopleWithApp)) {
        continue;
      }
      $person = null;
      if (is_array($allPeople) && isset($allPeople[$id])) {
        $person = $allPeople[$id];
        if (! $token->isAnonymous() && $id == $token->getViewerId()) {
          $person['isViewer'] = true;
        }
        if (! $token->isAnonymous() && $id == $token->getOwnerId()) {
          $person['isOwner'] = true;
        }
        if (!isset($fields[0]) || $fields[0] != '@all') {
          $newPerson = array();
          $newPerson['isOwner'] = isset($person['isOwner']) ? $person['isOwner'] : false;
          $newPerson['isViewer'] = isset($person['isViewer']) ? $person['isViewer'] : false;
          $newPerson['name'] = $person['name'];
          $newPerson['displayName'] = $person['displayName'];
          foreach ($fields as $field => $present) {
            $present = strtolower($present);
            if (isset($person[$present]) && ! isset($newPerson[$present])) {
              $newPerson[$present] = $person[$present];
            }
          }
          $person = $newPerson;
        }
        $people[$id] = $person;
      }
    }
    if ($sortOrder == 'name') {
      usort($people, array($this, 'comparator'));
    }
    
    try {
      $people = $this->filterResults($people, $options);
    } catch(Exception $e) {
      $people['filtered'] = 'false';
    }
    
    //TODO: The samplecontainer doesn't support any filters yet. We should fix this.
    $totalSize = count($people);
    $collection = new RestfulCollection($people, $options->getStartIndex(), $totalSize);
    $collection->setItemsPerPage($options->getCount());
    return $collection;
  }
  
  private function filterResults($peopleById, $options) {
    if (! $options->getFilterBy()) {
      return $peopleById; // no filtering specified
    }
    $filterBy = $options->getFilterBy();
    $op = $options->getFilterOperation();
    if (! $op) {
      $op = CollectionOptions::FILTER_OP_EQUALS; // use this container-specific default
    }
    $value = $options->getFilterValue();
    $filteredResults = array();
    $numFilteredResults = 0;
    foreach ($peopleById as $id => $person) {
      if ($this->passesFilter($person, $filterBy, $op, $value)) {
        $filteredResults[$id] = $person;
        $numFilteredResults ++;
      }
    }
    return $filteredResults;
  }

  private function passesFilter($person, $filterBy, $op, $value) {
    $fieldValue = $person[$filterBy];
    if (! $fieldValue || (is_array($fieldValue) && ! count($fieldValue))) {
      return false; // person is missing the field being filtered for
    }
    if ($op == CollectionOptions::FILTER_OP_PRESENT) {
      return true; // person has a non-empty value for the requested field
    }
    if (!$value) {
      return false; // can't do an equals/startswith/contains filter on an empty filter value
    }
    // grab string value for comparison
    if (is_array($fieldValue)) {
      // plural fields match if any instance of that field matches
      foreach ($fieldValue as $field) {
        if ($this->passesStringFilter($field, $op, $value)) {
          return true;
        }
      }
    } else {
      return $this->passesStringFilter($fieldValue, $op, $value);
    }
    
    return false;
  }

  public function getPersonData($userId, GroupId $groupId, $appId, $fields, SecurityToken $token) {
    if (! isset($fields[0])) {
      $fields[0] = '@all';
    }
    $db = $this->getDb();
    $allData = $this->getAllData();
    $friendsTable = $db[self::$FRIEND_LINK_TABLE];
    $data = array();
    $ids = $this->getIdSet($userId, $groupId, $token);
    foreach ($ids as $id) {
      if (isset($allData[$id])) {
        $allPersonData = $allData[$id];
        $personData = array();
        foreach (array_keys($allPersonData) as $key) {
          if (in_array($key, $fields) || in_array("@all", $fields)) {
            $personData[$key] = $allPersonData[$key];
          }
        }
        $data[$id] = $personData;
      }
    }
    return new DataCollection($data);
  }

  public function updatePersonData(UserId $userId, GroupId $groupId, $appId, $fields, $values, SecurityToken $token) {
    $db = $this->getDb();
    foreach ($fields as $key => $present) {
      if (! $this->isValidKey($present)) {
        throw new SocialSpiException("The person app data key had invalid characters", ResponseError::$BAD_REQUEST);
      }
    }
    $allData = $this->getAllData();
    $person = $allData[$userId->getUserId($token)];
    switch ($groupId->getType()) {
      case 'self':
        foreach ($fields as $key => $present) {
          $value = isset($values[$present]) ? @$values[$present] : null;
          $person[$present] = $value;
        }
        break;
      default:
        throw new SocialSpiException("We don't support updating data in batches yet", ResponseError::$NOT_IMPLEMENTED);
        break;
    }
    $allData[$userId->getUserId($token)] = $person;
    $db[self::$DATA_TABLE] = $allData;
    $this->saveDb($db);
    return null;
  }

  public function deletePersonData($userId, GroupId $groupId, $appId, $fields, SecurityToken $token) {
  	$db = $this->getDb();
  	$allData = $this->getAllData();
  	if ($fields == null || $fields[0] == '*') {
      $allData[$userId->getUserId($token)] = null;
      $db[self::$DATA_TABLE] = $allData;
      $this->saveDb($db);
      return null;
  	}
    foreach ($fields as $key => $present) {
      if (! $this->isValidKey($key)) {
        throw new SocialSpiException("The person app data key had invalid characters", ResponseError::$BAD_REQUEST);
      }
    }
    switch ($groupId->getType()) {
      case 'self':
        foreach ($fields as $key => $present) {
          $value = isset($values[$key]) ? null : @$values[$key];
          $person[$key] = $value;
        }
        $allData[$userId->getUserId($token)] = $person;
        $db[self::$DATA_TABLE] = $allData;
        $this->saveDb($db);
        break;
      default:
        throw new SocialSpiException("We don't support updating data in batches yet", ResponseError::$NOT_IMPLEMENTED);
        break;
    }
    return null;
  }

  public function getActivity($userId, $groupId, $appdId, $fields, $activityId, SecurityToken $token) {
    $activities = $this->getActivities($userId, $groupId, $appdId, null, null, null, null, $fields, array($activityId), $token);
    if ($activities instanceof RestfulCollection) {
      $activities = $activities->getEntry();
      foreach ($activities as $activity) {
        if ($activity->getId() == $activityId) {
          return $activity;
        }
      }
    }
    throw new SocialSpiException("Activity not found", ResponseError::$NOT_FOUND);
  }

  public function getActivities($userIds, $groupId, $appId, $sortBy, $filterBy, $filterOp, $filterValue, $startIndex, $count, $fields, $activityIds, $token) {
    $db = $this->getDb();
    $friendsTable = $db[self::$FRIEND_LINK_TABLE];
    $ids = array();
    $ids = $this->getIdSet($userIds, $groupId, $token);
    $allActivities = $this->getAllActivities();
    $activities = array();
    foreach ($ids as $id) {
      if (isset($allActivities[$id])) {
        $personsActivities = $allActivities[$id];
        $activities = array_merge($activities, $personsActivities);
        if ($fields) {
          $newPersonsActivities = array();
          foreach ($personsActivities as $activity) {
            $newActivity = array();
            foreach ($fields as $field => $present) {
              $newActivity[$present] = $activity[$present];
            }
            $newPersonsActivities[] = $newActivity;
          }
          $personsActivities = $newPersonsActivities;
          $activities = $personsActivities;
        }
        if ($filterBy && $filterValue) {
          $newActivities = array();
          foreach ($activities as $activity) {
            if (array_key_exists($filterBy, $activity)) {
              if ($this->passesStringFilter($activity[$filterBy], $filterOp, $filterValue)) {
                $newActivities[] = $activity;
              }
            } else {
              throw new SocialSpiException("Invalid filterby parameter", ResponseError::$NOT_FOUND);
            }
          }
          $activities = $newActivities;
        }
      }
    }
    $totalResults = count($activities);
    if (! $totalResults) {
      throw new SocialSpiException("Activity not found", ResponseError::$NOT_FOUND);
    }
    $activities = array_slice($activities, $startIndex, $count);
    $ret = new RestfulCollection($activities, $startIndex, $totalResults);
    $ret->setItemsPerPage($count);
    return $ret;
  }

  /*
	* 
	* to check the activity against filter
	*
	*/
  private function passesStringFilter($fieldValue, $filterOp, $filterValue) {
    switch ($filterOp) {
      case CollectionOptions::FILTER_OP_EQUALS:
        return $fieldValue == $filterValue;
      case CollectionOptions::FILTER_OP_CONTAINS:
        return strpos($fieldValue, $filterValue) !== false;
      case CollectionOptions::FILTER_OP_STARTSWITH:
        return strpos($fieldValue, $filterValue) === 0;
      default:
        throw new Exception('unrecognized filterOp');
    }
  }

  public function createActivity($userId, $groupId, $appId, $fields, $activity, SecurityToken $token) {
    $db = $this->getDb();
    $activitiesTable = $this->getAllActivities();
    $activity['appId'] = $token->getAppId();
    try {
      array_push($activitiesTable[$userId->getUserId($token)], $activity);
      $db[self::$ACTIVITIES_TABLE] = $activitiesTable;
      $this->saveDb($db);
      // Should this return something to show success?
    } catch (Exception $e) {
      throw new SocialSpiException("Activity can't be created: " . $e->getMessage(), ResponseError::$INTERNAL_ERROR);
    }
  }

  public function deleteActivities($userId, $groupId, $appId, $activityIds, SecurityToken $token) {
    throw new SocialSpiException("Not implemented", ResponseError::$NOT_IMPLEMENTED);
  }

  public function createMessage($userId, $appId, $message, $optionalMessageId, SecurityToken $token) {
    throw new SocialSpiException("Not implemented", ResponseError::$NOT_IMPLEMENTED);
  }

  /**
   * Determines whether the input is a valid key.
   *
   * @param key the key to validate.
   * @return true if the key is a valid appdata key, false otherwise.
   */
  public static function isValidKey($key) {
    if (empty($key)) {
      return false;
    }
    for ($i = 0; $i < strlen($key); ++ $i) {
      $c = substr($key, $i, 1);
      if (($c >= 'a' && $c <= 'z') || ($c >= 'A' && $c <= 'Z') || ($c >= '0' && $c <= '9') || ($c == '-') || ($c == '_') || ($c == '.')) {
        continue;
      }
      return false;
    }
    return true;
  }

  private function comparator($person, $person1) {
    $name = $person['name']['unstructured'];
    $name1 = $person1['name']['unstructured'];
    if ($name == $name1) {
      return 0;
    }
    return ($name < $name1) ? - 1 : 1;
  }

  /**
   * Get the set of user id's from a user or collection of users, and group
   * Code taken from http://code.google.com/p/partuza/source/browse/trunk/Shindig/PartuzaService.php
   */
  private function getIdSet($user, GroupId $group, SecurityToken $token) {
    $ids = array();
    $db = $this->getDb();
    $friendsTable = $db[self::$FRIEND_LINK_TABLE];
    if ($user instanceof UserId) {
      $userId = $user->getUserId($token);
      if ($group == null) {
        return array($userId);
      }
      switch ($group->getType()) {
        case 'self':
          $ids[] = $userId;
          break;
        case 'all':
        case 'friends':
          if (is_array($friendsTable) && count($friendsTable) && isset($friendsTable[$userId])) {
            $ids = $friendsTable[$userId];
          }
          break;
        default:
          return new ResponseItem(NOT_IMPLEMENTED, "We don't support fetching data in batches yet", null);
          break;
      }
    } elseif (is_array($user)) {
      $ids = array();
      foreach ($user as $id) {
        $ids = array_merge($ids, $this->getIdSet($id, $group, $token));
      }
    }
    return $ids;
  }
}
