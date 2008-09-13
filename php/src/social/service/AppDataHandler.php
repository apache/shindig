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
	private static $APP_DATA_PATH = "/people/{userId}/{groupId}/{appId}";
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
	 */
	public function handleDelete(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$APP_DATA_PATH);
		return $this->service->deletePersonData($requestItem->getUser(), $requestItem->getGroup(), $requestItem->getFields(), $requestItem->getAppId(), $requestItem->getToken());
	}

	/**
	 * /appdata/{userId}/{groupId}/{appId}
	 * - fields={field1, field2}
	 *
	 * examples:
	 * /appdata/john.doe/@friends/app?fields=count
	 * /appdata/john.doe/@self/app
	 */
	public function handleGet(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$APP_DATA_PATH);
		return $this->service->getPersonData($requestItem->getUser(), $requestItem->getGroup(), $requestItem->getFields(), $requestItem->getAppId(), $requestItem->getToken());
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
	public function handlePost(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$APP_DATA_PATH);
		// if no ?fields=foo,bar was specified, we try to guess them from the post data
		$postFields = array();
		if ($requestItem->getPostData() != null) {
			$data = $requestItem->getPostData();
			foreach ($data as $key => $val) {
				$postFields[] = $key;
			}
		}
		return $this->service->updatePersonData($requestItem->getUser(), $requestItem->getGroup(), $requestItem->getFieldsWithDefaultValue($postFields), $requestItem->getPostData(), $requestItem->getAppId(), $requestItem->getToken());
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
	public function handlePut(RestRequestItem $requestItem)
	{
		return $this->handlePost($requestItem);
	}
}