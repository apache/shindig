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
 * GadgetServer test case.
 */
class GadgetServerTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var GadgetServer
	 */
	private $GadgetServer;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->GadgetServer = new GadgetServer();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->GadgetServer = null;
		parent::tearDown();
	}

	/**
	 * Tests GadgetServer->processGadget()
	 */
	public function testProcessGadget()
	{
		
		$GadgetContext = new GadgetContext('GADGET');
		$GadgetContext->setUrl('http://' . $_SERVER["HTTP_HOST"] . Config::get('web_prefix') . '/test/gadgets/example.xml');
		
		$gadget = $this->GadgetServer->processGadget($GadgetContext);
		
		$this->assertTrue($gadget instanceof Gadget);
	
	}

}
