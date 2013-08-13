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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.render.FakeMessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.Assert;
import org.junit.Test;

public class MessageSubstituterTest extends Assert {
  private final FakeMessageBundleFactory messageBundleFactory = new FakeMessageBundleFactory();
  private final MessageSubstituter substituter = new MessageSubstituter(messageBundleFactory);

  private final GadgetContext context = new GadgetContext();

  @Test
  public void testMessageReplacements() throws Exception {
    String xml =
        "<Module>" +
        " <ModulePrefs title=''>" +
        "  <Locale>" +
        "    <msg name='foo'>bar</msg>" +
        "    <msg name='bar'>baz</msg>" +
        "  </Locale>" +
        " </ModulePrefs>" +
        " <Content />" +
        "</Module>";

    Substitutions substitutions = new Substitutions();
    substituter.addSubstitutions(substitutions, context, new GadgetSpec(Uri.parse("#"), xml));

    assertEquals("bar", substitutions.getSubstitution(Type.MESSAGE, "foo"));
    assertEquals("baz", substitutions.getSubstitution(Type.MESSAGE, "bar"));
  }
}
