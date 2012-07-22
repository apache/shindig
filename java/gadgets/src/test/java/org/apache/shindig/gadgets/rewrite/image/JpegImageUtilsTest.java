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

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.io.*;

/**
 * Tests for {@code JpegImageUtils}
 */
public class JpegImageUtilsTest extends Assert {
  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testGetJpegImageData_huffmanOptimized() throws Exception {
    String resource = "org/apache/shindig/gadgets/rewrite/image/testImage420.jpg";
    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
    JpegImageUtils.JpegImageParams imageParams = JpegImageUtils.getJpegImageData(is, resource);
    assertEquals(true, imageParams.isHuffmanOptimized());
  }

  @Test
  public void testGetJpegImageData_420Sampling() throws Exception {
    String resource = "org/apache/shindig/gadgets/rewrite/image/testImage420.jpg";
    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
    JpegImageUtils.JpegImageParams imageParams = JpegImageUtils.getJpegImageData(is, resource);
    assertEquals(JpegImageUtils.SamplingModes.YUV420, imageParams.getSamplingMode());
  }

  @Test
  public void testGetJpegImageData_444Sampling() throws Exception {
    String resource = "org/apache/shindig/gadgets/rewrite/image/testImage444.jpg";
    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
    JpegImageUtils.JpegImageParams imageParams = JpegImageUtils.getJpegImageData(is, resource);
    assertEquals(JpegImageUtils.SamplingModes.YUV444, imageParams.getSamplingMode());
    assertEquals(0.90F, imageParams.getChromaQualityFactor(), 0.01F);
    assertEquals(0.90F, imageParams.getLumaQualityFactor(), 0.01F);
    assertEquals(0.90F, imageParams.getApproxQualityFactor(), 0.01F);
  }

  @Test
  public void testGetJpegImageData_notHuffmanOptimized() throws Exception {
    String resource = "org/apache/shindig/gadgets/rewrite/image/testImageNotHuffmanOptimized.jpg";
    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
    JpegImageUtils.JpegImageParams imageParams = JpegImageUtils.getJpegImageData(is, resource);
    assertEquals(false, imageParams.isHuffmanOptimized());
  }
}
