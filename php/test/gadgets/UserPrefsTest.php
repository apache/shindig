<?php

require_once 'src/gadgets/UserPrefs.php';

require_once 'PHPUnit/Framework/TestCase.php';

/**
 * UserPrefs test case.
 */
class UserPrefsTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var UserPrefs
	 */
	private $UserPrefs;
	
	/**
	 * @var UserPrefsArrays
	 */
	private $UserPrefsArrays = array('Test1' => 'value for test1', 'Test2' => 'value for test2');

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->UserPrefs = new UserPrefs($this->UserPrefsArrays);
	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->UserPrefs = null;
		
		parent::tearDown();
	}

	/**
	 * Tests UserPrefs->getPref()
	 */
	public function testGetPref()
	{
		$this->assertEquals($this->UserPrefsArrays['Test1'], $this->UserPrefs->getPref('Test1'));
	
	}

	/**
	 * Tests UserPrefs->getPrefs()
	 */
	public function testGetPrefs()
	{
		$this->assertEquals($this->UserPrefsArrays, $this->UserPrefs->getPrefs());
	
	}

	/**
	 * Tests UserPrefs->getPrefs()
	 */
	public function testGetPrefsReturn()
	{
		$key = 'Test1';
		$this->assertEquals($this->UserPrefsArrays[$key], $this->UserPrefs->getPref($key));
	
	}

	/**
	 * Tests UserPrefs->getPrefs()
	 */
	public function testGetPrefsReturnNull()
	{
		$key = 'non_existing_key';
		$this->assertNull($this->UserPrefs->getPref($key));
	
	}
}

