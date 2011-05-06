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

package org.apache.shindig.gadgets.js;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;


/**
 * Tests for {@link AddJslInfoVariableProcessor}.
 */
public class AddJslInfoVariableProcessorTest {

  private static final String URI = "http://localhost";
  private static final List<String> LIBS = ImmutableList.of("fo'o", "bar", "baz");
  private static final String GENERATED_URI = "http://localhost?nohint=1";
  private static final String URI_JS = "http:\\/\\/localhost";
  private static final String LIBS_JS = "'fo\\'o','bar','baz'";
  private static final String GENERATED_URI_JS = "http:\\/\\/localhost?nohint=1";

  private IMocksControl control;
  private JsRequest request;
  private JsUriManager jsUriManager;
  private JsUri jsUri;
  private JsResponseBuilder response;
  private AddJslInfoVariableProcessor processor;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    request = control.createMock(JsRequest.class);
    jsUriManager = control.createMock(JsUriManager.class);
    response = new JsResponseBuilder();
    processor = new AddJslInfoVariableProcessor(jsUriManager);
  }

  @Test
  public void testSkipsWhenNohintIsTrue() throws Exception {
    response = control.createMock(JsResponseBuilder.class);
    setJsUri(URI + "?nohint=1");
    control.replay();
    processor.process(request, response);
    control.verify();
  }
  
  @Test
  public void testAddsHint() throws Exception {
    setJsUri(URI);
    control.replay();
    processor.process(request, response);
    String expected = String.format(AddJslInfoVariableProcessor.HINT_TEMPLATE, URI_JS, LIBS_JS);
    assertEquals(expected, response.build().toJsString());
    control.verify();    
  }

  @Test
  public void testAddsHintWithoutJsLoad() throws Exception {
    setJsUri(URI + "?jsload=1");
    Capture<JsUri> captureJsUri = new Capture<JsUri>();
    EasyMock.expect(jsUriManager.makeExternJsUri(EasyMock.capture(captureJsUri))).andReturn(Uri.parse(GENERATED_URI));
    control.replay();
    processor.process(request, response);
    String expected = String.format(AddJslInfoVariableProcessor.HINT_TEMPLATE, GENERATED_URI_JS, LIBS_JS);
    assertEquals(expected, response.build().toJsString());
    assertFalse(captureJsUri.getValue().isJsload());
    assertTrue(captureJsUri.getValue().isNohint());
    control.verify();    
  }
  

  private void setJsUri(String uri) {
    jsUri = new JsUri(UriStatus.VALID_UNVERSIONED, Uri.parse(uri), LIBS, null);
    EasyMock.expect(request.getJsUri()).andReturn(jsUri);
  }
}
