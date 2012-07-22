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

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.variables.Substitutions.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserPrefSubstituterTest extends Assert {
  private final Substitutions substituter = new Substitutions();
  private final static String DEFAULT_NAME = "default";
  private final static String DEFAULT_VALUE = "default value";
  private final static String USER_NAME = "user";
  private final static String USER_VALUE = "user value";
  private final static String OVERRIDE_NAME = "override";
  private final static String OVERRIDE_VALUE = "override value";
  private final static String UNESCAPED_USER_VALUE = "<hello, & world > \"";
  private final static String ESCAPED_USER_VALUE
      = "&lt;hello, &amp; world &gt; &quot;";
  private static final String DEFAULT_XML
      = "<Module>" +
        "<ModulePrefs title=\"Hello, __UP_world__\"/>" +
        "<UserPref name=\"" + DEFAULT_NAME + "\" datatype=\"string\"" +
        " default_value=\"" + DEFAULT_VALUE + "\"/>" +
        "<UserPref name=\"" + USER_NAME + "\" datatype=\"string\"/>" +
        "<UserPref name=\"" + OVERRIDE_NAME + "\" datatype=\"string\"" +
        "  default_value=\"FOOOOOOOOOOBAR!\"/>" +
        "<Content type=\"html\"/>" +
        "</Module>";
  private GadgetSpec spec;

  @Before
  public void setUp() throws Exception {
    spec = new GadgetSpec(Uri.parse("#"), DEFAULT_XML);
  }

  @Test
  public void testSubstitutions() throws Exception {
    Map<String, String> map = ImmutableMap.of(USER_NAME, USER_VALUE, OVERRIDE_NAME, OVERRIDE_VALUE);
    final UserPrefs prefs = new UserPrefs(map);
    GadgetContext context = new GadgetContext() {
        @Override
        public UserPrefs getUserPrefs() {
            return prefs;
        }
    };

    new UserPrefSubstituter().addSubstitutions(substituter, context, spec);

    assertEquals(DEFAULT_VALUE,
        substituter.getSubstitution(Type.USER_PREF, DEFAULT_NAME));
    assertEquals(USER_VALUE,
        substituter.getSubstitution(Type.USER_PREF, USER_NAME));
    assertEquals(OVERRIDE_VALUE,
        substituter.getSubstitution(Type.USER_PREF, OVERRIDE_NAME));
  }

  @Test
  public void testEscaping() throws Exception {
    Map<String, String> map = ImmutableMap.of(USER_NAME, UNESCAPED_USER_VALUE);
    final UserPrefs prefs = new UserPrefs(map);
    GadgetContext context = new GadgetContext() {
        @Override
        public UserPrefs getUserPrefs() {
            return prefs;
        }
    };

    new UserPrefSubstituter().addSubstitutions(substituter, context, spec);
    assertEquals(ESCAPED_USER_VALUE,
        substituter.getSubstitution(Type.USER_PREF, USER_NAME));
  }
}
