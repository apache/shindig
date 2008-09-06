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

class PeopleHandler extends DataRequestHandler {
	private $service;
	private static $PEOPLE_PATH = "/people/{userId}/{groupId}/{personId}";
	protected static $DEFAULT_PERSON_FIELDS = array("id" => 1, "name" => 1, "thumbnailUrl" => 1);

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
	public function handleGet(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$PEOPLE_PATH);
		$parameters = $requestItem->getParameters();
		$optionalPersonId = isset($parameters['personId']) ? $parameters['personId'] : null;
		$fields = $requestItem->getFieldsWithDefaultValue(self::$DEFAULT_PERSON_FIELDS);
		if ($optionalPersonId) {
			return $this->service->getPerson($requestItem->getUser(), $optionalPersonId, $fields, $requestItem->getToken());
		} else 
			if (is_object($requestItem->getGroup()) && $requestItem->getGroup()->getType() == 'self') {
				return $this->service->getPerson($requestItem->getUser(), $requestItem->getGroup(), $fields, $requestItem->getToken());
			}
		$startIndex = $requestItem->getStartIndex();
		$count = $requestItem->getCount();
		$networkDistance = $requestItem->getNetworkDistance();
		if ((! empty($startIndex) && ! is_numeric($startIndex)) || (! empty($count) && ! is_numeric($count)) || (! empty($networkDistance) && ! is_numeric($networkDistance))) {
			return new ResponseItem(BAD_REQUEST, "Invalid options specified", null);
		} else {
			$options = new CollectionOptions();
			$options->setSortBy($requestItem->getSortBy());
			$options->setSortOrder($requestItem->getSortOrder());
			$options->setFilterBy($requestItem->getFilterBy());
			$options->setFilterOperation($requestItem->getFilterOperation());
			$options->setFilterValue($requestItem->getFilterValue());
			$options->setUpdatedSince($requestItem->getUpdatedSince());
			$options->setStartIndex($startIndex);
			$options->setCount($count);
			$options->setNetworkDistance($networkDistance);
			return $this->service->getPeople($requestItem->getUser(), $requestItem->getGroup(), $options, $fields, $requestItem->getToken());
		}
	}

	public function handleDelete(RestRequestItem $requestItem)
	{
		return new ResponseItem(BAD_REQUEST, "You can't delete people.", null);
	}

	public function handlePost(RestRequestItem $requestItem)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "You can't edit people right now.", null);
	}

	public function handlePut(RestRequestItem $requestItem)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "You can't add people right now.", null);
	}
}
