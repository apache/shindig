/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.server.endtoend;

import org.apache.shindig.common.BasicSecurityToken;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end tests of the newFetchPersonRequest() API.
 */
public class FetchPersonTest extends EndToEndTests {
  private BasicSecurityToken token;

  @Before
  public void createSecurityToken() throws Exception {
    token = createToken("canonical", "john.doe");
  }
  
  @Test
  public void fetchPerson() throws Exception {
    executePageTest("fetchPersonTest", "fetchId", token);
  }

  @Test
  public void fetchOwner() throws Exception {
    executePageTest("fetchPersonTest", "fetchOwner", token);
  }

  @Test
  public void fetchViewer() throws Exception {
    executePageTest("fetchPersonTest", "fetchViewer", token);
  }
}
