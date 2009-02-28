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
package org.apache.shindig.gadgets.variables;

import org.apache.shindig.gadgets.variables.Substitutions.Type;

import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;

public class SubstitutionsTest extends TestCase {
  private Substitutions subst;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    subst = new Substitutions();
  }

  public void testMessages() throws Exception {
    String msg = "Hello, __MSG_world__!";
    subst.addSubstitution(Type.MESSAGE, "world", "planet");
    assertEquals("Hello, planet!", subst.substituteString(msg));
  }

  public void testBidi() throws Exception {
    String msg = "Hello, __BIDI_DIR__-world!";
    subst.addSubstitution(Type.BIDI, "DIR", "rtl");
    assertEquals("Hello, rtl-world!", subst.substituteString(msg));
  }

  public void testUserPref() throws Exception {
    String msg = "__UP_hello__, world!";
    subst.addSubstitution(Type.USER_PREF, "hello", "Greetings");
    assertEquals("Greetings, world!", subst.substituteString(msg));
  }

  public void testCorrectOrder() throws Exception {
    String msg = "__UP_hello__, __MSG_world__!";
    subst.addSubstitution(Type.MESSAGE, "world",
        "planet __BIDI_DIR__-__UP_planet__");
    subst.addSubstitution(Type.BIDI, "DIR", "rtl");
    subst.addSubstitution(Type.USER_PREF, "hello", "Greetings");
    subst.addSubstitution(Type.USER_PREF, "planet", "Earth");
    assertEquals("Greetings, planet rtl-Earth!", subst.substituteString(msg));
  }

  public void testIncorrectOrder() throws Exception {
    String msg = "__UP_hello__, __MSG_world__";
    subst.addSubstitution(Type.MESSAGE, "world",
        "planet __MSG_earth____UP_punc__");
    subst.addSubstitution(Type.MESSAGE, "earth", "Earth");
    subst.addSubstitution(Type.USER_PREF, "punc", "???");
    subst.addSubstitution(Type.USER_PREF, "hello",
        "Greetings __MSG_foo____UP_bar__");
    subst.addSubstitution(Type.MESSAGE, "foo", "FOO!!!");
    subst.addSubstitution(Type.USER_PREF, "bar", "BAR!!!");
    assertEquals("Greetings __MSG_foo____UP_bar__, planet __MSG_earth__???",
        subst.substituteString(msg));
  }

  public void testDanglingUnderScoresAreIgnored() throws Exception {
    String msg = "__MSG_hello__, var_msg + '__' + 'world __MSG_world__";
    subst.addSubstitution(Type.MESSAGE, "hello", "Hello");
    subst.addSubstitution(Type.MESSAGE, "world", "World");

    assertEquals("Hello, var_msg + '__' + 'world World", subst.substituteString(msg));
  }

  public void testComplexUnderscores() throws Exception {
    String msg = "__MSG_hello____________ten____________MSG_world______";
    subst.addSubstitution(Type.MESSAGE, "hello", "Hello");
    subst.addSubstitution(Type.MESSAGE, "world", "World");

    assertEquals("Hello__________ten__________World____", subst.substituteString(msg));
  }

  public void testMessageId() throws Exception {
    String msg = "Hello, __MODULE_ID__!";
    subst.addSubstitution(Type.MODULE, "ID", "123");
    assertEquals("Hello, 123!", subst.substituteString(msg));
  }

  public void testOddNumberOfPrecedingUnderscores() throws Exception {
    String msg = "<div id='div___MODULE_ID__'/>";
    subst.addSubstitution(Type.MODULE, "ID", "123");
    assertEquals("<div id='div_123'/>", subst.substituteString(msg));
  }

  public void testOddUnderscoresWithInvalidSubstFollowedByValidSubst() throws Exception {
    String msg = "<div id='div___HI_THERE__MODULE_ID___'/>";
    subst.addSubstitution(Type.MODULE, "ID", "123");
    assertEquals("<div id='div___HI_THERE123_'/>", subst.substituteString(msg));
  }

  public void loadTest() throws Exception {
    String msg
        = "Random text and __UP_hello__, amongst other words __MSG_world__ stuff __weeeeee";
    subst.addSubstitution(Type.MESSAGE, "world",
        "planet __MSG_earth____UP_punc__");
    subst.addSubstitution(Type.MESSAGE, "earth", "Earth");
    subst.addSubstitution(Type.USER_PREF, "punc", "???");
    subst.addSubstitution(Type.USER_PREF, "hello",
        "Greetings __MSG_foo____UP_bar__");
    subst.addSubstitution(Type.MESSAGE, "foo", "FOO!!!");
    subst.addSubstitution(Type.USER_PREF, "bar", "BAR!!!");

    // Most real-world content contains very few substitutions.
    msg += StringUtils.repeat("foo ", 1000);

    String message = StringUtils.repeat(msg, 1000);

    long now = System.nanoTime();
    int cnt = 1000;
    for (int i = 0; i < cnt; ++i) {
      subst.substituteString(message);
    }
    long duration = System.nanoTime() - now;
    System.out.println("Duration: " + duration + " avg: " + duration / cnt);
  }
}
