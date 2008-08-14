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
 * Implementation tests based on AppDataHandler
 */
class AppDataHandlerTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var AppDataHandler
	 */
	private $AppDataHandler;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->clearFileCache();
		$this->AppDataHandler = new AppDataHandler();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->AppDataHandler = null;
		$this->clearFileCache();
		parent::tearDown();
	}

	/**
	 * Tests ActivitiesHandler->handleDelete()
	 */
	public function testHandleDelete()
	{
		$request = array();
		$request['url'] = '/appdata/@viewer/@app?fields=count';
		$request['method'] = 'POST';
		$request['postData'] = array();
		$request['postData']['data'] = 'TestHandleDelete';
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->AppDataHandler->handleDelete($requestItem);
		$this->assertEquals(NOT_IMPLEMENTED, $response->getError());
		$this->assertEquals("We don't support deleting data in batches yet", $response->getErrorMessage());
	}

	/**
	 * Tests ActivitiesHandler->handleGet()
	 */
	public function testHandleGet()
	{
		return;
		$request = array();
		$request['url'] = '/appdata/@viewer/@self/@app?networkDistance=&fields=count';
		$request['method'] = 'GET';
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->AppDataHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('0', $entry['john.doe']['count']);
	}

	/**
	 * Tests ActivitiesHandler->handlePost()
	 */
	public function testHandlePost()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		//Create data
		$request = array();
		$request['url'] = '/appdata/@viewer/@self/@app?fields=count';
		$request['method'] = 'POST';
		$request['postData'] = array();
		$request['postData']['count'] = 'TestHandlePost';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$this->AppDataHandler->handlePost($requestItem);
		
		//Validate generated data
		$request = array();
		$request['url'] = '/appdata/@viewer/@self/@app?networkDistance=&fields=count';
		$request['method'] = 'GET';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->AppDataHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('TestHandlePost', $entry['john.doe']['count']);
	}

	/**
	 * Tests ActivitiesHandler->handlePut()
	 */
	public function testHandlePut()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		//Create data
		$request = array();
		$request['url'] = '/appdata/@viewer/@self/@app?fields=count';
		$request['method'] = 'POST';
		$request['postData'] = array();
		$request['postData']['count'] = 'TestHandlePut';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$this->AppDataHandler->handlePut($requestItem);
		
		//Validate generated data
		$request = array();
		$request['url'] = '/appdata/@viewer/@self/@app?networkDistance=&fields=count';
		$request['method'] = 'GET';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->AppDataHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('TestHandlePut', $entry['john.doe']['count']);
	}

	private function clearFileCache()
	{
		unlink(sys_get_temp_dir() . "ShindigDb.json");
	}
}
