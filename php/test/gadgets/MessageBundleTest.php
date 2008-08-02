<?php

require_once 'src/gadgets/MessageBundle.php';

require_once 'PHPUnit/Framework/TestCase.php';

/**
 * MessageBundle test case.
 */
class MessageBundleTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var MessageBundle
	 */
	private $MessageBundle;
	
	/**
	 * @var Message
	 */
	private $Messages = array('Dummie Message', 'Hello World');

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->MessageBundle = new MessageBundle($this->Messages);
	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->MessageBundle = null;
		parent::tearDown();
	}

	/**
	 * Tests MessageBundle->getMessages()
	 */
	public function testGetMessages()
	{
		$this->assertEquals($this->Messages, $this->MessageBundle->getMessages());
	
	}

}

