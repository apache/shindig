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

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SeparatorCommentingProcessorTest {
  private static final List<String> ERRORS = ImmutableList.<String>of();

  private IMocksControl control;
  private SeparatorCommentingProcessor processor;
  private JsResponse response;

  @Before
  public void setUp() {
    control = createControl();
    processor = new SeparatorCommentingProcessor();
  }

  @Test
  public void testNoFeature() throws Exception {
    JsResponseBuilder builder = newBuilder();

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals("", actualResponse.toJsString());
  }

  @Test
  public void testOneFeature() throws Exception {
    JsContent js = JsContent.fromFeature("content", "source", mockBundle("bundle"), null);
    JsResponseBuilder builder = newBuilder(js);

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(
        "\n/* [start] feature=bundle */\n" +
        "content" +
        "\n/* [end] feature=bundle */\n",
        actualResponse.toJsString());
  }

  @Test
  public void testOneText() throws Exception {
    JsContent text1 = JsContent.fromText("text1", "source");
    JsResponseBuilder builder = newBuilder(text1);

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals("text1", actualResponse.toJsString());
  }

  @Test
  public void testMultipleFeaturesWithoutInBetweenTexts() throws Exception {
    JsContent js1 = JsContent.fromFeature("content1", "source1", mockBundle("bundle1"), null);
    JsContent js2 = JsContent.fromFeature("content2", "source2", mockBundle("bundle2"), null);
    JsResponseBuilder builder = newBuilder(js1, js2);

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(
        "\n/* [start] feature=bundle1 */\n" +
        "content1" +
        "\n/* [end] feature=bundle1 */\n" +
        "\n/* [start] feature=bundle2 */\n" +
        "content2" +
        "\n/* [end] feature=bundle2 */\n",
        actualResponse.toJsString());
  }

  @Test
  public void testNeighboringSameFeatures() throws Exception {
    FeatureBundle bundle = mockBundle("bundle");
    JsContent js1 = JsContent.fromFeature("content1", "source1", bundle, null);
    JsContent js2 = JsContent.fromFeature("content2", "source2", bundle, null);
    JsResponseBuilder builder = newBuilder(js1, js2);

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(
        "\n/* [start] feature=bundle */\n" +
        "content1" +
        "content2" +
        "\n/* [end] feature=bundle */\n",
        actualResponse.toJsString());
  }

  @Test
  public void testMultipleFeaturesWithInBetweenTexts() throws Exception {
    JsContent text1 = JsContent.fromText("text1", "source1");
    JsContent text2 = JsContent.fromText("text2", "source2");
    JsContent text3 = JsContent.fromText("text3", "source3");
    JsContent js1 = JsContent.fromFeature("content1", "source4", mockBundle("bundle1"), null);
    JsContent js2 = JsContent.fromFeature("content2", "source5", mockBundle("bundle2"), null);
    JsResponseBuilder builder = newBuilder(text1, js1, text2, js2, text3);

    control.replay();
    boolean actualReturn = processor.process(null, builder);
    JsResponse actualResponse = builder.build();

    control.verify();
    assertTrue(actualReturn);
    assertEquals(
        "text1" +
        "\n/* [start] feature=bundle1 */\n" +
        "content1" +
        "\n/* [end] feature=bundle1 */\n" +
        "text2" +
        "\n/* [start] feature=bundle2 */\n" +
        "content2" +
        "\n/* [end] feature=bundle2 */\n" +
        "text3",
        actualResponse.toJsString());
  }

  private JsResponseBuilder newBuilder(JsContent... contents) {
    response = new JsResponse(Lists.newArrayList(contents),
        -1, -1, false, ERRORS, null);
    return new JsResponseBuilder(response);
  }

  private FeatureBundle mockBundle(String name) {
    FeatureBundle result = control.createMock(FeatureBundle.class);
    expect(result.getName()).andReturn(name).anyTimes();
    return result;
  }

}
