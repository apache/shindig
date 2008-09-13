<?php

require_once 'src/common/sample/BasicSecurityToken.php';

require_once 'external/PHPUnit/Framework/TestCase.php';

/**
 * BasicSecurityToken test case.
 */
class BasicSecurityTokenTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var BasicSecurityToken
	 */
	private $BasicSecurityToken;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->BasicSecurityToken = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1');
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->BasicSecurityToken = null;		
		parent::tearDown();
	}

	/**
	 * Tests BasicSecurityToken::createFromValues()
	 */
	public function testCreateFromValues()
	{
		$token = BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1');
		$this->assertEquals('owner', $token->getOwnerId());
		$this->assertEquals('viewer', $token->getViewerId());
		$this->assertEquals('app', $token->getAppId());
		$this->assertEquals('domain', $token->getDomain());
		$this->assertEquals('appUrl', $token->getAppUrl());
		$this->assertEquals('1', $token->getModuleId());
	}

	/**
	 * Tests BasicSecurityToken->getAppId()
	 */
	public function testGetAppId()
	{
		$this->assertEquals('app', $this->BasicSecurityToken->getAppId());	
	}

	/**
	 * Tests BasicSecurityToken->getAppUrl()
	 */
	public function testGetAppUrl()
	{
		$this->assertEquals('appUrl', $this->BasicSecurityToken->getAppUrl());
	}

	/**
	 * Tests BasicSecurityToken->getDomain()
	 */
	public function testGetDomain()
	{
		$this->assertEquals('domain', $this->BasicSecurityToken->getDomain());
	}

	/**
	 * Tests BasicSecurityToken->getModuleId()
	 */
	public function testGetModuleId()
	{
		$this->assertEquals(1, $this->BasicSecurityToken->getModuleId());
	}

	/**
	 * Tests BasicSecurityToken->getOwnerId()
	 */
	public function testGetOwnerId()
	{
		$this->assertEquals('owner', $this->BasicSecurityToken->getOwnerId());
	}

	/**
	 * Tests BasicSecurityToken->getViewerId()
	 */
	public function testGetViewerId()
	{
		$this->assertEquals('viewer', $this->BasicSecurityToken->getViewerId());
	}

	/**
	 * Tests BasicSecurityToken->isAnonymous()
	 */
	public function testIsAnonymous()
	{
		$this->assertFalse($this->BasicSecurityToken->isAnonymous());
	}
}
