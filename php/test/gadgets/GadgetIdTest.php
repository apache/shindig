<?php

require_once '../../src/gadgets/GadgetId.php';

require_once '../PHPUnit/Framework/TestCase.php';

/**
 * GadgetId test case.
 */
class GadgetIdTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var GadgetId
	 */
	private $GadgetId;
	private $uri;
	private $moduleId;
	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp() {
		parent::setUp ();
		
		// TODO Auto-generated GadgetIdTest::setUp()
		
		
		$this->GadgetId = new GadgetId($this->uri,$this->moduleId);
	
	}
	
	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown() {
		// TODO Auto-generated GadgetIdTest::tearDown()
		

		$this->GadgetId = null;
		
		parent::tearDown ();
	}
	
	/**
	 * Constructs the test case.
	 */
	public function __construct() {
		// TODO Auto-generated constructor
	}
	
	
	/**
	 * Tests GadgetId->getKey()
	 */
	public function testGetKey() {
		// TODO Auto-generated GadgetIdTest->testGetKey()
		
		$this->assertEquals($this->uri,$this->GadgetId->getKey());
	}
	
	/**
	 * Tests GadgetId->getModuleId()
	 */
	public function testGetModuleId() {
		// TODO Auto-generated GadgetIdTest->testGetModuleId()
		
		$this->GadgetId->getModuleId();
	
	}
	
	/**
	 * Tests GadgetId->getURI()
	 */
	public function testGetURI() {
		// TODO Auto-generated GadgetIdTest->testGetURI()
		$this->assertEquals($this->uri,$this->GadgetId->getURI());
	
	}

}

