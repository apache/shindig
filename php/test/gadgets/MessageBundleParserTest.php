<?php

require_once 'src/gadgets/MessageBundle.php';
require_once 'src/gadgets/MessageBundleParser.php';

require_once 'PHPUnit/Framework/TestCase.php';

/**
 * MessageBundleParser test case.
 */
class MessageBundleParserTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var MessageBundleParser
	 */
	private $MessageBundleParser;
	private $MessageBundle;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		
		$this->MessageBundleParser = new MessageBundleParser(/* parameters */);
	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		
		$this->MessageBundleParser = null;
		$this->MessageBundle = null;
		
		parent::tearDown();
	}

	/**
	 * Tests MessageBundleParser->parse()
	 */
	public function testParse()
	{
		
		$xml = '<?xml version="1.0" encoding="UTF-8" ?>
<doc>
	<msg name="name1">Message 1</msg>
	<msg name="name2">Message 2</msg>
	<msg name="name3">Message 3</msg>
	<msg name="name4">Message 4</msg>
</doc>';
		
		$this->MessageBundle = $this->MessageBundleParser->parse($xml);
		
		$this->assertTrue($this->MessageBundle instanceof MessageBundle);
	
	}

}

