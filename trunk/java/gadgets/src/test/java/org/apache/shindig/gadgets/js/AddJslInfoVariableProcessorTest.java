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
package org.apache.shindig.gadgets.js;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;


/**
 * Tests for {@link AddJslInfoVariableProcessor}.
 */
public class AddJslInfoVariableProcessorTest {

  private static final String URI = "http://localhost";
  private static final List<String> LIBS = ImmutableList.of("fo'o", "bar", "baz");
  private static final String LIBS_JS = "'bar','baz','fo\\'o'";

  private IMocksControl control;
  private JsRequest request;
  private JsUri jsUri;
  private JsResponseBuilder response;
  private AddJslInfoVariableProcessor processor;
  private final FeatureRegistryProvider fregProvider = EasyMock.createMock(FeatureRegistryProvider.class);
  private final FeatureRegistry freg = EasyMock.createMock(FeatureRegistry.class);

  @Before
  public void setUp() throws GadgetException {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor = new AddJslInfoVariableProcessor(fregProvider);

    EasyMock.reset(fregProvider, freg);
    EasyMock.expect(fregProvider.get(EasyMock.anyObject(String.class))).andReturn(freg).anyTimes();

    Capture<List<String>> features = new Capture<List<String>>();
    EasyMock.expect(freg.getFeatures(EasyMock.capture(features))).andAnswer(new IAnswer<List<String>>() {
      public List<String> answer() throws Throwable {
        return LIBS;
      }
    });

    EasyMock.replay(fregProvider, freg);
  }

  @Test
  public void skipsWhenNohintIsTrue() throws Exception {
    setJsUri(URI + "?nohint=1");
    control.replay();
    processor.process(request, response);
    assertEquals("", response.build().toJsString());
    control.verify();
  }

  @Test
  public void featureInfo() throws Exception {
    setJsUri(URI);
    control.replay();
    processor.process(request, response);
    String expected = String.format(AddJslInfoVariableProcessor.BASE_HINT_TEMPLATE +
        AddJslInfoVariableProcessor.FEATURES_HINT_TEMPLATE, LIBS_JS);
    assertEquals(expected, response.build().toJsString());
    control.verify();
  }

  private void setJsUri(String uri) {
    jsUri = new JsUri(UriStatus.VALID_UNVERSIONED, Uri.parse(uri), LIBS, null);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
  }
}
