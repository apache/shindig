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
package org.apache.shindig.common.servlet;

import org.junit.Assert;
import org.junit.Test;

public class UserAgentTest extends Assert {
  private UserAgent getUaEntry(String version) {
    return new UserAgent(UserAgent.Browser.OTHER, version);
  }

  @Test
  public void testVersionNumberParsingStandard() {
    assertEquals(3D, getUaEntry("3").getVersionNumber(), 0);
  }

  @Test
  public void testVersionNumberParsingStandardDecimal() {
    assertEquals(3.1415, getUaEntry("3.1415").getVersionNumber(), 0);
  }

  @Test
  public void testVersionNumberParsingMultiPart() {
    assertEquals(3.1, getUaEntry("3.1.5").getVersionNumber(), 0);
  }

  @Test
  public void testVersionNumberParsingAlphaSuffix() {
    assertEquals(4.5, getUaEntry("4.5beta2").getVersionNumber(), 0);
  }

  @Test
  public void testVersionNumberParsingEmbeddedInTheMiddle() {
    assertEquals(1.5, getUaEntry("beta 1.5 rc 5").getVersionNumber(), 0);
  }

  @Test
  public void testVersionNumberParsingNoMatch() {
    assertEquals(-1, getUaEntry("invalid").getVersionNumber(), 0);
  }
}
