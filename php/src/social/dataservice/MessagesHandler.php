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

class MessagesHandler extends DataRequestHandler {
	
	private static $MESSAGES_PATH = "/messages/{userId}/outbox/{msgId}";
	private $service;

	public function __construct()
	{
		$service = Config::get('messages_service');
		$this->service = new $service();
	}

	public function handleDelete(RestRequestItem $requestItem)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "You can't delete messages", null);
	}

	public function handleGet(RestRequestItem $requestItem)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "You can't retrieve messages", null);
	}

	public function handlePost(RestRequestItem $requestItem)
	{
		return new ResponseItem(NOT_IMPLEMENTED, "You can't edit messages", null);
	}

	/**
	 * /messages/{groupId}/outbox/{msgId}
	 *
	 * @param RestRequestItem $requestItem
	 * @return responseItem
	 */
	public function handlePut(RestRequestItem $requestItem)
	{
		$requestItem->parseUrlWithTemplate(self::$MESSAGES_PATH);
		return $this->service->createMessage($requestItem->getUser(), $requestItem->getPostData(), $requestItem->getToken());
	}
}
