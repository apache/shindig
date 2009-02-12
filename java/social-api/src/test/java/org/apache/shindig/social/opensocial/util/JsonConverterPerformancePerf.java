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

import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.conversion.BeanJsonLibConverter;
import org.apache.shindig.protocol.conversion.jsonlib.JsonLibTestsGuiceModule;
import org.apache.shindig.social.SocialApiTestsGuiceModule;
import org.apache.shindig.social.core.model.ActivityImpl;
import org.apache.shindig.social.core.model.AddressImpl;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.core.model.MediaItemImpl;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Person;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class JsonConverterPerformancePerf extends TestCase {

  private static final Log log = LogFactory.getLog(JsonConverterPerformancePerf.class);
  private static final int TEST_SIZE = 10000;
  private Person johnDoe;
  private Activity activity;

  private BeanJsonLibConverter beanJsonLibConverter;
  private BeanJsonConverter beanJsonConverter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    johnDoe = new PersonImpl("johnDoeId", "Johnny", new NameImpl("John Doe"));
    johnDoe.setPhoneNumbers(Lists.<ListField> newArrayList(new ListFieldImpl("home",
        "+33H000000000"), new ListFieldImpl("mobile", "+33M000000000"), new ListFieldImpl("work",
        "+33W000000000")));

    johnDoe.setAddresses(Lists.<Address> newArrayList(new AddressImpl("My home address")));

    johnDoe.setEmails(Lists.<ListField> newArrayList(
        new ListFieldImpl("work", "john.doe@work.bar"), new ListFieldImpl("home",
            "john.doe@home.bar")));

    activity = new ActivityImpl("activityId", johnDoe.getId());

    activity.setMediaItems(Lists.<MediaItem> newArrayList(new MediaItemImpl("image/jpg",
        MediaItem.Type.IMAGE, "http://foo.bar")));

    Injector injector = Guice.createInjector(new JsonLibTestsGuiceModule());
    beanJsonLibConverter = injector.getInstance(BeanJsonLibConverter.class);

    beanJsonConverter = new BeanJsonConverter(
        Guice.createInjector(new SocialApiTestsGuiceModule()));

  }

  public static class SpecialPerson extends PersonImpl {
    private String newfield;

    public SpecialPerson() {
      super();
    }

    public SpecialPerson(String id, String name, String newfield) {
      super(id, name, new NameImpl(name));
      this.newfield = newfield;
    }

    public String getNewfield() {
      return newfield;
    }

    public void setNewfield(String newfield) {
      this.newfield = newfield;
    }

  }

  public void testToJsonLibOnInheritedClassOutput() throws Exception {
    SpecialPerson[] spa = new SpecialPerson[TEST_SIZE];
    for (int i = 0; i < TEST_SIZE; i++) {
      spa[i] = new SpecialPerson(String.valueOf(i), "robot", "nonsense");
    }
    Runtime r = Runtime.getRuntime();
    r.gc();
    long memstart = r.totalMemory() - r.freeMemory();
    long startOutput = System.currentTimeMillis();
    String[] output = new String[TEST_SIZE];
    for (int i = 0; i < TEST_SIZE; i++) {
      output[i] = beanJsonLibConverter.convertToString(spa[i]);
    }
    long endOutput = System.currentTimeMillis();
    long memend = r.totalMemory() - r.freeMemory();

    String[] serializeOutput = new String[TEST_SIZE];
    char[] source = output[0].toCharArray();
    r.gc();

    long stringsizeStart = r.totalMemory() - r.freeMemory();
    for (int i = 0; i < TEST_SIZE; i++) {
      serializeOutput[i] = new String(source);
    }
    long stringsizeEnd = r.totalMemory() - r.freeMemory();

    /*
     * Output the time per conversion and the memory usage - the output per
     * conversion.
     *
     */

    log
        .info("SF JSON Lib Output "
            + average(startOutput, endOutput, TEST_SIZE)
            + " ms/conversion, "
            + (average(memstart, memend, TEST_SIZE) - average(stringsizeStart, stringsizeEnd,
                TEST_SIZE)) + " heap bytes/conversion, output packet consumed on average "
            + average(stringsizeStart, stringsizeEnd, TEST_SIZE) + " for a string length of "
            + output[0].length());
    log.info("Output Was [" + output[0] + ']');
  }

  public void testToJsonLibOnInheritedClassInput() throws Exception {
    SpecialPerson[] spa = new SpecialPerson[TEST_SIZE];
    SpecialPerson[] sparesult = new SpecialPerson[TEST_SIZE];
    Runtime r = Runtime.getRuntime();
    r.gc();
    long personStart = r.totalMemory() - r.freeMemory();
    for (int i = 0; i < TEST_SIZE; i++) {
      spa[i] = new SpecialPerson(String.valueOf(i), "robot", "nonsense");
    }
    long personEnd = r.totalMemory() - r.freeMemory();

    String[] serializeOutput = new String[TEST_SIZE];
    r.gc();
    for (int i = 0; i < TEST_SIZE; i++) {

      serializeOutput[i] = beanJsonLibConverter.convertToString(spa[i]);
    }

    r.gc();
    long memstart = r.totalMemory() - r.freeMemory();
    long startInput = System.currentTimeMillis();
    for (int i = 0; i < TEST_SIZE; i++) {
      sparesult[i] = beanJsonLibConverter.convertToObject(serializeOutput[i], SpecialPerson.class);
    }
    long endInput = System.currentTimeMillis();
    long memend = r.totalMemory() - r.freeMemory();

    log.info("SF JSON Lib Input " + average(startInput, endInput, TEST_SIZE) + " ms/conversion, "
        + (average(memstart, memend, TEST_SIZE) - average(personStart, personEnd, TEST_SIZE))
        + " heap bytes/conversion, person object consumed on average "
        + average(personStart, personEnd, TEST_SIZE));
  }

