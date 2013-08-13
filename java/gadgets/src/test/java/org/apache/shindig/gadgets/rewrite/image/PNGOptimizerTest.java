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
package org.apache.shindig.gadgets.rewrite.image;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Test;

import java.io.IOException;

/**
 * Test PNG handling
 */
public class PNGOptimizerTest extends BaseOptimizerTest {
  @Test
  public void testRewriteInefficientPNG() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/inefficient.png", "image/png");
    HttpResponse httpResponse = rewrite(resp);
    assertTrue(httpResponse.getContentLength() <= resp.getContentLength());
    assertEquals("image/png", httpResponse.getHeader("Content-Type"));
  }

  // Strip the alpha component from an image that was stored in RGBA form but
  // which is entirely opaque
  @Test
  public void testStripAlpha() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/rgbawithnoalpha.png", "image/png");
    HttpResponse httpResponse = rewrite(resp);
    assertTrue(httpResponse.getContentLength() <= resp.getContentLength());
    assertEquals("image/png", httpResponse.getHeader("Content-Type"));
  }

  @Test(expected=IOException.class)
  public void testEvil() throws Exception {
    // Metadata length is too long causes OutOfMemory
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/evil.png", "image/png");
    rewrite(resp);
    fail("Should have failed to read image");
  }

  HttpResponse rewrite(HttpResponse original)
      throws IOException, ImageReadException {
    HttpResponseBuilder builder = new HttpResponseBuilder(original);
    new PNGOptimizer(new OptimizerConfig(), builder).rewrite(
         Sanselan.getBufferedImage(original.getResponse()));
    return builder.create();
  }
}
