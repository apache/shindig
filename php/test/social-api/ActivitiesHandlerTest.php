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
 * Implementation tests based on ActivitiesHandler
 */
class ActivitiesHandlerTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var ActivitiesHandler
	 */
	private $ActivitiesHandler;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->clearFileCache();
		$this->ActivitiesHandler = new ActivitiesHandler();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->ActivitiesHandler = null;
		$this->clearFileCache();
		parent::tearDown();
	}

	/**
	 * Tests ActivitiesHandler->handleDelete()
	 */
	public function testHandleDelete()
	{
		$request = array();
		$request['url'] = '/activities/@viewer/@self?appId=@app&networkDistance=';
		$request['method'] = 'GET';
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->ActivitiesHandler->handleDelete($requestItem);
		$this->assertEquals(BAD_REQUEST, $response->getError());
		$this->assertEquals('You can\'t delete activities. ', $response->getErrorMessage());
	}

	/**
	 * Tests ActivitiesHandler->handleGet()
	 */
	public function testHandleGet()
	{
		$request = array();
		$request['url'] = '/activities/@viewer/@self?appId=@app&networkDistance=';
		$request['method'] = 'GET';
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->ActivitiesHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('1', $response->getTotalResults());
		$this->assertEquals('1', $entry[0]['id']);
		$this->assertEquals('john.doe', $entry[0]['userId']);
		$this->assertEquals('yellow', $entry[0]['title']);
		$this->assertEquals('what a color!', $entry[0]['body']);
	}

	/**
	 * Tests ActivitiesHandler->handlePost()
	 */
	public function testHandlePost()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		
		//Create activity
		$request = array();
		$request['url'] = '/activities/@viewer/@self/@app?networkDistance=';
		$request['method'] = 'POST';
		$request['postData'] = array();
		$request['postData']['id'] = '2';
		$request['postData']['appId'] = '1';
		$request['postData']['userId'] = 'john.doe';
		$request['postData']['title'] = 'TestPost';
		$request['postData']['body'] = 'TestBody';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$this->ActivitiesHandler->handlePost($requestItem);
		
		//Validate generated activity
		$request = array();
		$request['url'] = '/activities/@viewer/@self/@app';
		$request['method'] = 'GET';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->ActivitiesHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('2', $response->getTotalResults());
		//First Entry
		$this->assertEquals('1', $entry[0]['id']);
		$this->assertEquals('john.doe', $entry[0]['userId']);
		$this->assertEquals('yellow', $entry[0]['title']);
		$this->assertEquals('what a color!', $entry[0]['body']);
		//Second Entry
		$this->assertEquals('2', $entry[1]['id']);
		$this->assertEquals('john.doe', $entry[1]['userId']);
		$this->assertEquals('TestPost', $entry[1]['title']);
		$this->assertEquals('TestBody', $entry[1]['body']);
	}

	/**
	 * Tests ActivitiesHandler->handlePut()
	 */
	public function testHandlePut()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		
		//Create activity
		$request = array();
		$request['url'] = '/activities/@viewer/@self/@app?networkDistance=';
		$request['method'] = 'POST';
		$request['postData'] = array();
		$request['postData']['id'] = '3';
		$request['postData']['appId'] = '1';
		$request['postData']['userId'] = 'john.doe';
		$request['postData']['title'] = 'TestPost 3';
		$request['postData']['body'] = 'TestBody 3';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$this->ActivitiesHandler->handlePut($requestItem);
		
		//Validate generated activity
		$request = array();
		$request['url'] = '/activities/@viewer/@self/@app';
		$request['method'] = 'GET';
		$requestItem = new RestRequestItem();
		$requestItem->createRequestItemWithRequest($request, $token);
		$response = $this->ActivitiesHandler->handleGet($requestItem);
		$response = $response->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals('2', $response->getTotalResults());
		//First Entry
		$this->assertEquals('1', $entry[0]['id']);
		$this->assertEquals('john.doe', $entry[0]['userId']);
		$this->assertEquals('yellow', $entry[0]['title']);
		$this->assertEquals('what a color!', $entry[0]['body']);
		//Second Entry
		$this->assertEquals('3', $entry[1]['id']);
		$this->assertEquals('john.doe', $entry[1]['userId']);
		$this->assertEquals('TestPost 3', $entry[1]['title']);
		$this->assertEquals('TestBody 3', $entry[1]['body']);
	}

	private function clearFileCache()
	{
		unlink(sys_get_temp_dir() . "ShindigDb.json");
	}
}
