<?php
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

class BasicAppDataService extends AppDataService {

	
	public function deletePersonData(UserId $userId, GroupId $groupId, $fields, $appId, SecurityToken $token)
	{
		foreach ($fields as $key) {
			if (! BasicAppDataService::isValidKey($key)) {
				return new ResponseItem(BAD_REQUEST, "The person app data key had invalid characters", null);
			}
		}
		switch($groupId->getType()) {
			case 'self':
				foreach ($fields as $key) {
					XmlStateFileFetcher::get()->deleteAppData($userId->getUserId($token), $key);	
				}
				break;
			default:
				return new ResponseItem(NOT_IMPLEMENTED, "We don't support deleting data in batches yet", null);		
				break;
		}
		return new ResponseItem(null, null, array());
	}
	
	public function getPersonData(UserId $userId, GroupId $groupId, $fields, $appId, SecurityToken $token)
	{
		$allData = XmlStateFileFetcher::get()->getAppData();
		$data = array();
		$ids = array();
		switch($groupId->getType()) {
			case 'self':
				$ids[] = $userId->getUserId($token);
				break;
			case 'all':
			case 'friends':
				$friendIds = XmlStateFileFetcher::get()->getFriendIds();
				if (is_array($friendIds) && count($friendIds) && isset($friendIds[$userId->getUserId($token)])) {
					$ids = $friendIds[$userId->getUserId($token)];
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
					if (in_array($key, $fields) || $fields[0] == "*") {
						$personData[$key] = $allPersonData[$key];
					}
				}
				$data[$id] = $personData;
			}
		}
		return new ResponseItem(null, null, RestFulCollection::createFromEntry($data));
	}

	public function updatePersonData(UserID $userId, GroupId $groupId, $fields, $values, $appId, SecurityToken $token)
	{
		foreach ($fields as $key) {
			if (! BasicAppDataService::isValidKey($key)) {
				return new ResponseItem(BAD_REQUEST, "The person app data key had invalid characters", null);
			}			
		}
		switch($groupId->getType()) {
			case 'self':
				foreach ($fields as $key) {
					$value = isset($values[$key]) ? @$values[$key] : null;
					XmlStateFileFetcher::get()->setAppData($userId->getUserId($token), $key, $value);	
				}
				break;
			default:
				return new ResponseItem(NOT_IMPLEMENTED, "We don't support updating data in batches yet", null);		
				break;
		}
		return new ResponseItem(null, null, array());
	}

	/**
	 * Determines whether the input is a valid key. Valid keys match the regular
	 * expression [\w\-\.]+.
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
}
