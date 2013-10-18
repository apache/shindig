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
package org.apache.shindig.gadgets.oauth2;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;

public class OAuth2ArgumentsTest extends MockUtils {
  private HttpServletRequest requestMock;
  private Map<String, String> attrs;

  @Before
  public void setUp() throws Exception {
    attrs = Maps.newHashMap();
    attrs.put("OAUTH_SCOPE", MockUtils.SCOPE);
    attrs.put("OAUTH_SERVICE_NAME", MockUtils.SERVICE_NAME);
    attrs.put("bypassSpecCache", "1");
    attrs.put("extraParam", "extraValue");
    requestMock = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(requestMock.getParameterNames()).andReturn(
        Collections.enumeration(attrs.keySet()));
    EasyMock.expect(requestMock.getParameterMap()).andReturn(attrs);
    for (final Entry<String, String> entry : attrs.entrySet()) {
      EasyMock.expect(requestMock.getParameter(entry.getKey())).andReturn(
          entry.getValue());
    }
    EasyMock.replay(requestMock);
  }

  @Test
  public void testOAuth2Arguments_1() throws Exception {
    final OAuth2Arguments result = new OAuth2Arguments(requestMock);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.getBypassSpecCache());
    Assert.assertEquals(MockUtils.SCOPE, result.getScope());
    Assert.assertEquals(MockUtils.SERVICE_NAME, result.getServiceName());
  }

  @Test
  public void testOAuth2Arguments_2() throws Exception {
    final Map<String, String> attrs1 = Maps.newHashMap();
    attrs1.put("OAUTH_SCOPE", "xxx");
    attrs1.put("OAUTH_SERVICE_NAME", "yyy");
    attrs1.put("bypassSpecCache", "0");
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getParameterNames())
        .andReturn(Collections.enumeration(attrs1.keySet()));
    EasyMock.expect(request.getParameterMap()).andReturn(attrs1);
    for (final Entry<String, String> entry : attrs1.entrySet()) {
      EasyMock.expect(request.getParameter(entry.getKey())).andReturn(entry.getValue());
    }
    EasyMock.replay(request);

    final OAuth2Arguments orig = new OAuth2Arguments(request);

    final OAuth2Arguments result = new OAuth2Arguments(orig);

    Assert.assertNotNull(result);
    Assert.assertFalse(result.getBypassSpecCache());
    Assert.assertEquals("xxx", result.getScope());
    Assert.assertEquals("yyy", result.getServiceName());
  }

  @Test
  public void testOAuth2Arguments_3() throws Exception {
    final RequestAuthenticationInfo info = EasyMock.createNiceMock(RequestAuthenticationInfo.class);
    EasyMock.expect(info.getAuthType()).andReturn(AuthType.OAUTH2);
    EasyMock.expect(info.getAttributes()).andReturn(attrs);
    EasyMock.replay(info);

    final OAuth2Arguments result = new OAuth2Arguments(info);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.getBypassSpecCache());
    Assert.assertEquals(MockUtils.SCOPE, result.getScope());
    Assert.assertEquals(MockUtils.SERVICE_NAME, result.getServiceName());
  }

  @Test
  public void testOAuth2Arguments_4() throws Exception {
    final OAuth2Arguments result = new OAuth2Arguments(AuthType.OAUTH2, attrs);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.getBypassSpecCache());
    Assert.assertEquals(MockUtils.SCOPE, result.getScope());
    Assert.assertEquals(MockUtils.SERVICE_NAME, result.getServiceName());
  }

  @Test
  public void testEquals_1() throws Exception {
    final OAuth2Arguments fixture = new OAuth2Arguments(AuthType.OAUTH2, attrs);

    final Object obj = new OAuth2Arguments(requestMock);

    final boolean result = fixture.equals(obj);

    Assert.assertTrue(result);
  }

  @Test
  public void testEquals_2() throws Exception {
    final Object obj = new Object();

    final boolean result = requestMock.equals(obj);

    Assert.assertFalse(result);
  }

  @Test
  public void testEquals_3() throws Exception {
    final boolean result = requestMock.equals(null);

    Assert.assertFalse(result);
  }

  @Test
  public void testEquals_4() throws Exception {
    final Map<String, String> attrs1 = Maps.newHashMap();
    attrs1.put("OAUTH_SCOPE", "xxx");
    attrs1.put("OAUTH_SERVICE_NAME", "yyy");
    attrs1.put("bypassSpecCache", "0");
    final HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getParameterNames())
        .andReturn(Collections.enumeration(attrs1.keySet()));
    EasyMock.expect(request.getParameterMap()).andReturn(attrs1);
    for (final Entry<String, String> entry : attrs1.entrySet()) {
      EasyMock.expect(request.getParameter(entry.getKey())).andReturn(entry.getValue());
    }
    EasyMock.replay(request);

    final OAuth2Arguments obj = new OAuth2Arguments(request);

    final boolean result = requestMock.equals(obj);

    Assert.assertFalse(result);
  }

  @Test
  public void testGetBypassSpecCache_1() throws Exception {
    final OAuth2Arguments fixture = new OAuth2Arguments(requestMock);

    final boolean result = fixture.getBypassSpecCache();

    Assert.assertTrue(result);
  }

  @Test
  public void testGetScope_1() throws Exception {
    final OAuth2Arguments fixture = new OAuth2Arguments(requestMock);

    final String result = fixture.getScope();

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.SCOPE, result);
  }

  @Test
  public void testGetServiceName_1() throws Exception {
    final OAuth2Arguments fixture = new OAuth2Arguments(requestMock);

    final String result = fixture.getServiceName();

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.SERVICE_NAME, result);
  }

  @Test
  public void testHashCode_1() throws Exception {
    final OAuth2Arguments fixture = new OAuth2Arguments(requestMock);

    final int result = fixture.hashCode();

    Assert.assertEquals(-1928533070, result);
  }
}
