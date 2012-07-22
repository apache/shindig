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

import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.IMocksControl;

import org.junit.Test;

public class AnonFuncWrappingProcessorTest {
  @Test
  public void wrapCodeAllRunTime() throws Exception {
    checkWrapCode(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, true);
  }

  @Test
  public void wrapCodeExplicitRunTime() throws Exception {
    checkWrapCode(JsCompileMode.CONCAT_COMPILE_EXPORT_EXPLICIT, true);
  }

  @Test
  public void wrapCodeBuildTimeDoesNothing() throws Exception {
    checkWrapCode(JsCompileMode.COMPILE_CONCAT, false);
  }

  private void checkWrapCode(JsCompileMode mode, boolean wraps) throws Exception {
    IMocksControl control = createControl();
    JsRequest request = control.createMock(JsRequest.class);
    JsUri jsUri = control.createMock(JsUri.class);
    expect(jsUri.getCompileMode()).andReturn(mode);
    expect(request.getJsUri()).andReturn(jsUri);
    JsResponseBuilder builder = new JsResponseBuilder().appendJs("JS_CODE", "source");
    AnonFuncWrappingProcessor processor = new AnonFuncWrappingProcessor();
    control.replay();
    assertTrue(processor.process(request, builder));
    control.verify();
    if (wraps) {
      assertEquals("(function(){JS_CODE})();", builder.build().toJsString());
    } else {
      assertEquals("JS_CODE", builder.build().toJsString());
    }
  }
}
