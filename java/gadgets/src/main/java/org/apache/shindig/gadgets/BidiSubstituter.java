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
package org.apache.shindig.gadgets;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides static hangman substitutions for bidirectional language support.
 * Useful for generating internationalized layouts using CSS.
 */

public class BidiSubstituter implements GadgetFeatureFactory {
  private final static GadgetFeature feature
      = new BidiSubstituterFeature();

  /**
   * {@inheritDoc}
   */
  public GadgetFeature create() {
    return feature;
  }
}

class BidiSubstituterFeature extends GadgetFeature {

  /**
   * Fetches a message bundle spec from the {@code GadgetSpec} for the
   * provided locale.
   * @param spec Gadget spec from which to retrieve message bundle spec
   * @param locale Locale of message bundle to retrieve
   * @return The message bundle, or null if not found
   */
  private GadgetSpec.LocaleSpec getLocaleSpec(GadgetSpec spec,
                                              Locale locale) {
    List<GadgetSpec.LocaleSpec> localeSpecs = spec.getLocaleSpecs();
    for (GadgetSpec.LocaleSpec locSpec : localeSpecs) {
      if (locSpec.getLocale().equals(locale)) {
        return locSpec;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    super.process(gadget, context, params);
    Substitutions subst = gadget.getSubstitutions();
    Locale locale = context.getLocale();
    // Find an appropriate locale for the ltr flag.
    GadgetSpec.LocaleSpec locSpec = getLocaleSpec(gadget, locale);
    if (locSpec == null) {
      locSpec = getLocaleSpec(gadget, new Locale(locale.getLanguage(), "all"));
    }
    if (locSpec == null) {
      locSpec = getLocaleSpec(gadget, new Locale("all", "all"));
    }
    boolean rtl = false;
    if (locSpec != null) {
      rtl = locSpec.isRightToLeft();
    }
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "START_EDGE",
                          rtl ? "right" : "left");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "END_EDGE",
                          rtl ? "left" : "right");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "DIR",
                          rtl ? "rtl" : "ltr");
    subst.addSubstitution(Substitutions.Type.BIDI,
                          "REVERSE_DIR",
                          rtl ? "ltr" : "rtl");
  }
}
