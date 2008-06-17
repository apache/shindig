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

class ActivityHandler extends DataRequestHandler {
	private $service;

	public function __construct()
	{
		$service = Config::get('activity_service');
		$this->service = new $service();
	}

	public function handleDelete($params, $token)
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
	public function handleGet($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		$optionalActivityId = null;
		if (count($params) > 3) {
			$optionalActivityId = $params[3];
		}
		// TODO: Filter by fields
		// TODO: do we need to add pagination and sorting support?
		if ($optionalActivityId != null) {
			return $this->service->getActivity($userId, $groupId, $optionalActivityId, $token);
		}
		return $this->service->getActivities($userId, $groupId, $token);
	}

	/**
	 * /activities/{userId}/@self
	 *
	 * examples:
	 * /activities/john.doe/@self
	 * - postBody is an activity object
	 */
	public function handlePost($params, $token)
	{
		$userId = UserId::fromJson($params[1]);
		$groupId = GroupId::fromJson($params[2]);
		// TODO: Should we pass the groupId through to the service?
		$jsonActivity = isset($_POST['entry']) ? $_POST['entry'] : (isset($_GET['entry']) ? $_GET['entry'] : null);
		if (get_magic_quotes_gpc()) {
			$jsonActivity = stripslashes($jsonActivity);
		}
		$activity = $this->convertToObject($jsonActivity);
    	return $this->service->createActivity($userId, $activity, $token);
	}

	/**
	 * /activities/{userId}/@self
	 *
	 * examples:
	 * /activities/john.doe/@self
	 * - postBody is an activity object
	 */
	public function handlePut($params, $token)
	{
		return $this->handlePost($token);
	}
}