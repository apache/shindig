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
package org.apache.shindig.social.opensocial.util;

import org.apache.shindig.common.xml.XmlException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the XSDValidator
 */
public class XSDValidatorTest extends Assert {

  /**
   * Check that the XSDValidator is working correctly by testing for pass and fail
   * @throws XmlException
   */
  @Test
  public void testValidator() throws XmlException {
    assertEquals("", XSDValidator.validate(this.getClass().getResourceAsStream("testschema1.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
    assertNotSame("", XSDValidator.validate(this.getClass().getResourceAsStream("testschema1bad.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
  }

  @Test
  public void testPerson1() throws XmlException {
    assertEquals("", XSDValidator.validate(this.getClass().getResourceAsStream("testxml/person1.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
  }

  @Test
  public void testActivity1() throws XmlException {
    assertEquals("", XSDValidator.validate(this.getClass().getResourceAsStream("testxml/activity1.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
  }

  @Test
  public void testAppdata1() throws XmlException {
    assertEquals("", XSDValidator.validate(this.getClass().getResourceAsStream("testxml/appdata1.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
  }

  @Test
  public void testGroup1() throws XmlException {
    assertEquals("", XSDValidator.validate(this.getClass().getResourceAsStream("testxml/group1.xml"),this.getClass().getResourceAsStream("opensocial.xsd")));
  }
}
