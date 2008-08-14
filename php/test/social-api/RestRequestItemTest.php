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
 * RestRequestItem test case.
 */
class RestRequestItemTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var RestRequestItem
	 */
	private $RestRequestItem;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->RestRequestItem = new RestRequestItem();
		$url = '/people/@viewer/@self?fields=age,name,gender,profileUrl,thumbnailUrl,' . 'status,id&startIndex=0&count=40&orderBy=name&filterBy=all&networkDistance=1';
		$request = array();
		$request['url'] = $url;
		$request['method'] = 'GET';
		$request['postData'] = array();
		$request['postData']['data'] = 'DataTest';
		$this->RestRequestItem->createRequestItemWithRequest($request, $this->getToken());
		
		//(the getters of the parsedTemplate will be tested on other methods)
		$this->RestRequestItem->parseUrlWithTemplate("/people/{userId}/{groupId}/{personId}");
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->RestRequestItem = null;
		parent::tearDown();
	}

	/**
	 * Constructs the test case.
	 */
	public function __construct()
	{
	}
	
	private $token;

	private function getToken()
	{
		if (is_null($this->token)) {
			$this->token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		}
		return $this->token;
	}

	/**
	 * Tests RestRequestItem->createRequestItemWithRequest()
	 */
	public function testCreateRequestItemWithRequest()
	{
		$url = '/people/@viewer/@self?fields=age,name,gender,profileUrl,thumbnailUrl,' . 'status,id&startIndex=0&count=40&orderBy=name&filterBy=all&networkDistance=1';
		$request = array();
		$request['url'] = $url;
		$request['method'] = 'GET';
		$request['postData'] = array();
		$request['postData']['data'] = 'DataTest';
		$this->RestRequestItem->createRequestItemWithRequest($request, $this->getToken());
		
		//Without the parseTemplate
		$this->assertEquals($url, $this->RestRequestItem->getUrl());
		$this->assertEquals('0', $this->RestRequestItem->getAppId());
		$this->assertEquals(false, $this->RestRequestItem->getUser());
		$this->assertEquals(false, $this->RestRequestItem->getGroup());
		$this->assertEquals('0', $this->RestRequestItem->getStartIndex());
		$this->assertEquals('20', $this->RestRequestItem->getCount());
		$this->assertEquals(PeopleOptions::$sortOrder, $this->RestRequestItem->getOrderBy());
		$this->assertEquals(PeopleOptions::$filterType, $this->RestRequestItem->getFilterBy());
		$this->assertEquals(false, $this->RestRequestItem->getNetworkDistance());
	}

	/**
	 * Tests RestRequestItem->createRequestItem()
	 */
	public function testCreateRequestItem()
	{
		$url = '/people/@viewer/@self';
		$method = 'GET';
		$postData = array();
		$postData['data'] = 'DataTest';
		$params = array();
		$params['param1'] = 'DataParam1';
		$this->RestRequestItem->createRequestItem($url, $this->getToken(), $method, $params, $postData);
		$this->assertEquals($url, $this->RestRequestItem->getUrl());
		$this->assertEquals($this->token, $this->RestRequestItem->getToken());
		$this->assertEquals($method, $this->RestRequestItem->getMethod());
		$this->assertEquals($params, $this->RestRequestItem->getParameters());
		$this->assertEquals($postData, $this->RestRequestItem->getPostData());
	}

	/**
	 * Tests RestRequestItem->getParameters()
	 */
	public function testGetParameters()
	{
		$parameters = $this->RestRequestItem->getParameters();
		$this->assertEquals('people', $parameters[0]);
		$this->assertEquals('@viewer', $parameters[1]);
		$this->assertEquals('@self', $parameters[2]);
	}

	/**
	 * Tests RestRequestItem->getPostData()
	 */
	public function testGetPostData()
	{
		$postData = $this->RestRequestItem->getPostData();
		$this->assertEquals('DataTest', $postData['data']);
	}

	/**
	 * Tests RestRequestItem->getToken()
	 */
	public function testGetToken()
	{
		$this->assertEquals($this->getToken(), $this->RestRequestItem->getToken());
	}

	/**
	 * Tests RestRequestItem->getMethod()
	 */
	public function testGetMethod()
	{
		$this->assertEquals('GET', $this->RestRequestItem->getMethod());
	}

	/**
	 * Tests RestRequestItem->getUrl()
	 */
	public function testGetUrl()
	{
		$this->assertEquals("/people/@viewer/@self", $this->RestRequestItem->getUrl());
	}

	/**
	 * Tests RestRequestItem->getAppId()
	 */
	public function testGetAppId()
	{
		$this->assertEquals('0', $this->RestRequestItem->getAppId());
	}

	/**
	 * Tests RestRequestItem->getUser()
	 */
	public function testGetUser()
	{
		$userId = $this->RestRequestItem->getUser();
		$this->assertEquals('viewer', $userId->getType());
	}

	/**
	 * Tests RestRequestItem->getGroup()
	 */
	public function testGetGroup()
	{
		$groupId = $this->RestRequestItem->getGroup();
		$this->assertEquals('self', $groupId->getType());
	}

	/**
	 * Tests RestRequestItem->getStartIndex()
	 */
	public function testGetStartIndex()
	{
		$this->assertEquals('0', $this->RestRequestItem->getStartIndex());
	}

	/**
	 * Tests RestRequestItem->getCount()
	 */
	public function testGetCount()
	{
		$this->assertEquals('40', $this->RestRequestItem->getCount());
	}

	/**
	 * Tests RestRequestItem->getOrderBy()
	 */
	public function testGetOrderBy()
	{
		$this->assertEquals('name', $this->RestRequestItem->getOrderBy());
	}

	/**
	 * Tests RestRequestItem->getFilterBy()
	 */
	public function testGetFilterBy()
	{
		$this->assertEquals('all', $this->RestRequestItem->getFilterBy());
	}

	/**
	 * Tests RestRequestItem->getNetworkDistance()
	 */
	public function testGetNetworkDistance()
	{
		$this->assertEquals('1', $this->RestRequestItem->getNetworkDistance());
	}
}
