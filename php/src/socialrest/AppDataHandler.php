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

class AppDataHandler extends DataRequestHandler {
	private $service;

	public function __construct()
	{
		$service = Config::get('app_data_service');
		$this->service = new $service();
	}

	/**
	 * /people/{userId}/{groupId}/{appId}
	 * - fields={field1, field2}
	 *
	 * examples:
	 * /appdata/john.doe/@friends/app?fields=count
	 * /appdata/john.doe/@self/app
	 *
	 * The post data should be a regular json object. All of the fields vars will
	 * be pulled from the values and set on the person object. If there are no
	 * fields vars then all of the data will be overridden.
	 */
	public function handleDelete($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		$appId = $this->getAppId($params[3], $token);
		$fields = isset($_GET['fields']) ? explode(',', $_GET['fields']) : null;
		return $this->service->deletePersonData($userId, $groupId, $fields, $appId, $token);		
	}

	/**
	 * /appdata/{userId}/{groupId}/{appId}
	 * - fields={field1, field2}
	 *
	 * examples:
	 * /appdata/john.doe/@friends/app?fields=count
	 * /appdata/john.doe/@self/app
	 */
	public function handleGet($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		$appId = $this->getAppId($params[3], $token);
		$fields = isset($_GET['fields']) ? explode(',', $_GET['fields']) : null;
		return $this->service->getPersonData($userId, $groupId, $fields, $appId, $token);
	}

	/**
	 * /people/{userId}/{groupId}/{appId}
	 * - fields={field1, field2}
	 *
	 * examples:
	 * /appdata/john.doe/@friends/app?fields=count
	 * /appdata/john.doe/@self/app
	 *
	 * The post data should be a regular json object. All of the fields vars will
	 * be pulled from the values and set on the person object. If there are no
	 * fields vars then all of the data will be overridden.
	 */
	public function handlePost($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		$appId = $this->getAppId($params[3], $token);
		$fields = isset($_GET['fields']) ? explode(',', $_GET['fields']) : null;
		if (!isset($GLOBALS['HTTP_RAW_POST_DATA'])) {
			throw new Exception("Empty raw post data");
		}
		$jsonActivity = $GLOBALS['HTTP_RAW_POST_DATA'];
		if (get_magic_quotes_gpc()) {
			$jsonActivity = stripslashes($jsonActivity);
		}
		$values = $this->convertToObject($jsonActivity);
		return $this->service->updatePersonData($userId, $groupId, $fields, $values, $appId, $token);
	}

	/**
	 * /people/{userId}/{groupId}/{appId}
	 * - fields={field1, field2}
	 *
	 * examples:
	 * /appdata/john.doe/@friends/app?fields=count
	 * /appdata/john.doe/@self/app
	 *
	 * The post data should be a regular json object. All of the fields vars will
	 * be pulled from the values and set on the person object. If there are no
	 * fields vars then all of the data will be overridden.
	 */
	public function handlePut($params, $token)
	{
		return $this->handlePost($params, $token);
	}
}