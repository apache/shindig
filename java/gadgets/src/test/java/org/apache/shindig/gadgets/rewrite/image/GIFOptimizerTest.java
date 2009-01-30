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
package org.apache.shindig.gadgets.rewrite.image;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;

/**
 * Tests for GIFOptimizer
 */
public class GIFOptimizerTest extends BaseOptimizerTest {

  public void testEfficientGIF() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/unanimated.gif", "image/gif");
    HttpResponse httpResponse = rewrite(resp);
    assertSame(resp, httpResponse);
  }

  public void testRewriteLargeGIFToPNG() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/large.gif", "image/gif");
    HttpResponse httpResponse = rewrite(resp);
    assertTrue(httpResponse.getContentLength() <= resp.getContentLength());
    assertEquals(httpResponse.getHeader("Content-Type"), "image/png");
  }

  protected HttpResponse rewrite(HttpResponse original)
      throws IOException, ImageReadException {
    return new GIFOptimizer(new OptimizerConfig(), original).rewrite(
         Sanselan.getBufferedImage(original.getResponse()));
  }
}
