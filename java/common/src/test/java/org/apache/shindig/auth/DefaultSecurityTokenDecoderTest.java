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
package org.apache.shindig.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.config.ContainerConfigException;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Tests of DefaultSecurityTokenDecoder
 */
public class DefaultSecurityTokenDecoderTest {

  private static class FakeContainerConfig extends AbstractContainerConfig {
    private final String tokenType;

    public FakeContainerConfig(String tokenType) throws ContainerConfigException {
      this.tokenType = tokenType;
    }

    @Override
    public Object getProperty(String container, String parameter) {
      if ("gadgets.securityTokenType".equals(parameter)) {
        if ("default".equals(container)) {
          return tokenType;
        }
      } else if ("gadgets.securityTokenKeyFile".equals(parameter)) {
        return "container key file: " + container;
      }
      return null;
    }

    @Override
    public Collection<String> getContainers() {
      return Lists.newArrayList("somecontainer");
    }
  }

  @Test
  public void testBasicDecoder() throws Exception {
    DefaultSecurityTokenDecoder decoder = new DefaultSecurityTokenDecoder(
        new FakeContainerConfig("insecure"));
    String token = "o:v:app:domain:appurl:12345:container";
    Map<String, String> parameters = Collections.singletonMap(
        SecurityTokenDecoder.SECURITY_TOKEN_NAME, token);
    SecurityToken st = decoder.createToken(parameters);
    assertEquals("o", st.getOwnerId());
    assertEquals("v", st.getViewerId());
    assertEquals("appurl", st.getAppUrl());
    assertEquals("container", st.getContainer());
  }

  @Test
  public void testInvalidDecoder() throws Exception {
    try {
      new DefaultSecurityTokenDecoder(new FakeContainerConfig("garbage"));
      fail("Should have thrown");
    } catch (RuntimeException e) {
      assertTrue("exception should contain garbage: " + e, e.getMessage().contains("garbage"));
    }
  }

  @Test
  public void testNullDecoder() throws Exception {
    try {
      new DefaultSecurityTokenDecoder(new FakeContainerConfig(null));
      fail("Should have thrown");
    } catch (RuntimeException e) {
      assertTrue("exception should contain null: " + e, e.getMessage().contains("null"));
    }
  }

  @Test
  public void testRealDecoder() throws Exception {
    // Just verifies that "secure" tokens get routed to the right decoder class.
    try {
      new DefaultSecurityTokenDecoder(new FakeContainerConfig("secure"));
      fail("Should have thrown");
    } catch (RuntimeException e) {
      assertTrue("root cause should have been FileNotFoundException: " + e,
          e.getMessage().contains("FileNotFoundException: container key file: somecontainer"));
    }
  }
}
