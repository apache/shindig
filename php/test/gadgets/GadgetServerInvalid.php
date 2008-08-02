<?php

require_once 'src/gadgets/GadgetException.php';
require_once 'src/gadgets/GadgetServer.php';
require_once 'src/gadgets/GadgetContext.php';

require_once 'src/common/RemoteContentFetcher.php';
require_once 'src/common/RemoteContentRequest.php';
require_once 'src/gadgets/JsFeatureLoader.php';
require_once 'src/gadgets/GadgetFeatureRegistry.php';
require_once 'src/gadgets/JsLibrary.php';
require_once 'src/gadgets/GadgetFeatureFactory.php';
require_once 'src/gadgets/GadgetFeature.php';
require_once 'src/gadgets/JsLibraryFeatureFactory.php';

require_once 'src/common/samplecontainer/BasicRemoteContentFetcher.php';

require_once 'PHPUnit/Framework/TestCase.php';

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
