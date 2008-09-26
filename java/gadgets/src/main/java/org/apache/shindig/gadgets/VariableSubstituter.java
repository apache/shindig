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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.inject.Inject;

/**
 * Performs variable substitution on a gadget spec.
 */
public class VariableSubstituter {
  private final MessageBundleFactory messageBundleFactory;

  @Inject
  public VariableSubstituter(MessageBundleFactory messageBundleFactory) {
    this.messageBundleFactory = messageBundleFactory;
  }

  /**
   * Substitutes all hangman variables into the gadget spec.
   *
   * @return A new GadgetSpec, with all fields substituted as needed.
   */
  public GadgetSpec substitute(GadgetContext context, GadgetSpec spec) throws GadgetException {
    MessageBundle bundle =
        messageBundleFactory.getBundle(spec, context.getLocale(), context.getIgnoreCache());
    String dir = bundle.getLanguageDirection();

    Substitutions substituter = new Substitutions();
    substituter.addSubstitutions(Substitutions.Type.MESSAGE, bundle.getMessages());
    BidiSubstituter.addSubstitutions(substituter, dir);
    substituter.addSubstitution(Substitutions.Type.MODULE, "ID",
        Integer.toString(context.getModuleId()));
    UserPrefSubstituter.addSubstitutions(substituter, spec, context.getUserPrefs());

    return spec.substitute(substituter);
  }
}
