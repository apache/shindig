<?php

require_once 'src/common/samplecontainer/BasicRemoteContent.php';

require_once 'external/PHPUnit/Framework/TestCase.php';

/**
 * BasicRemoteContent test case.
 */
class BasicRemoteContentTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var BasicRemoteContent
	 */
	private $BasicRemoteContent;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->BasicRemoteContent = new BasicRemoteContent();	
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->BasicRemoteContent = null;		
		parent::tearDown();
	}

	/**
	 * Tests BasicRemoteContent->fetch()
	 */
	public function testFetch()
	{
		$request = new RemoteContentRequest('http://test.chabotc.com/ok.html');
		$context = new TestContext();
		$ret = $this->BasicRemoteContent->fetch($request, $context);
		$content = $ret->getResponseContent();
		$this->assertEquals("OK\n", $content);
	}
	
	/**
	 * Tests BasicRemoteContent->fetch() 404 response
	 */
	public function testFetch404()
	{
		$request = new RemoteContentRequest('http://test.chabotc.com/fail.html');
		$context = new TestContext();
		$ret = $this->BasicRemoteContent->fetch($request, $context);
		$this->assertEquals('404', $ret->getHttpCode());
	}
}
