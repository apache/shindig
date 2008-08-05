<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * LinkSpec test case.
 */
class LinkSpecTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var LinkSpec
	 */
	private $LinkSpec;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->LinkSpec = new LinkSpec('rel', 'href', 'method');	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->LinkSpec = null;		
		parent::tearDown();
	}

	/**
	 * Tests LinkSpec->__construct()
	 */
	public function test__construct()
	{
		$this->LinkSpec = new LinkSpec('rel', 'href', 'method');	
	}

	/**
	 * Tests LinkSpec->getHref()
	 */
	public function testGetHref()
	{
		$this->assertEquals('href', $this->LinkSpec->getHref());	
	}

	/**
	 * Tests LinkSpec->getMethod()
	 */
	public function testGetMethod()
	{
		$this->assertEquals('method', $this->LinkSpec->getMethod());
	}

	/**
	 * Tests LinkSpec->getRel()
	 */
	public function testGetRel()
	{
		$this->assertEquals('rel', $this->LinkSpec->getRel());
	}
}
