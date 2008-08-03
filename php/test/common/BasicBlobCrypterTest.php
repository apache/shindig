<?php

/**
 * BasicBlobCrypter test case.
 */
class BasicBlobCrypterTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var BasicBlobCrypter
	 */
	private $BasicBlobCrypter;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->BasicBlobCrypter = new BasicBlobCrypter();	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->BasicBlobCrypter = null;		
		parent::tearDown();
	}

	/**
	 * Tests BasicBlobCrypter->__construct()
	 */
	public function test__construct()
	{
		$this->BasicBlobCrypter->__construct();
	}

	/**
	 * Tests BasicBlobCrypter->wrap()
	 */
	public function testWrap()
	{
		$test = array();
		$test['o'] = 'o';
		$test['v'] = 'v';
		$test['a'] = 'a';
		$test['d'] = 'd';
		$test['u'] = 'u';
		$test['m'] = 'm';		
		$wrapped = $this->BasicBlobCrypter->wrap($test);
		$unwrapped = $this->BasicBlobCrypter->unwrap($wrapped, 3600);
		$this->assertEquals($unwrapped['o'], 'o');
		$this->assertEquals($unwrapped['v'], 'v');
		$this->assertEquals($unwrapped['a'], 'a');
		$this->assertEquals($unwrapped['d'], 'd');
		$this->assertEquals($unwrapped['u'], 'u');
		$this->assertEquals($unwrapped['m'], 'm');
	}
	/**
	 * Tests BasicBlobCrypter->wrap() exception
	 */
	public function testWrapException()
	{
		$this->setExpectedException('BlobExpiredException');
		$test = array();
		$test['o'] = 'o';
		$test['v'] = 'v';
		$test['a'] = 'a';
		$test['d'] = 'd';
		$test['u'] = 'u';
		$test['m'] = 'm';		
		$wrapped = $this->BasicBlobCrypter->wrap($test);
		/* there is a 180 seconds clock skew allowed, so this way we make sure it's expired */
		$this->BasicBlobCrypter->unwrap($wrapped, -4000); 
	}

}

