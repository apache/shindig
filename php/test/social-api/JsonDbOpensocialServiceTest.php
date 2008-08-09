<?
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Implementation tests based on supported services backed by a JSON DB
 */
class JsonDbOpensocialServiceTest extends PHPUnit_Framework_TestCase {
	
	/**
	 * @var JsonDbOpensocialService
	 */
	private $JsonDbOpensocialService;

	/**
	 * Prepares the environment before running a test.
	 */
	protected function setUp()
	{
		parent::setUp();
		$this->clearFileCache();
		$this->JsonDbOpensocialService = new JsonDbOpensocialService();
	}

	/**
	 * Cleans up the environment after running a test.
	 */
	protected function tearDown()
	{
		$this->JsonDbOpensocialService = null;
		$this->clearFileCache();
		parent::tearDown();
	}

	/**
	 * Tests JsonDbOpensocialService->getPeople()
	 */
	public function testGetPeople()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$sortOrder = null;
		$filter = null;
		$first = null;
		$max = null;
		$profileDetails = array('id', 'name', 'thumbnailUrl');
		$networkDistance = null;
		
		//With existing data
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getPeople($userId, $groupId, $sortOrder, $filter, $first, $max, $profileDetails, $networkDistance, $token);
		$response = $responseItem->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals(1, $response->getTotalResults());
		$this->assertEquals(1, count($entry));
		$this->assertEquals('john.doe', $entry[0]['id']);
		$this->assertEquals('Doe', $entry[0]['name']['familyName']);
		$this->assertEquals('John', $entry[0]['name']['givenName']);
		$this->assertEquals('John Doe', $entry[0]['name']['unstructured']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getPeople($userId, $groupId, $sortOrder, $filter, $first, $max, $profileDetails, $networkDistance, $token);
		$response = $responseItem->getResponse();
		$this->assertEquals(0, $response->getTotalResults());
		$this->assertEquals(0, count($response->getEntry()));
	}

	/**
	 * Tests JsonDbOpensocialService->getPerson()
	 */
	public function testGetPerson()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$profileDetails = array('id', 'name', 'thumbnailUrl');
		
		//With existing data
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$person = $this->JsonDbOpensocialService->getPerson($userId, $groupId, $profileDetails, $token);
		$response = $person->getResponse();
		$this->assertNotNull($response);
		$this->assertEquals('john.doe', $response['id']);
		$this->assertEquals('Doe', $response['name']['familyName']);
		$this->assertEquals('John', $response['name']['givenName']);
		$this->assertEquals('John Doe', $response['name']['unstructured']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$person = $this->JsonDbOpensocialService->getPerson($userId, $groupId, $profileDetails, $token);
		$response = $person->getResponse();
		$this->assertEquals('NOT_FOUND', $person->getError());
		$this->assertEquals('Person not found', $person->getErrorMessage());
		$this->assertNull($person->getResponse());
	}

	/**
	 * Tests JsonDbOpensocialService->getPersonData()
	 */
	public function testGetPersonData()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$profileDetails = array('count', 'size');
		$appId = 'app';
		
		//With existing data
		$token = BasicSecurityToken::createFromValues('canonical', 'canonical', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getPersonData($userId, $groupId, $profileDetails, $appId, $token);
		$response = $responseItem->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals(1, $response->getTotalResults());
		$this->assertEquals(1, count($entry));
		$this->assertEquals(2, $entry['canonical']['count']);
		$this->assertEquals(100, $entry['canonical']['size']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getPersonData($userId, $groupId, $profileDetails, $appId, $token);
		$response = $responseItem->getResponse();
		$this->assertEquals(0, $response->getTotalResults());
		$this->assertEquals(0, count($response->getEntry()));
	}

	/**
	 * Tests JsonDbOpensocialService->updatePersonData()
	 */
	public function testUpdatePersonData()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$profileDetails = array('count', 'size');
		$values = array();
		$values['count'] = 10;
		$values['size'] = 500;
		$appId = 'app';
		
/*
		//With existing data
		$token = BasicSecurityToken::createFromValues('canonical', 'canonical', 'app', 'domain', 'appUrl', '1');
		$this->JsonDbOpensocialService->updatePersonData($userId, $groupId, $profileDetails, $values, $appId, $token);
		$responseItem = $this->JsonDbOpensocialService->getPersonData($userId, $groupId, $profileDetails, $appId, $token);
		$response = $responseItem->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals(1, $response->getTotalResults());
		$this->assertEquals(1, count($entry));
		$this->assertEquals(10, $entry['canonical']['count']);
		$this->assertEquals(500, $entry['canonical']['size']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$this->JsonDbOpensocialService->updatePersonData($userId, $groupId, $profileDetails, $values, $appId, $token);
		$responseItem = $this->JsonDbOpensocialService->getPersonData($userId, $groupId, $profileDetails, $appId, $token);
		$response = $responseItem->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals(1, $response->getTotalResults());
		$this->assertEquals(1, count($entry));
		$this->assertEquals(10, $entry['notexists']['count']);
		$this->assertEquals(500, $entry['notexists']['size']);
*/
	}

