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
package org.apache.shindig.social.dataservice;

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.common.SecurityToken;

import javax.servlet.http.HttpServletRequest;

public class DataRequestHandlerTest extends TestCase {

  public void testgetParamsFromRequest() throws Exception {
    HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(req.getPathInfo()).andReturn("/people/5/@self");
    DataRequestHandler drh = new DataRequestHandler(null) {
      ResponseItem handleDelete(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handlePut(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handlePost(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }

      ResponseItem handleGet(HttpServletRequest servletRequest,
          SecurityToken token) {
        return null;
      }
    };

    EasyMock.replay(req);
    String[] params = drh.getParamsFromRequest(req);
    assertEquals("5", params[0]);
    assertEquals("@self", params[1]);
    EasyMock.verify(req);
  }
}