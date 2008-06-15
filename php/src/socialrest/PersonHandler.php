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

class PersonHandler extends DataRequestHandler {
	private $service;

	public function __construct()
	{
		$service = Config::get('people_service');
		$this->service = new $service();
	}

	/**
	 * /people/{userId}/{groupId}/{optionalPersonId}
	 *
	 * examples:
	 * /people/john.doe/@all
	 * /people/john.doe/@friends
	 * /people/john.doe/@self
	 */
	public function handleGet($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		$optionalPersonId = false;
		if (count($params) > 3) {
			$optionalPersonId = $params[3];
		}
		$fields = !empty($_GET['fields']) ? explode(',', $_GET['fields']) : null;
		if ($optionalPersonId || $groupId->getType() == 'self') {
			//FIXME same logic as the java code here, but doesn't seem to do much with the optionalPersonId which seems odd
			return $this->service->getPerson($userId, $groupId, $fields, $token);
		}
		$sort = !empty($_GET['orderBy']) && in_array($_GET['orderBy'], PeopleService::$sortOrder) ? $_GET['orderBy'] : '';
		$filter = !empty($_GET['filterBy']) && in_array($_GET['filterBy'], PeopleService::$filterType) ? $_GET['filterBy'] : '';
		$first = !empty($_GET['startIndex']) && is_numeric($_GET['startIndex']) ? $_GET['startIndex'] : 0;
		$max = !empty($_GET['count']) && is_numeric($_GET['count']) ? $_GET['count'] : 0;
		return $this->service->getPeople($userId, $groupId, $sort, $filter, $first, $max, $fields, $token);
	}

	public function handleDelete($params, $token)
	{
	    return new ResponseItem(BAD_REQUEST, "You can't delete people.", null);
	}
	
	public function handlePost($params, $token)
	{
	    return new ResponseItem(NOT_IMPLEMENTED, "You can't edit people right now.", null);
	}

	public function handlePut($params, $token)
	{
	    return new ResponseItem(NOT_IMPLEMENTED, "You can't add people right now.", null); 
	}
}