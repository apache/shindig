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

import static org.easymock.EasyMock.createControl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.easymock.IMocksControl;

import org.junit.Test;

public class AnonFuncWrappingProcessorTest {
  @Test
  public void wrapCode() throws Exception {
    IMocksControl control = createControl();
    JsRequest request = control.createMock(JsRequest.class);
    JsResponseBuilder builder = new JsResponseBuilder().appendJs("JS_CODE", "source");
    AnonFuncWrappingProcessor processor = new AnonFuncWrappingProcessor();
    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    assertEquals("(function() {\nJS_CODE\n})();", builder.build().toJsString());
  }
}
