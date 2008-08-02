<?php

require_once 'src/gadgets/JsLibrary.php';

require_once 'PHPUnit/Framework/TestCase.php';

/**
 * JsLibrary test case.
 */
class JsLibraryTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var JsLibrary
	 */
	private $JsLibrary;
	
	/**
	 * @var type
	 */
	private $type = 'URL';
	
	/**
	 * @var content
	 */
	private $content = '';
	
	/**
	 * @var featureName
	 */
	private $featureName = '';

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->JsLibrary = new JsLibrary($this->type, $this->content, $this->featureName);
	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->JsLibrary = null;
		parent::tearDown();
	}

	/**
	 * Tests JsLibrary->getContent()
	 */
	public function testGetContent()
	{
		$content = trim($this->JsLibrary->getContent());
		$this->assertEquals($this->content, $content);
	}

	/**
	 * Tests JsLibrary->toString()
	 */
	public function testToString()
	{
		$output = $this->JsLibrary->toString();
		$this->assertEquals("<script src=\"" . $this->JsLibrary->getContent() . "\"></script>", $output);
	
	}

}

