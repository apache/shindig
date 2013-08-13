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
 * Provides static hangman substitutions for bidirectional language support.
 * Useful for generating internationalized layouts using CSS.
 */
public class BidiSubstituter implements Substituter {
  public static final String START_EDGE = "START_EDGE";
  public static final String END_EDGE = "END_EDGE";
  public static final String DIR = "DIR";
  public static final String REVERSE_DIR = "REVERSE_DIR";

  public static final String RIGHT = "right";
  public static final String LEFT = "left";
  public static final String RTL = "rtl";
  public static final String LTR = "ltr";

  private final MessageBundleFactory messageBundleFactory;

  @Inject
  public BidiSubstituter(MessageBundleFactory messageBundleFactory) {
    this.messageBundleFactory = messageBundleFactory;
  }

  public void addSubstitutions(Substitutions substituter, GadgetContext context, GadgetSpec spec)
      throws GadgetException {
    MessageBundle bundle =
        messageBundleFactory.getBundle(spec, context.getLocale(), context.getIgnoreCache(),
                    context.getContainer(), context.getView());
    String dir = bundle.getLanguageDirection();

    boolean rtl = RTL.equals(dir);
    substituter.addSubstitution(Substitutions.Type.BIDI, START_EDGE, rtl ? RIGHT : LEFT);
    substituter.addSubstitution(Substitutions.Type.BIDI, END_EDGE, rtl ? LEFT : RIGHT);
    substituter.addSubstitution(Substitutions.Type.BIDI, DIR, rtl ? RTL : LTR);
    substituter.addSubstitution(Substitutions.Type.BIDI, REVERSE_DIR, rtl ? LTR : RTL);
  }
}
