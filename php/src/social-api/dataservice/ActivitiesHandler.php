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

class ActivitiesHandler extends DataRequestHandler {
	private $service;
	
	private static $ACTIVITY_ID_PATH = "/activities/{userId}/{groupId}/{activityId}";

	public function __construct()
	{
		$service = Config::get('activity_service');
		$this->service = new $service();
	}

	public function handleDelete(RestRequestItem $requestItem)
	{
		return new ResponseItem(BAD_REQUEST, "You can't delete activities. ", null);
	}

	/**
	 * /activities/{userId}/{groupId}/{optionalActvityId}
	 *
	 * examples:
	 * /activities/john.doe/@self/1
	 * /activities/john.doe/@self
	 * /activities/john.doe/@friends
	 */
	public function handleGet(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$ACTIVITY_ID_PATH);
		$parameters = $requestItem->getParameters();
		$optionalActivityId = in_array('activityId', $parameters) ? $parameters['activityId'] : null;
		// TODO: Filter by fields
		// TODO: do we need to add pagination and sorting support?
		if ($optionalActivityId != null) {
			return $this->service->getActivity($requestItem->getUser(), $requestItem->getGroup(), $optionalActivityId, $requestItem->getStartIndex(), $requestItem->getCount(), $requestItem->getToken());
		}
		$ret = $this->service->getActivities($requestItem->getUser(), $requestItem->getGroup(), $requestItem->getStartIndex(), $requestItem->getCount(), $requestItem->getToken());
		if ($ret->getError()) {
			return new ResponseItem(null, null, array());
		}
	}

	/**
	 * /activities/{userId}/@self
	 *
	 * examples:
	 * /activities/@viewer/@self/@app
	 * /activities/john.doe/@self
	 * - postBody is an activity object
	 */
	public function handlePost(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$ACTIVITY_ID_PATH);
		return $this->service->createActivity($requestItem->getUser(), $requestItem->getPostData(), $requestItem->getToken());
	}

	/**
	 * /activities/{userId}/@self
	 *
	 * examples:
	 * /activities/john.doe/@self
	 * - postBody is an activity object
	 */
	public function handlePut(RestRequestItem $requestItem)
	{
		return $this->handlePost($requestItem);
	}
}
