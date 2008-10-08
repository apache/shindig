<?php
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

	public function getDb()
	{
		try {
			$fileName = sys_get_temp_dir() . '/' . $this->jsonDbFileName;
			if (file_exists($fileName)) {
				if (! is_readable($fileName)) {
					throw new Exception("Could not read temp json db file: $fileName, check permissions");
				}
				$cachedDb = file_get_contents($fileName);
				$jsonDecoded = json_decode($cachedDb, true);
				if ($jsonDecoded == $cachedDb) {
					throw new Exception("Failed to decode the json db");
				}
				return $jsonDecoded;
			} else {
				$jsonDb = Config::get('jsondb_path');
				if (! file_exists($jsonDb) || ! is_readable($jsonDb)) {
					throw new Exception("Could not read json db file: $jsonDb, check if the file exists & has proper permissions");
				}
				$dbConfig = @file_get_contents($jsonDb);
				$contents = preg_replace('/[^http:\/\/|^https:\/\/]\/\/.*$/m', '', preg_replace('@/\\*(?:.|[\\n\\r])*?\\*/@', '', $dbConfig));
				$jsonDecoded = json_decode($contents, true);
				if ($jsonDecoded == $contents) {
					throw new Exception("Failed to decode the json db");
				}
				$this->saveDb($jsonDecoded);
				return $jsonDecoded;
			}
		} catch (Exception $e) {
			throw new Exception("An error occured while reading/writing the json db: " . $e);
		}
	}

	private function saveDb($db)
	{
		if (! @file_put_contents(sys_get_temp_dir() . '/' . $this->jsonDbFileName, json_encode($db))) {
			throw new Exception("Could not save json db: " . sys_get_temp_dir() . '/' . $this->jsonDbFileName);
		}
	}

	/**
	 * Get the set of user id's from a user and group
	 */
	private function getIdSet(UserId $user, $group, SecurityToken $token)
	{
		$userId = $user->getUserId($token);
		if ($group == null) {
			return array($userId);
		}
		$returnVal = array();
		switch ($group->getType()) {
			case 'all':
			case 'friends':
			case 'groupId':
				$db = $this->getDb();
				$friendsLinkTable = $db[self::$FRIEND_LINK_TABLE];
				if (isset($friendsLinkTable[$userId])) {
					$friends = $friendsLinkTable[$userId];
					foreach ($friends as $friend) {
						$returnVal[$friend['id']] = $friend;
					}
				}
				break;
			case 'self':
				$returnVal[$userId] = $userId;
				break;
		}
		return $returnVal;
	}

	private function getAllPeople()
	{
		$db = $this->getDb();
		$peopleTable = $db[self::$PEOPLE_TABLE];
		foreach ($peopleTable as $people) {
			$this->allPeople[$people['id']] = $people;
		}
		$db[self::$PEOPLE_TABLE] = $this->allPeople;
		return $this->allPeople;
	}

	private function getAllData()
	{
		$db = $this->getDb();
		$dataTable = $db[self::$DATA_TABLE];
		foreach ($dataTable as $key => $value) {
			$this->allData[$key] = $value;
		}
		$db[self::$DATA_TABLE] = $this->allData;
		return $this->allData;
	}

	private function getAllActivities()
	{
		$db = $this->getDb();
		$activitiesTable = $db[self::$ACTIVITIES_TABLE];
		foreach ($activitiesTable as $key => $value) {
			$this->allActivities[$key] = $value;
		}
		$db[self::$ACTIVITIES_TABLE] = $this->allActivities;
		return $this->allActivities;
	}

	private function getPeopleWithApp($appId)
	{
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

	public function getPerson($userId, $groupId, $fields, SecurityToken $token)
	{
		$person = $this->getPeople($userId, $groupId, new CollectionOptions(), $fields, $token);
		// return of getPeople is a ResponseItem(RestfulCollection(ArrayOfPeople)), disassemble to return just one person
		$person = $person->getResponse()->getEntry();
		if (is_array($person) && count($person) == 1) {
			return new ResponseItem(null, null, $person[0]);
		}
		return new ResponseItem(NOT_FOUND, "Person not found", null);
	}

	public function getPeople($userId, $groupId, CollectionOptions $options, $fields, SecurityToken $token)
	{
		$sortOrder = $options->getSortOrder();
		$filter = $options->getFilterBy();
		$first = $options->getStartIndex();
		$max = $options->getCount();
		$networkDistance = $options->getNetworkDistance();
		
		$db = $this->getDb();
		$friendsTable = $db[self::$FRIEND_LINK_TABLE];
		$ids = array();
		$group = is_object($groupId) ? $groupId->getType() : '';
		switch ($group) {
			case 'all':
			case 'friends':
				if (is_array($friendsTable) && count($friendsTable) && isset($friendsTable[$userId->getUserId($token)])) {
					$ids = $friendsTable[$userId->getUserId($token)];
				}
				break;
			case 'self':
			default:
				$ids[] = $userId->getUserId($token);
				break;
		}
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
				if (! isset($fields['@all'])) {
					$newPerson = array();
					$newPerson['isOwner'] = isset($person['isOwner']) ? $person['isOwner'] : false;
					$newPerson['isViewer'] = isset($person['isViewer']) ? $person['isViewer'] : false;
					$newPerson['name'] = $person['name'];
					foreach ($fields as $field => $present) {
						if (isset($person[$field]) && ! isset($newPerson[$field])) {
							$newPerson[$field] = $person[$field];
						}
					}
					$person = $newPerson;
				}
				$people[] = $person;
			}
		}
		if ($sortOrder == 'name') {
			usort($people, array($this, 'comparator'));
		}
		//TODO: The samplecontainer doesn't support any filters yet. We should fix this.
		$totalSize = count($people);
		$last = $first + $max;
		$last = min($last, $totalSize);
		if ($first !== false && $first != null && $last) {
			$people = array_slice($people, $first, $last);
		}
		$collection = new RestfulCollection($people, $first, $totalSize);
		return new ResponseItem(null, null, $collection);
	}

	public function getPersonData($userId, GroupId $groupId, $appId, $fields, SecurityToken $token)
	{
		$db = $this->getDb();
		$allData = $this->getAllData();
		$friendsTable = $db[self::$FRIEND_LINK_TABLE];
		$data = array();
		$ids = array();
		switch ($groupId->getType()) {
			case 'self':
				$ids[] = $userId->getUserId($token);
				break;
			case 'all':
			case 'friends':
				if (is_array($friendsTable) && count($friendsTable) && isset($friendsTable[$userId->getUserId($token)])) {
					$ids = $friendsTable[$userId->getUserId($token)];
				}
				break;
			default:
				return new ResponseItem(NOT_IMPLEMENTED, "We don't support fetching data in batches yet", null);
				break;
		}
		foreach ($ids as $id) {
			if (isset($allData[$id])) {
				$allPersonData = $allData[$id];
				$personData = array();
				foreach (array_keys($allPersonData) as $key) {
					if (isset($fields[$key]) || isset($fields['@all'])) {
						$personData[$key] = $allPersonData[$key];
					}
				}
				$data[$id] = $personData;
			}
		}
		return new ResponseItem(null, null, RestfulCollection::createFromEntry($data));
	}

	public function updatePersonData(UserId $userId, GroupId $groupId, $appId, $fields, $values, SecurityToken $token)
	{
		$db = $this->getDb();
		foreach ($fields as $key => $present) {
			if (! $this->isValidKey($key)) {
				return new ResponseItem(BAD_REQUEST, "The person app data key had invalid characters", null);
			}
		}
		$allData = $this->getAllData();
		$person = $allData[$userId->getUserId($token)];
		switch ($groupId->getType()) {
			case 'self':
				foreach ($fields as $key => $present) {
					$value = isset($values[$key]) ? @$values[$key] : null;
					$person[$key] = $value;
				}
				break;
			default:
				return new ResponseItem(NOT_IMPLEMENTED, "We don't support updating data in batches yet", null);
				break;
		}
		$allData[$userId->getUserId($token)] = $person;
		$db[self::$DATA_TABLE] = $allData;
		$this->saveDb($db);
		return new ResponseItem(null, null, array());
	}

	public function deletePersonData($userId, GroupId $groupId, $appId, $fields, SecurityToken $token)
	{
		foreach ($fields as $key => $present) {
			if (! $this->isValidKey($key)) {
				return new ResponseItem(BAD_REQUEST, "The person app data key had invalid characters", null);
			}
		}
		switch ($groupId->getType()) {
			case 'self':
				foreach ($fields as $key => $present) {
					//TODO: actually implement this!  
				}
				break;
			default:
				return new ResponseItem(NOT_IMPLEMENTED, "We don't support deleting data in batches yet", null);
				break;
		}
		return new ResponseItem(null, null, array());
	}

	public function getActivities($userId, $groupId, $first, $max, SecurityToken $token)
	{
		$db = $this->getDb();
		$friendsTable = $db[self::$FRIEND_LINK_TABLE];
		$ids = array();
		switch ($groupId->getType()) {
			case 'all':
			case 'friends':
				if (is_array($friendsTable) && count($friendsTable) && isset($friendsTable[$userId->getUserId($token)])) {
					$ids = $friendsTable[$userId->getUserId($token)];
				}
				break;
			case 'self':
				$ids[] = $userId->getUserId($token);
				break;
		}
		$allActivities = $this->getAllActivities();
		$activities = array();
		foreach ($ids as $id) {
			if (isset($allActivities[$id])) {
				$activities = array_merge($activities, $allActivities[$id]);
			}
		}
		// TODO: Sort them
		return new ResponseItem(null, null, RestfulCollection::createFromEntry($activities));
	}

	public function createActivity($userId, $activity, SecurityToken $token)
	{
		$db = $this->getDb();
		$activitiesTable = $this->getAllActivities();
		$activity['appId'] = $token->getAppId();
		array_push($activitiesTable[$userId->getUserId($token)], $activity);
		$db[self::$ACTIVITIES_TABLE] = $activitiesTable;
		$this->saveDb($db);
		return new ResponseItem(null, null, array());
	}

	public function getActivity($userId, $groupId, $activityId, $first, $max, SecurityToken $token)
	{
		$activities = $this->getActivities($userId, $groupId, null, null, $token);
		$activities = $activities->getResponse();
		if ($activities instanceof RestfulCollection) {
			$activities = $activities->getEntry();
			foreach ($activities as $activity) {
				if ($activity['id'] == $activityId) {
					return new ResponseItem(null, null, $activity);
				}
			}
		}
		return new ResponseItem(NOT_FOUND, "Activity not found", null);
	}

	public function createMessage($userId, $message, SecurityToken $token)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "Not implemented", null);
	}

	/**
	 * Determines whether the input is a valid key.
	 *
	 * @param key the key to validate.
	 * @return true if the key is a valid appdata key, false otherwise.
	 */
	public static function isValidKey($key)
	{
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

	private function comparator($person, $person1)
	{
		$name = $person['name']['unstructured'];
		$name1 = $person1['name']['unstructured'];
		if ($name == $name1) {
			return 0;
		}
		return ($name < $name1) ? - 1 : 1;
	}
}
