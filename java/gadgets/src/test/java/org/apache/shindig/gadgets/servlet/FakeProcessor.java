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
package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import java.util.List;
import java.util.Map;

public class FakeProcessor extends Processor {
  protected final Map<Uri, ProcessingException> exceptions = Maps.newHashMap();
  protected final Map<Uri, String> gadgets = Maps.newHashMap();
  public static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  public static final Uri SPEC_URL2 = Uri.parse("http://example.org/g2.xml");
  public static final Uri SPEC_URL3 = Uri.parse("http://example.org/g3.xml");
  public static final Uri SPEC_URL4 = Uri.parse("http://example.org/g4.xml");
  public static final String SPEC_TITLE = "JSON-TEST";
  public static final String SPEC_TITLE2 = "JSON-TEST2";
  public static final int PREFERRED_HEIGHT = 100;
  public static final int PREFERRED_WIDTH = 242;
  public static final String FEATURE1="core";
  public static final String FEATURE2="example-feature";
  public static final String FEATURE3="example-feature2";
  public static final String PARAM_NAME="param-name";
  public static final String PARAM_NAME2="param-name2";
  public static final String PARAM_VALUE="param-value";
  public static final String PARAM_VALUE2="param-value2";
  public static final String PARAM_VALUE3="param-value3";
  public static final String LINK_REL = "rel";
  public static final String LINK_HREF = "http://example.org/foo";
  public static final String SPEC_XML =
      "<Module>" +
      "<ModulePrefs title=\"" + SPEC_TITLE + "\">" +
      "  <Link rel='" + LINK_REL + "' href='" + LINK_HREF + "'/>" +
      "</ModulePrefs>" +
      "<UserPref name=\"up_one\">" +
      "  <EnumValue value=\"val1\" display_value=\"disp1\"/>" +
      "  <EnumValue value=\"abc\" display_value=\"disp2\"/>" +
      "  <EnumValue value=\"z_xabc\" display_value=\"disp3\"/>" +
      "  <EnumValue value=\"foo\" display_value=\"disp4\"/>" +
      "</UserPref>" +
      "<Content type=\"html\"" +
      " preferred_height = \"" + PREFERRED_HEIGHT + '\"' +
      " preferred_width = \"" + PREFERRED_WIDTH + '\"' +
      ">Hello, world</Content>" +
      "</Module>";

  public static final String SPEC_XML2 =
          "<Module>" +
          "<ModulePrefs title=\"" + SPEC_TITLE2 + "\"/>" +
          "<Content type=\"html\">Hello, world</Content>" +
          "</Module>";

  public static final String SPEC_XML3 =
      "<Module>" +
      "<ModulePrefs title=\"" + SPEC_TITLE2 + "\"/>" +
      "<Content name=\"canvas\">Hello, world</Content>" +
      "</Module>";

  public static final String SPEC_XML4 =
      "<Module>" +
      "<ModulePrefs title=\"" + SPEC_TITLE + "\">" +
      "  <Link rel='" + LINK_REL + "' href='" + LINK_HREF + "'/>" +
      "<Optional feature=\""+FEATURE2+"\">"+
      "<Param name=\""+PARAM_NAME+"\">"+PARAM_VALUE+"</Param>"+
      "<Param name=\""+PARAM_NAME+"\">"+PARAM_VALUE2+"</Param>"+
      "</Optional>"+
      "<Require feature=\""+FEATURE3+"\">"+
      "<Param name=\""+PARAM_NAME2+"\">"+PARAM_VALUE3+"</Param>"+
      "</Require>"+
      "</ModulePrefs>" +
      "<UserPref name=\"up_one\">" +
      "  <EnumValue value=\"val1\" display_value=\"disp1\"/>" +
      "  <EnumValue value=\"abc\" display_value=\"disp2\"/>" +
      "  <EnumValue value=\"z_xabc\" display_value=\"disp3\"/>" +
      "  <EnumValue value=\"foo\" display_value=\"disp4\"/>" +
      "</UserPref>" +
      "<Content type=\"html\"" +
      " preferred_height = \"" + PREFERRED_HEIGHT + '\"' +
      " preferred_width = \"" + PREFERRED_WIDTH + '\"' +
      ">Hello, world</Content>" +
      "</Module>";

  private final FeatureRegistry featureRegistry;

  public static final List<String> FEATURE_NAMES=ImmutableList.of(FEATURE1, FEATURE2, FEATURE3);

  public FakeProcessor() {
    this(null);
  }

  public FakeProcessor(FeatureRegistry featureRegistry) {
    super(null, null, null, null, null);
    this.gadgets.put(FakeProcessor.SPEC_URL, FakeProcessor.SPEC_XML);
    this.gadgets.put(FakeProcessor.SPEC_URL2, FakeProcessor.SPEC_XML2);
    this.gadgets.put(FakeProcessor.SPEC_URL3, FakeProcessor.SPEC_XML3);
    this.gadgets.put(FakeProcessor.SPEC_URL4, FakeProcessor.SPEC_XML4);
    this.featureRegistry = featureRegistry;
  }

  @Override
  public Gadget process(GadgetContext context) throws ProcessingException {

    ProcessingException exception = exceptions.get(context.getUrl());
    if (exception != null) {
      throw exception;
    }

    try {
      GadgetSpec spec = new GadgetSpec(context.getUrl(), gadgets.get(context.getUrl()));
      View view = spec.getView(context.getView());
      return new Gadget()
          .setContext(context)
          .setSpec(spec)
          .setCurrentView(view)
          .setGadgetFeatureRegistry(featureRegistry);
    } catch (GadgetException e) {
      throw new RuntimeException(e);
    }
  }
}
