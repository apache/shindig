<?php
namespace apache\shindig\test\social;

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

require_once 'RestBase.php';

class GroupsRestTest extends RestBase {


  public function testGroupsLifeCycleInJson() {
    // Get the groups for the user.
    $ret = $this->curlRest('/groups/john.doe', '', 'application/json', 'GET');
    $retDecoded = json_decode($ret, true);
    $this->assertTrue($ret != $retDecoded && $ret != null, "Invalid json string in return: $ret.");
    $this->assertTrue(isset($retDecoded['entry']) && isset($retDecoded['entry']["john.doe"])
        && isset($retDecoded['entry']["john.doe"][0])
        && $retDecoded['entry']["john.doe"][0] == '1', "Unexpected return value: $ret.");
  }
}
