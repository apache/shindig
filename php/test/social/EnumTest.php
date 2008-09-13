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
 * DummieEnum Class
 */
class DummieEnum extends Enum {
	public $values = array('First' => "1", 'Second' => "2", 'last' => "LAST");
}

/**
 * Enum test case.
 */
class EnumTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var Enum
	 */
	private $Enum;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		parent::tearDown();
	}

	/**
	 * Tests Enum->getDisplayValue()
	 */
	public function testGetDisplayValue()
	{
		$this->Enum = new DummieEnum('last');
		$this->assertEquals('LAST', $this->Enum->getDisplayValue());
	}

	/**
	 * Tests Enum->setDisplayValue()
	 */
	public function testSetDisplayValue()
	{
		$this->Enum = new DummieEnum('First');
		$this->Enum->setDisplayValue('0');
		$this->assertEquals('0', $this->Enum->displayValue);
	}

	/**
	 * Tests Enum->__construct()
	 */
	public function test__constructException()
	{
		$this->setExpectedException('Exception');
		$this->Enum = new DummieEnum('NON_EXISTING_KEY');
	}
}
