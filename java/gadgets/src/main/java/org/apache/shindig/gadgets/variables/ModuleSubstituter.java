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

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.spec.GadgetSpec;

/**
 * Provides hangman substitution variables related to the Module (i.e. __MODULE_ID__)
 *
 * @since 2.0.0
 */
public class ModuleSubstituter implements Substituter {
  public void addSubstitutions(Substitutions substituter, GadgetContext context, GadgetSpec spec)
        throws GadgetException {
    substituter.addSubstitution(Substitutions.Type.MODULE, "ID", Long.toString(context.getModuleId()));
  }
}
