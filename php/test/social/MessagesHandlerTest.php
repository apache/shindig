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

/**
 * Implementation tests based on MessagesHandler
 */
class MessagesHandlerTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var MessagesHandler
	 */
	private $MessagesHandler;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->MessagesHandler = new MessagesHandler();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->MessagesHandler = null;
		parent::tearDown();
	}

	/**
	 * Tests MessagesHandler->handleDelete()
	 */
	public function testHandleDelete()
	{
		$response = $this->MessagesHandler->handleDelete(new RestRequestItem());
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("You can't delete messages", $response->getErrorMessage());
	}

	/**
	 * Tests MessagesHandler->handleGet()
	 */
	public function testHandleGet()
	{
		$response = $this->MessagesHandler->handleGet(new RestRequestItem());
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("You can't retrieve messages", $response->getErrorMessage());
	}

	/**
	 * Tests MessagesHandler->handlePost()
	 */
	public function testHandlePost()
	{
		$response = $this->MessagesHandler->handlePost(new RestRequestItem());
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("You can't edit messages", $response->getErrorMessage());
	}

	/**
	 * Tests MessagesHandler->handlePut()
	 */
	public function testHandlePut()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		//Create message
		$request = array();
		$request['url'] = '/messages/@viewer/outbox/1';
		$request['postData'] = 'message 1';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->MessagesHandler->handlePut($requestItem);
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("Not implemented", $response->getErrorMessage());
	}

}
?>