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

import com.google.inject.Inject;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

/**
 * Provides static hangman substitutions for message bundles.
 *
 * @since 2.0.0
 */
public class MessageSubstituter implements Substituter {
  private final MessageBundleFactory messageBundleFactory;

  @Inject
  public MessageSubstituter(MessageBundleFactory messageBundleFactory) {
    this.messageBundleFactory = messageBundleFactory;
  }

  public void addSubstitutions(Substitutions substituter, GadgetContext context, GadgetSpec spec)
          throws GadgetException {
    MessageBundle bundle = messageBundleFactory.getBundle(spec, context.getLocale(),
        context.getIgnoreCache(), context.getContainer(), context.getView());

    substituter.addSubstitutions(Substitutions.Type.MESSAGE, bundle.getMessages());
  }
}
