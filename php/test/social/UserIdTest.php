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
 * UserId test case.
 */
class UserIdTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var UserId
	 */
	private $UserId;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->UserId = new UserId(UserId::$types[0], 1);
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->UserId = null;
		parent::tearDown();
	}

	/**
	 * Constructs the test case.
	 */
	public function __construct()
	{
	}

	/**
	 * Tests UserId->__construct()
	 */
	public function test__construct()
	{
		$this->UserId->__construct(UserId::$types[0], 1); //viewer
	}

	/**
	 * Tests UserId->getUserId()
	 */
	public function testGetUserId()
	{
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$userId = $this->UserId->getUserId($token);
		$this->assertEquals('john.doe', $userId);
		$this->UserId->__construct(UserId::$types[1], 1); //owner
		$userId = $this->UserId->getUserId($token);
		$this->assertEquals('john.doe', $userId);
		$this->UserId->__construct(UserId::$types[2], 1); //userId
		$userId = $this->UserId->getUserId($token);
		$this->assertEquals('1', $userId);
	}

	/**
	 * Tests UserId->getType()
	 */
	public function testGetType()
	{
		$this->assertEquals('viewer', $this->UserId->getType());
	}

	/**
	 * Tests UserId->fromJson()
	 */
	public function testFromJson()
	{
		$json = 'jsonid';
		$fromJson = $this->UserId->fromJson($json);
		$this->assertEquals('userId', $fromJson->getType());
	}
}
