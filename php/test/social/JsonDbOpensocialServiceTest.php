<?php
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * JsonDbOpensocialService test case.
 */
class JsonDbOpensocialServiceTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var JsonDbOpensocialService
   */
  private $service;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->service = new JsonDbOpensocialService();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->service = null;
    parent::tearDown();
  }

  /**
   * Constructs the test case.
   */
  public function __construct() {}

  /**
   * Tests JsonDbOpensocialService->getActivities() with paging.
   */
  public function testGetActivities() {
    $token = BasicSecurityToken::createFromValues('jane.doe', 'jane.doe', 1, 1, 1, 1, 'default');
    $userId = new UserId('owner', null);
    $userIds = array($userId);
    $groupId = new GroupId('self', null);
    $startIndex = 1;
    $count = 1;
    
    $ret = $this->service->getActivities($userIds, $groupId, 1, null, null, null, null, $startIndex, $count, null, 1, $token);
    $this->assertEquals($startIndex, $ret->startIndex);
    $this->assertEquals($count, count($ret->entry));
    $this->assertEquals(2, $ret->totalResults);
    $this->assertEquals('2', $ret->entry[0]['id']);
    $this->assertEquals('Jane says George likes yoda!', $ret->entry[0]['title']);
    $this->assertEquals('or is it you?', $ret->entry[0]['body']);
  }
  
  public function testActivityLifeCycle() {
    $token = BasicSecurityToken::createFromValues('jane.doe', 'jane.doe', 1, 1, 1, 1, 'default');
    $userId = new UserId('owner', null);
    $userIds = array($userId);
    $groupId = new GroupId('self', null);
    $title = 'activity life cycle unit test title';
    $activity = array('id' => '1', 'userId' => 'userId', 'title' => $title);
    $ret = $this->service->getActivities($userIds, $groupId, 1, null, null, null, null, 0, 4, null, 1, $token);
    $this->assertEquals(2, count($ret->entry));
    
    $this->service->createActivity($userId, $groupId, $token->getAppId(), null, $activity, $token);
    $ret = $this->service->getActivities($userIds, $groupId, 1, null, null, null, null, 0, 4, null, 1, $token);
    $this->assertEquals(3, count($ret->entry));
    $id = null;
    foreach ($ret->entry as $entity) {
      if ($entity['title'] == $title) {
        $id = $entity['id'];
      }
    }
    $this->assertNotNull($id);
    
    $this->service->deleteActivities($userId, $groupId, $token->getAppId(), array($id), $token);
    
    $ret = $this->service->getActivities($userIds, $groupId, 1, null, null, null, null, 0, 4, null, 1, $token);
    $this->assertEquals(2, count($ret->entry));
  }

  public function testMessageLifeCycle() {
    $token = BasicSecurityToken::createFromValues('john.doe', 'canonical', 1, 1, 1, 1, 'default');
    $userId = new UserId('viewer', null);
    $body = 'message unit test body';
    $title = 'message unit test title';
    $message = array('id' => '1', 'body' => $body, 'title' => $title, 'type' => 'NOTIFICATION');
    $ret = $this->service->getMessages($userId, 'notification', null, null, null, $token);
    $this->assertEquals(3, count($ret->entry));
    
    $this->service->createMessage($userId, 'notification', $message, $token);
    $ret = $this->service->getMessages($userId, 'notification', null, null, null, $token);
    $this->assertEquals(4, count($ret->entry));
    
    $fetchedMessage = null;
    foreach ($ret->entry as $message) {
      if ($message['title'] == $title) {
        $fetchedMessage = $message;
      }
    }
    $this->assertEquals($body, $fetchedMessage['body']);
    
    $this->service->deleteMessages($userId, 'notification', array($fetchedMessage['id']), $token);
    $ret = $this->service->getMessages($userId, 'notification', null, null, null, $token);
    $this->assertEquals(3, count($ret->entry));
  }
  
  public function testGetMessages() {
    $token = BasicSecurityToken::createFromValues('canonical', 'canonical', 1, 1, 1, 1, 'default');
    $userId = new UserId('viewer', null);
    $options = new CollectionOptions();
    $options->setCount(2);
    $options->setStartIndex(1);
    $ret = $this->service->getMessages($userId, 'notification', Message::$DEFAULT_FIELDS, array('1', '2', '3'), $options, $token);
    
    $this->assertEquals(2, count($ret->entry));
    $this->assertEquals('2', $ret->entry[0]['id']);
    $this->assertEquals('notification', $ret->entry[0]['type']);
    $this->assertEquals('play checkers', $ret->entry[0]['title']);
    
    $this->assertEquals('3', $ret->entry[1]['id']);
    
    $this->assertEquals('you won!', $ret->entry[1]['title']);
  }
  
  public function testMessageCollectionLifeCycle() {
    // NOTE: If this method failes after the creation of the market collection.
    // There will be a market collection in the cached json file /tmp/ShindigDb.json
    // that prevents the test from passing. Change the test case and remove that
    // file then run again.
    $token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 1, 1, 1, 1, 'default');
    $title = 'created for message collection unit test';
    $userId = new UserId('owner', null);
    $msgColl = array('id' => '1', 'title' => $title);
    $this->service->createMessageCollection($userId, $msgColl, $token);
    
    $msgColls = $this->service->getMessageCollections($userId, null, null, $token)->entry;
    $this->assertEquals(4, count($msgColls));
    $fetchedMsgColl = null;
    foreach ($msgColls as $coll) {
      if ($coll['title'] == $title) {
        $fetchedMsgColl = $coll;
      }
    }
    $this->assertNotNull($fetchedMsgColl);
    
    $newTitle = 'new title for unit test';
    $msgColl['title'] = $newTitle;
    $msgColl['id'] = $fetchedMsgColl['id'];
    $this->service->updateMessageCollection($userId, $msgColl, $token);
    
    $msgColls = $this->service->getMessageCollections($userId, null, null, $token)->entry;
    $this->assertEquals(4, count($msgColls));
    foreach ($msgColls as $coll) {
      if ($coll['id'] == $fetchedMsgColl['id']) {
        $fetchedMsgColl = $coll;
      }
    }
    $this->assertEquals($newTitle, $fetchedMsgColl['title']);
    
    $this->service->deleteMessageCollection($userId, $fetchedMsgColl['id'], $token);
    $msgColls = $this->service->getMessageCollections($userId, null, null, $token)->entry;
    $this->assertEquals(3, count($msgColls));    
  }
  
  public function testGetMessageCollections() {
    $token = BasicSecurityToken::createFromValues('john.doe', 'john.doe', 1, 1, 1, 1, 'default');
    $userId = new UserId('owner', null);
    $ret = $this->service->getMessageCollections($userId, MessageCollection::$DEFAULT_FIELDS, null, $token);
    $this->assertEquals('Notifications', $ret->entry[0]['title']);
    $this->assertEquals('notification', $ret->entry[0]['id']);
    $this->assertEquals(2, $ret->entry[0]['total']);
    
    $this->assertEquals('Inbox', $ret->entry[1]['title']);
    $this->assertEquals('privateMessage', $ret->entry[1]['id']);
    $this->assertEquals(0, $ret->entry[1]['total']);
    
    $this->assertEquals('Inbox', $ret->entry[2]['title']);
    $this->assertEquals('publicMessage', $ret->entry[2]['id']);
    $this->assertEquals(0, $ret->entry[2]['total']);
  }
  
}