//  public void testToJsonOnInheritedClassOutput() throws Exception {
//    SpecialPerson[] spa = new SpecialPerson[TEST_SIZE];
//    for (int i = 0; i < TEST_SIZE; i++) {
//      spa[i] = new SpecialPerson(String.valueOf(i), "robot", "nonsense");
//    }
//    Runtime r = Runtime.getRuntime();
//    String[] output = new String[TEST_SIZE];
//    r.gc();
//    long memstart = r.totalMemory() - r.freeMemory();
//    long startOutput = System.currentTimeMillis();
//    for (int i = 0; i < TEST_SIZE; i++) {
//      output[i] = ((JSONObject) beanJsonConverter.convertToJson(spa[i])).toString();
//    }
//    long endOutput = System.currentTimeMillis();
//    long memend = r.totalMemory() - r.freeMemory();
//    String[] serializeOutput = new String[TEST_SIZE];
//    char[] source = output[0].toCharArray();
//    r.gc();
//
//    long stringsizeStart = r.totalMemory() - r.freeMemory();
//
//    for (int i = 0; i < TEST_SIZE; i++) {
//      serializeOutput[i] = new String(source);
//    }
//    long stringsizeEnd = r.totalMemory() - r.freeMemory();
//
//    log
//        .info("ORG JSON Lib Output "
//            + average(startOutput, endOutput, TEST_SIZE)
//            + " ms/conversion, "
//            + (average(memstart, memend, TEST_SIZE) - average(stringsizeStart, stringsizeEnd,
//                TEST_SIZE)) + " heap bytes/conversion, output packet consumed on average "
//            + average(stringsizeStart, stringsizeEnd, TEST_SIZE) + " for a string length of "
//            + output[0].length());
//    log.info("Output Was [" + output[0] + ']');
//  }

  /**
   * @param endOutput
   * @param startOutput
   * @param testSize
   * @return
   */
  private float average(long start, long end, int testSize) {
    float r = end - start;
    r = r / testSize;
    return r;
  }

  public void XtestToJsonOnInheritedClassInput() throws Exception {
    SpecialPerson[] spa = new SpecialPerson[TEST_SIZE];
    SpecialPerson[] sparesult = new SpecialPerson[TEST_SIZE];
    Runtime r = Runtime.getRuntime();
    r.gc();
    long personStart = r.totalMemory() - r.freeMemory();
    for (int i = 0; i < TEST_SIZE; i++) {
      spa[i] = new SpecialPerson(String.valueOf(i), "robot", "nonsense");
    }
    long personEnd = r.totalMemory() - r.freeMemory();

    String[] serializeOutput = new String[TEST_SIZE];
    r.gc();
    for (int i = 0; i < TEST_SIZE; i++) {

      serializeOutput[i] = beanJsonConverter.convertToString(spa[i]);
    }

    r.gc();
    long memstart = r.totalMemory() - r.freeMemory();
    long startInput = System.currentTimeMillis();

    for (int i = 0; i < TEST_SIZE; i++) {
      sparesult[i] = beanJsonConverter.convertToObject(serializeOutput[i], SpecialPerson.class);
    }
    long endInput = System.currentTimeMillis();
    long memend = r.totalMemory() - r.freeMemory();

    log.info("SF JSON Lib Input " + average(startInput, endInput, TEST_SIZE) + " ms/conversion, "
        + (average(memstart, memend, TEST_SIZE) - average(personStart, personEnd, TEST_SIZE))
        + " heap bytes/conversion, person object consumed on average "
        + average(personStart, personEnd, TEST_SIZE));
  }

}
