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

import org.apache.shindig.gadgets.GadgetException;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;

/**
 * Substituter that provides variables to {@link VariableSubstituter}.
 *
 * @since 2.0.0
 */
public interface Substituter {

  /**
   * Add the substitutions from this Substituter to the {@link Substitutions}.
   *
   * @param substituter container for the new substitutions, containing any existing substitutions
   * @param context the context in which this gadget is being rendered
   * @param spec the gadget specification being substituted
   * @throws GadgetException when there has been a general error adding substitutions
   */
  void addSubstitutions(Substitutions substituter, GadgetContext context, GadgetSpec spec) throws GadgetException;

}
