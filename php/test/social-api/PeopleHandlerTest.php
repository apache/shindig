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
 * Implementation tests based on PeopleHandler
 */
class PeopleHandlerTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var PeopleHandler
	 */
	private $PeopleHandler;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->PeopleHandler = new PeopleHandler();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->PeopleHandler = null;
		parent::tearDown();
	}

	/**
	 * Tests PeopleHandler->handleDelete()
	 */
	public function testHandleDelete()
	{
		$response = $this->PeopleHandler->handleDelete(new RestRequestItem());
		$this->assertEquals(BAD_REQUEST, $response->getError());
		$this->assertEquals("You can't delete people.", $response->getErrorMessage());
	}

	/**
	 * Tests PeopleHandler->handleGet()
	 */
	public function testHandleGet()
	{
		$request = array();
		$request['url'] = '/people/@viewer/@self';
		$request['method'] = 'GET';
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->PeopleHandler->handleGet($requestItem);
		$person = $response->getResponse();
		$this->assertEquals('john.doe', $person['id']);
		$this->assertEquals('MALE', $person['gender']['key']);
		$this->assertEquals('Male', $person['gender']['displayValue']);
		$this->assertEquals('Doe', $person['name']['familyName']);
		$this->assertEquals('John', $person['name']['givenName']);
		$this->assertEquals('John Doe', $person['name']['unstructured']);
	}

	/**
	 * Tests PeopleHandler->handlePost()
	 */
	public function testHandlePost()
	{
		$response = $this->PeopleHandler->handlePost(new RestRequestItem());
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("You can't edit people right now.", $response->getErrorMessage());
	}

	/**
	 * Tests PeopleHandler->handlePut()
	 */
	public function testHandlePut()
	{
		$response = $this->PeopleHandler->handlePut(new RestRequestItem());
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("You can't add people right now.", $response->getErrorMessage());
	}
}
