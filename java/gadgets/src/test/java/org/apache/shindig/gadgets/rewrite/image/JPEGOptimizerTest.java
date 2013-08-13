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
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Test;

import java.io.IOException;

/**
 * Test JPEG rewiting.
 */
public class JPEGOptimizerTest extends BaseOptimizerTest {
  @Test
  public void testSmallJPEGToPNG() throws Exception {
    // Should be significantly smaller
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/small.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp);
    assertEquals("image/png", rewritten.getHeader("Content-Type"));
    assertTrue(rewritten.getContentLength() * 100 / resp.getContentLength() < 70);
  }

  @Test
  public void testSmallJPEGIsNotConvertedToPNGWithJpegConversionDisabled() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/small.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp, getDefaultConfigWithJpegConversionDisabled());
    assertEquals("image/jpeg", rewritten.getHeader("Content-Type"));
    assertTrue(rewritten.getContentLength() <= resp.getContentLength());
  }

  @Test
  public void testRetainSubsmaplingDisabled() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/testImage444.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp, getConfigWithRetainSampling(false, 0.70f));
    JpegImageUtils.JpegImageParams params =
        JpegImageUtils.getJpegImageData(rewritten.getResponse(), "");
    assertTrue(rewritten.getContentLength() < resp.getContentLength());
    assertEquals(JpegImageUtils.SamplingModes.YUV420, params.getSamplingMode());
  }

  @Test
  public void testRetainSubsmaplingEnabled() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/testImage444.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp, getConfigWithRetainSampling(true, 0.70f));
    JpegImageUtils.JpegImageParams params =
        JpegImageUtils.getJpegImageData(rewritten.getResponse(), "");
    assertTrue(rewritten.getContentLength() < resp.getContentLength());
    assertEquals(JpegImageUtils.SamplingModes.YUV444, params.getSamplingMode());
  }

  @Test
  public void testLargeJPEG() throws Exception {
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/large.jpg", "image/jpeg");
    HttpResponse rewritten = rewrite(resp);
    assertEquals("image/jpeg", resp.getHeader("Content-Type"));
    assertTrue(rewritten.getContentLength() <= resp.getContentLength());
  }

  @Test
  public void testLargeJPEGWithEtagAndCacheHeaders() throws Exception {
    HttpResponseBuilder responseBuilder =
        createResponseBuilder("org/apache/shindig/gadgets/rewrite/image/large.jpg", "image/jpeg");
    responseBuilder.addHeader("ETag", "wereertret");
    responseBuilder.addHeader("Cache-Control", "public, max-age=86400");
    HttpResponse resp = responseBuilder.create();
    HttpResponse rewritten = rewrite(resp, getConfigWithRetainSampling(false, 0.70f));
    assertEquals("image/jpeg", resp.getHeader("Content-Type"));
    assertEquals("public, max-age=86400", resp.getHeader("Cache-Control"));
    assertNull(rewritten.getHeader("ETag"));
    assertTrue(rewritten.getContentLength() < resp.getContentLength());
  }

  @Test(expected=Throwable.class)
  public void testBadImage() throws Exception {
    // Not a JPEG
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/bad.jpg", "image/jpeg");
    rewrite(resp);
  }

  @Test(expected=Throwable.class)
  public void xtestBadICC1() throws Exception {
    // ICC section too long
    HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc.jpg", "image/jpeg");
    rewrite(resp);
  }

  @Test(expected=Throwable.class)
  public void testBadICC2() throws Exception {
    // ICC section too long
    HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc2.jpg", "image/jpeg");
    rewrite(resp);
  }

  @Test(expected=Throwable.class)
  public void testBadICC3() throws Exception {
    // ICC length lies
    HttpResponse resp = createResponse("org/apache/shindig/gadgets/rewrite/image/badicc3.jpg", "image/jpeg");
    rewrite(resp);
  }

  @Test
  public void testBadICC4() throws Exception {
    // ICC count too large
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/badicc4.jpg", "image/jpeg");
    try {
      rewrite(resp);
      fail("Should have failed with OutOfMemory exception");
    } catch (OutOfMemoryError oome) {
      // Currently we expect an OutOfMemory error. Working on this with Sanselan
    } catch (NullPointerException npe) {
      // For IBM JVM, NPE is thrown, bug: SANSELAN-23
    }
  }

  @Test
  public void testBadICC5() throws Exception {
    // ICC length too large. Should be readable by most VMs
    HttpResponse resp =
        createResponse("org/apache/shindig/gadgets/rewrite/image/2ndjpgbad.jpg", "image/jpeg");
    rewrite(resp);
  }

  HttpResponse rewrite(HttpResponse original) throws Exception {
    return rewrite(original, new OptimizerConfig());
  }

  HttpResponse rewrite(HttpResponse original, OptimizerConfig config)
      throws IOException, ImageReadException {
    HttpResponseBuilder builder = new HttpResponseBuilder(original);
    new JPEGOptimizer(config, builder,
                      JpegImageUtils.getJpegImageData(builder.getContentBytes(), ""))
        .rewrite(JPEGOptimizer.readJpeg(original.getResponse()));
    return builder.create();
  }

  OptimizerConfig getDefaultConfigWithJpegConversionDisabled() {
    OptimizerConfig defaultConfig = new OptimizerConfig();
    return new OptimizerConfig(defaultConfig.getMaxInMemoryBytes(),
    defaultConfig.getMaxPaletteSize(), false,
    defaultConfig.getJpegCompression(), defaultConfig.getMinThresholdBytes(),
    defaultConfig.getJpegHuffmanOptimization(), defaultConfig.getJpegRetainSubsampling());
  }

  OptimizerConfig getConfigWithRetainSampling(boolean enabled, float quality) {
    OptimizerConfig defaultConfig = new OptimizerConfig();
    return new OptimizerConfig(defaultConfig.getMaxInMemoryBytes(),
       defaultConfig.getMaxPaletteSize(), defaultConfig.isJpegConversionAllowed(),
       quality, defaultConfig.getMinThresholdBytes(), defaultConfig.getJpegHuffmanOptimization(),
       enabled);
  }
}
