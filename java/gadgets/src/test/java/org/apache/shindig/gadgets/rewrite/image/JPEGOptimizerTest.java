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
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Test JPEG rewiting.
 */
public class JPEGOptimizerTest extends BaseOptimizerTest {

  public void testSmallJPEGToPNG() throws Exception {
    // Should be significantly smaller
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/small.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp);
    assertEquals(rewritten.getHeader("Content-Type"), "image/png");
    assertTrue(rewritten.getContentLength() * 100 / resp.getContentLength() < 70);
  }

  public void testLargeJPEG() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/large.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp);
    assertEquals(resp.getHeader("Content-Type"), "image/jpeg");
    assertTrue(rewritten.getContentLength() <= resp.getContentLength());
  }

  public void testBadImage() throws Exception {
    // Not a JPEG
    try {
      HttpResponse resp =
          createResponse("org/apache/shindig/gadgets/rewrite/image/bad.jpg", "image/jpeg");
      rewrite(resp);
      fail("Should fail to read an invalid JPEG");
    } catch (Throwable t) {
      // Exception type can vary between implementations
    }

  }

  public void xtestBadICC1() throws Exception {
    // ICC section too long 
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/badicc.jpg", "image/jpeg");
    rewrite(resp);
  }

  public void testBadICC2() throws Exception {
    // ICC section too long
    try {
      HttpResponse resp =
          createResponse("org/apache/shindig/gadgets/rewrite/image/badicc2.jpg", "image/jpeg");
      rewrite(resp);
      fail("Should error with invalid ICC data");
    } catch (Throwable t) {
      //assertTrue(t instanceof ImageReadException);
    }
  }

  public void testBadICC3() throws Exception {
    // ICC length lies
    try {
      HttpResponse resp =
          createResponse("org/apache/shindig/gadgets/rewrite/image/badicc3.jpg", "image/jpeg");
      rewrite(resp);
      fail("Should error with invalid ICC data");
    } catch (Throwable t) {
      //assertTrue(t instanceof ImageReadException);
    }
  }

  public void testBadICC4() throws Exception {
    // ICC count too large
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/badicc4.jpg", "image/jpeg");
    try {
      rewrite(resp);
      fail("Should have failed with OutOfMemory exception");
    } catch (OutOfMemoryError oome) {
      // Currently we expect an OutOfMemory error. Working on this with Sanselan
    }
  }

  public void testBadICC5() throws Exception {
    // ICC length too large. Should be readable by most VMs
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/2ndjpgbad.jpg", "image/jpeg");
    rewrite(resp);
  }

  HttpResponse rewrite(HttpResponse original)
      throws IOException, ImageReadException {
    return new JPEGOptimizer(new OptimizerConfig(), original).rewrite(
        JPEGOptimizer.readJpeg(original.getResponse()));
  }
}
