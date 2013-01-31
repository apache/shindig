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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.variables.Substitutions.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ModuleSubstituterTest extends Assert {
  private final Substitutions substitutions = new Substitutions();
  private GadgetSpec spec;

  @Before
  public void setUp() throws Exception {
    spec = new GadgetSpec(Uri.parse("#"), "<Module><ModulePrefs title='' /><Content /></Module>");
  }

  @Test
  public void testDefault() throws Exception {
    ModuleSubstituter substituter = new ModuleSubstituter();
    substituter.addSubstitutions(substitutions, new GadgetContext(), spec);

    assertEquals("0",
        substitutions.getSubstitution(Type.MODULE, "ID"));
  }

  @Test
  public void testSpecific() throws Exception {
    final long moduleId = 12345678L;

    ModuleSubstituter substituter = new ModuleSubstituter();
    substituter.addSubstitutions(substitutions, new GadgetContext() {
        @Override
        public long getModuleId() {
            return moduleId;
        }
    }, spec);

    assertEquals(Long.toString(moduleId),
        substitutions.getSubstitution(Type.MODULE, "ID"));
  }
}
