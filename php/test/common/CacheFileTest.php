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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * CacheFile test case.
 */
class CacheFileTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var CacheFile
	 */
	private $CacheFile;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->CacheFile = new CacheFile(/* parameters */);
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->CacheFile = null;
		parent::tearDown();
	}

	/**
	 * Tests CacheFile->delete()
	 */
	public function testDelete()
	{
		$cache = new CacheFile();
		$cache->set("test", "testing");
		$cache->delete("test");
		$this->assertEquals("", $cache->get("test"));
	}

	/**
	 * Tests CacheFile->get()
	 */
	public function testGet()
	{	
		$this->CacheFile->set("test", "testing");	
		$this->assertEquals("testing", $this->CacheFile->get("test"));
	}

	/**
	 * Tests CacheFile->set()
	 */
	public function testSet()
	{
		$this->CacheFile->set("test", "testing");	
		$this->assertEquals("testing", $this->CacheFile->get("test"));
	}

}