	/**
	 * Tests JsonDbOpensocialService->deletePersonData()
	 */
	public function testDeletePersonData()
	{
		//TODO: Implement me!
	}

	/**
	 * Tests JsonDbOpensocialService->getActivities()
	 */
	public function testGetActivities()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		
		//With existing data
		$token = BasicSecurityToken::createFromValues('canonical', 'canonical', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getActivities($userId, $groupId, null, null, $token);
		$response = $responseItem->getResponse();
		$entry = $response->getEntry();
		$this->assertEquals(2, $response->getTotalResults());
		$this->assertEquals(2, count($entry));
		$this->assertEquals(1, $entry[0]['appId']);
		$this->assertEquals('Went rafting', $entry[0]['body']);
		$this->assertEquals(1, $entry[0]['bodyId']);
		$this->assertEquals('http://www.example.org/123456', $entry[0]['externalId']);
		$this->assertEquals(1, $entry[0]['id']);
		$this->assertEquals('2008-06-06T12:12:12Z', $entry[0]['updated']);
		$this->assertEquals('image/*', $entry[0]['mediaItems'][0]['mimeType']);
		$this->assertEquals('image', $entry[0]['mediaItems'][0]['type']);
		$this->assertEquals('http://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Rafting_em_Brotas.jpg/800px-Rafting_em_Brotas.jpg', $entry[0]['mediaItems'][0]['url']);
		$this->assertEquals('audio/mpeg', $entry[0]['mediaItems'][1]['mimeType']);
		$this->assertEquals('audio', $entry[0]['mediaItems'][1]['type']);
		$this->assertEquals('http://www.archive.org/download/testmp3testfile/mpthreetest.mp3', $entry[0]['mediaItems'][1]['url']);
		$this->assertEquals(1111111111, $entry[0]['postedTime']);
		$this->assertEquals(0.7, $entry[0]['priority']);
		$this->assertEquals('http://upload.wikimedia.org/wikipedia/commons/0/02/Nuvola_apps_edu_languages.gif', $entry[0]['streamFaviconUrl']);
		$this->assertEquals('http://www.example.org/canonical/streamsource', $entry[0]['streamSourceUrl']);
		$this->assertEquals('All my activities', $entry[0]['streamTitle']);
		$this->assertEquals('http://www.example.org/canonical/activities', $entry[0]['streamUrl']);
		$this->assertEquals(true, $entry[0]['templateParams']['small']);
		$this->assertEquals('and got wet', $entry[0]['templateParams']['otherContent']);
		$this->assertEquals('My trip', $entry[0]['title']);
		$this->assertEquals(1, $entry[0]['titleId']);
		$this->assertEquals('http://www.example.org/canonical/activities/1', $entry[0]['url']);
		$this->assertEquals('canonical', $entry[0]['userId']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getActivities($userId, $groupId, null, null, $token);
		$response = $responseItem->getResponse();
		$this->assertEquals(0, $response->getTotalResults());
		$this->assertEquals(0, count($response->getEntry()));
	}

	/**
	 * Tests JsonDbOpensocialService->getActivity()
	 */
	public function testGetActivity()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$token = BasicSecurityToken::createFromValues('canonical', 'canonical', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getActivity($userId, $groupId, '1', null, null, $token);
		$entry = $responseItem->getResponse();
		$this->assertEquals(1, count($responseItem));
		$this->assertEquals(1, $entry['appId']);
		$this->assertEquals('Went rafting', $entry['body']);
		$this->assertEquals(1, $entry['bodyId']);
		$this->assertEquals('http://www.example.org/123456', $entry['externalId']);
		$this->assertEquals(1, $entry['id']);
		$this->assertEquals('2008-06-06T12:12:12Z', $entry['updated']);
		$this->assertEquals('image/*', $entry['mediaItems'][0]['mimeType']);
		$this->assertEquals('image', $entry['mediaItems'][0]['type']);
		$this->assertEquals('http://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Rafting_em_Brotas.jpg/800px-Rafting_em_Brotas.jpg', $entry['mediaItems'][0]['url']);
		$this->assertEquals('audio/mpeg', $entry['mediaItems'][1]['mimeType']);
		$this->assertEquals('audio', $entry['mediaItems'][1]['type']);
		$this->assertEquals('http://www.archive.org/download/testmp3testfile/mpthreetest.mp3', $entry['mediaItems'][1]['url']);
		$this->assertEquals(1111111111, $entry['postedTime']);
		$this->assertEquals(0.7, $entry['priority']);
		$this->assertEquals('http://upload.wikimedia.org/wikipedia/commons/0/02/Nuvola_apps_edu_languages.gif', $entry['streamFaviconUrl']);
		$this->assertEquals('http://www.example.org/canonical/streamsource', $entry['streamSourceUrl']);
		$this->assertEquals('All my activities', $entry['streamTitle']);
		$this->assertEquals('http://www.example.org/canonical/activities', $entry['streamUrl']);
		$this->assertEquals(true, $entry['templateParams']['small']);
		$this->assertEquals('and got wet', $entry['templateParams']['otherContent']);
		$this->assertEquals('My trip', $entry['title']);
		$this->assertEquals(1, $entry['titleId']);
		$this->assertEquals('http://www.example.org/canonical/activities/1', $entry['url']);
		$this->assertEquals('canonical', $entry['userId']);
		
		//With non existing data
		$token = BasicSecurityToken::createFromValues('notexists', 'notexists', 'app', 'domain', 'appUrl', '1');
		$activity = $this->JsonDbOpensocialService->getActivity($userId, $groupId, '1', null, null, $token);
		$this->assertEquals('NOT_FOUND', $activity->getError());
		$this->assertEquals('Activity not found', $activity->getErrorMessage());
		$this->assertNull($activity->getResponse());
	}

	/**
	 * Tests JsonDbOpensocialService->createActivity()
	 */
	public function testCreateActivity()
	{
		$userId = new UserId('viewer', null);
		$groupId = new GroupId('self', null);
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$activity = array();
		$activity['id'] = 2;
		$activity['title'] = 'John Doe wrote: asdasd';
		$activity['body'] = 'write back!';
		$activity['mediaItems'] = array();
		$activity['mediaItems'][0]['type'] = 'image';
		$activity['mediaItems'][0]['mimeType'] = 'image';
		$activity['mediaItems'][0]['image'] = 'http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg';
		$this->JsonDbOpensocialService->createActivity($userId, $activity, $token);
/*		
		//Validating the created activity
		$token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 'app', 'domain', 'appUrl', '1');
		$responseItem = $this->JsonDbOpensocialService->getActivity($userId, $groupId, 2, null, null, $token);
		$entry = $responseItem->getResponse();
		$this->assertEquals(2, $entry['id']);
		$this->assertEquals('John Doe wrote: asdasd', $entry['title']);
		$this->assertEquals('write back!', $entry['body']);
		$this->assertEquals('image', $activity['mediaItems'][0]['type']);
		$this->assertEquals('image', $activity['mediaItems'][0]['mimeType']);
		$this->assertEquals('http://cdn.davesdaily.com/pictures/784-awesome-hands.jpg', $activity['mediaItems'][0]['image']);
		$this->assertEquals('app', $entry['appId']);
*/
	}

	private function clearFileCache()
	{
		unlink(sys_get_temp_dir() . "ShindigDb.json");
	}
}
