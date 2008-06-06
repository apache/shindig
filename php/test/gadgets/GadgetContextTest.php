<?php

require_once '../../src/gadgets/GadgetContext.php';
require_once '../PHPUnit/Framework/TestCase.php';
require_once '../../config.php';

/**
 * GadgetContext test case.
 */
class GadgetContextTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var GadgetContext
	 */
	private $GadgetContext;
	
	/**
	 * @var testData
	 */
	private $testData = array(
		'url' => 'http://www.google.com/gadget-',
		'libs' => '',
		'synd' => 'default',
		'nocache' => '',
		'container' => 'default',
		'view' => 'default',
		'mid' => '123',
		'bcp' => ''
	);
	
	/**
	 * @var gadgetRenderingContext
	 */
	private $gadgetRenderingContext = 'GADGET';
	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp() {
		parent::setUp ();
	
		$_GET = $this->testData;
		$this->GadgetContext = new GadgetContext($this->gadgetRenderingContext);
	
	}
	
	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown() {
		$this->GadgetContext = null;
		
		parent::tearDown ();
	}
	
	/**
	 * Constructs the test case.
	 */
	public function __construct() {
		// TODO Auto-generated constructor
		
		
	}
	
	/**
	 * Tests GadgetContext->getBlacklist()
	 */
	public function testGetBlacklist() {
		$this->markTestIncomplete ( "getBlacklist test not implemented" );
		
		$this->GadgetContext->getBlacklist(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getCache()
	 */
	public function testGetCache() {
		// TODO Auto-generated GadgetContextTest->testGetCache()
		$this->markTestIncomplete ( "getCache test not implemented" );
		
		$this->GadgetContext->getCache(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getContainer()
	 */
	public function testGetContainer() {
		
		$this->assertEquals($this->testData['container'],$this->GadgetContext->getContainer());
	
	}
	
	/**
	 * Tests GadgetContext->getContainerConfig()
	 */
	public function testGetContainerConfig() {
		// TODO Auto-generated GadgetContextTest->testGetContainerConfig()
		$this->markTestIncomplete ( "getContainerConfig test not implemented" );
		
		$this->GadgetContext->getContainerConfig(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getFeatureRegistry()
	 */
	public function testGetFeatureRegistry() {
		// TODO Auto-generated GadgetContextTest->testGetFeatureRegistry()
		$this->markTestIncomplete ( "getFeatureRegistry test not implemented" );
		
		$this->GadgetContext->getFeatureRegistry(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getForcedJsLibs()
	 */
	public function testGetForcedJsLibs() {
		
		$this->assertEquals($this->testData['libs'],$this->GadgetContext->getForcedJsLibs());
	
	}
	
	/**
	 * Tests GadgetContext->getGadgetId()
	 */
	public function testGetGadgetId() {
		// TODO Auto-generated GadgetContextTest->testGetGadgetId()
		$this->markTestIncomplete ( "getGadgetId test not implemented" );
		
		$this->GadgetContext->getGadgetId(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getHttpFetcher()
	 */
	public function testGetHttpFetcher() {
		// TODO Auto-generated GadgetContextTest->testGetHttpFetcher()
		$this->markTestIncomplete ( "getHttpFetcher test not implemented" );
		
		$this->GadgetContext->getHttpFetcher(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getIgnoreCache()
	 */
	public function testGetIgnoreCache() {
		// TODO Auto-generated GadgetContextTest->testGetIgnoreCache()
		$this->markTestIncomplete ( "getIgnoreCache test not implemented" );
		
		$this->GadgetContext->getIgnoreCache(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getLocale()
	 */
	public function testGetLocale() {
		// TODO Auto-generated GadgetContextTest->testGetLocale()
		$this->markTestIncomplete ( "getLocale test not implemented" );
		
		$this->GadgetContext->getLocale(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getModuleId()
	 */
	public function testGetModuleId() {		
		$this->assertEquals($this->testData['mid'],$this->GadgetContext->getModuleId());
	
	}
	
	/**
	 * Tests GadgetContext->getRegistry()
	 */
	public function testGetRegistry() {
		// TODO Auto-generated GadgetContextTest->testGetRegistry()
		$this->markTestIncomplete ( "getRegistry test not implemented" );
		
		$this->GadgetContext->getRegistry(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->getRenderingContext()
	 */
	public function testGetRenderingContext() {
		
		$this->assertEquals($this->gadgetRenderingContext,$this->GadgetContext->getRenderingContext());
	
	}
	
	/**
	 * Tests GadgetContext->getUrl()
	 */
	public function testGetUrl() {
		
		$this->assertEquals($this->testData['url'],$this->GadgetContext->getUrl());
	
	}
	
	/**
	 * Tests GadgetContext->getUserPrefs()
	 */
	public function testGetUserPrefs() {
		// TODO Auto-generated GadgetContextTest->testGetUserPrefs()
		$this->markTestIncomplete ( "getUserPrefs test not implemented" );
		
		$this->GadgetContext->getUserPrefs();
	
	}
	
	/**
	 * Tests GadgetContext->getView()
	 */
	public function testGetView() {
		
		$this->assertEquals($this->testData['view'], $this->GadgetContext->getView());
	
	}
	
	/**
	 * Tests GadgetContext->setBlacklist()
	 */
	public function testSetBlacklist() {
		// TODO Auto-generated GadgetContextTest->testSetBlacklist()
		$this->markTestIncomplete ( "setBlacklist test not implemented" );
		
		$this->GadgetContext->setBlacklist(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setCache()
	 */
	public function testSetCache() {
		// TODO Auto-generated GadgetContextTest->testSetCache()
		$this->markTestIncomplete ( "setCache test not implemented" );
		
		$this->GadgetContext->setCache(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setContainer()
	 */
	public function testSetContainer() {
		// TODO Auto-generated GadgetContextTest->testSetContainer()
		$this->markTestIncomplete ( "setContainer test not implemented" );
		
		$this->GadgetContext->setContainer(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setContainerConfig()
	 */
	public function testSetContainerConfig() {
		// TODO Auto-generated GadgetContextTest->testSetContainerConfig()
		$this->markTestIncomplete ( "setContainerConfig test not implemented" );
		
		$this->GadgetContext->setContainerConfig(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setForcedJsLibs()
	 */
	public function testSetForcedJsLibs() {
		// TODO Auto-generated GadgetContextTest->testSetForcedJsLibs()
		$this->markTestIncomplete ( "setForcedJsLibs test not implemented" );
		
		$this->GadgetContext->setForcedJsLibs(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setGadgetId()
	 */
	public function testSetGadgetId() {
		// TODO Auto-generated GadgetContextTest->testSetGadgetId()
		$this->markTestIncomplete ( "setGadgetId test not implemented" );
		
		$this->GadgetContext->setGadgetId(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setHttpFetcher()
	 */
	public function testSetHttpFetcher() {
		// TODO Auto-generated GadgetContextTest->testSetHttpFetcher()
		$this->markTestIncomplete ( "setHttpFetcher test not implemented" );
		
		$this->GadgetContext->setHttpFetcher(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setIgnoreCache()
	 */
	public function testSetIgnoreCache() {
		// TODO Auto-generated GadgetContextTest->testSetIgnoreCache()
		$this->markTestIncomplete ( "setIgnoreCache test not implemented" );
		
		$this->GadgetContext->setIgnoreCache(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setLocale()
	 */
	public function testSetLocale() {
		// TODO Auto-generated GadgetContextTest->testSetLocale()
		$this->markTestIncomplete ( "setLocale test not implemented" );
		
		$this->GadgetContext->setLocale(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setModuleId()
	 */
	public function testSetModuleId() {
		// TODO Auto-generated GadgetContextTest->testSetModuleId()
		$this->markTestIncomplete ( "setModuleId test not implemented" );
		
		$this->GadgetContext->setModuleId(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setRegistry()
	 */
	public function testSetRegistry() {
		// TODO Auto-generated GadgetContextTest->testSetRegistry()
		$this->markTestIncomplete ( "setRegistry test not implemented" );
		
		$this->GadgetContext->setRegistry(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setRenderingContext()
	 */
	public function testSetRenderingContext() {
		$redering_context = 'Dummie_rendering_context';
		$this->GadgetContext->setRenderingContext($redering_context);
		$this->assertAttributeEquals($redering_context,'renderingContext',$this->GadgetContext);
	
	}
	
	/**
	 * Tests GadgetContext->setUrl()
	 */
	public function testSetUrl() {
		$url = 'Dummie_url';
		$this->GadgetContext->setUrl($url);
		$this->assertAttributeEquals($url,'url',$this->GadgetContext);
	
	}
	
	/**
	 * Tests GadgetContext->setUserPrefs()
	 */
	public function testSetUserPrefs() {
		// TODO Auto-generated GadgetContextTest->testSetUserPrefs()
		$this->markTestIncomplete ( "setUserPrefs test not implemented" );
		
		$this->GadgetContext->setUserPrefs(/* parameters */);
	
	}
	
	/**
	 * Tests GadgetContext->setView()
	 */
	public function testSetView() {
		$view = 'Dummie_view';
		$this->GadgetContext->setView($view);
		$this->assertAttributeEquals($view,'view',$this->GadgetContext);
		
	}
	

}

