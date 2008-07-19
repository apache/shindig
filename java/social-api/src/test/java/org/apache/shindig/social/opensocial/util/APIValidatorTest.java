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
package org.apache.shindig.social.opensocial.util;


import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Test the api validator to make certain it behaves as is expected.
 */
public class APIValidatorTest {

  /**
   * A definition of the test json used in this unit test.
   */
  private static final String TEST_DEFINITION = 
    "var TestDef = {}; TestDef.Field = { FIELD1 : \"json\", FIELD2 : \"xyz\", FIELD3 : \"shouldBeMissing\" };";

  /**
   * test the validator for successful validation
   * @throws APIValidatorExpcetion
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  @Test
  public void testValidator() throws APIValidatorExpcetion, IOException, ParserConfigurationException, SAXException {
    APIValidator apiVal = new APIValidator("opensocial-reference");
    apiVal.addScript(TEST_DEFINITION);
    String[] optional = {"shouldBeMissing"};
    String[] nullfields = {};
     Map<String, Object> result = apiVal.validate("{ json: \"A Test JSON\", xyz : 123 }", "TestDef.Field", optional, nullfields );
     Assert.assertNotNull(result);
     Assert.assertNotNull(result.get("json"));
     Assert.assertNotNull(result.get("xyz"));
     Assert.assertEquals(String.class,result.get("json").getClass());
     Assert.assertEquals(Integer.class,result.get("xyz").getClass());
     Assert.assertEquals("A Test JSON",result.get("json"));
     Assert.assertEquals(123,((Integer)result.get("xyz")).intValue());
     
    
  }
  
  /**
   * Test for a failing validation
   * @throws APIValidatorExpcetion
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  @Test
  public void testValidatorFail() throws APIValidatorExpcetion, IOException, ParserConfigurationException, SAXException {
    APIValidator apiVal = new APIValidator("opensocial-reference");
    apiVal.addScript(TEST_DEFINITION);
    String[] optional = {};
    String[] nullfields = {};
    try {
      apiVal.validate("{ jsonIsMissing: \"A Test JSON\", xyz : 123 }", "TestDef.Field", optional, nullfields );
      Assert.fail("Should have Generated an APIValidatorException ");
    } catch ( APIValidatorExpcetion ex ) {
      
    }
     
    
  }

}
