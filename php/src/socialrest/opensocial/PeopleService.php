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

abstract class PeopleService {
	static public $sortOrder = array('topFriends', 'name');
	static public $filterType = array('all', 'hasApp', 'topFriends');

	/**
	 * Returns a Person object for person with $id or false on not found
	 *
	 * @param container specific id $id
	 * @param profileDetails the details to return
	 * @param security token $token
	 */
	abstract public function getPerson($userId, $groupId, $profileDetails, $token);

	/**
	 * Returns a list of people that correspond to the passed in person ids.
	 * @param ids The ids of the people to fetch.
	 * @param sortOrder How to sort the people
	 * @param filter How the people should be filtered.
	 * @param first The index of the first person to fetch.
	 * @param profileDetails the details to return
	 * @param max The max number of people to fetch.
	 * @return a list of people.
	 */
	abstract public function getPeople($userId, $groupId, $sortOrder, $filter, $first, $max, $profileDetails, $token);
}
