<?php

require_once 'src/gadgets/ViewSpec.php';
require_once 'PHPUnit/Framework/TestCase.php';

require_once 'src/gadgets/GadgetSpecParser.php';

/**
 * ViewSpec test case.
 */
class ViewSpecTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var ViewSpec
	 */
	private $ViewSpec;
	
	/**
	 * @var GadgetXML
	 */
	private $GadgetXML = '<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Dummie gadget" />
    <Require feature="rpc">
  </Require> 
  <Content type="html">
  <![CDATA[<h1>Hello, world!</h1>]]>
  </Content>
</Module>
	';

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
	 * Tests ViewSpec->__construct()
	 */
	public function test__construct()
	{
		$doc = simplexml_load_string($this->GadgetXML, 'SimpleXMLElement', LIBXML_NOCDATA);
		$content = $doc->Content[0];
		$attributes = $content->attributes();
		$view = isset($attributes['view']) ? trim($attributes['view']) : DEFAULT_VIEW;
		$attributes['type'] = 'url';
		
		$this->setExpectedException('SpecParserException');
		$this->ViewSpec = new ViewSpec($view, $content);
	
	}

}

