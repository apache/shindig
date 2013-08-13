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

import static
    org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.CONTENT_TYPE_AND_EXTENSION_MISMATCH;
import static
    org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.CONTENT_TYPE_AND_MIME_MISMATCH;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createControl;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * Tests for the {@link ImageRewriter} class.
 */
public class ImageRewriterTest extends Assert {

  /** The image used for the scaling tests, a head-shot of a white dog, 500pix x 500pix */
  private static final String SCALE_IMAGE = "org/apache/shindig/gadgets/rewrite/image/dog.gif";

  /** A 60 x 30 image whose size in bytes expands when resized to 120 x 60 */
  private static final String EXPAND_IMAGE = "org/apache/shindig/gadgets/rewrite/image/expand.gif";

  /** A 600 x 400 image whose size used to cause trouble with rounding when resizing to 171 x 171 */
  private static final String RATIO_IMAGE = "org/apache/shindig/gadgets/rewrite/image/ratio.gif";

  /**
   * This image has a huge memory footprint that the rewriter should refuse to resize, but not
   * refuse to render.  The response containing this image should not be rewritten.
   */
  private static final String HUGE_IMAGE = "org/apache/shindig/gadgets/rewrite/image/huge.gif";

  private static final String CONTENT_TYPE_BOGUS = "notimage/anything";
  private static final String CONTENT_TYPE_JPG = "image/jpeg";
  private static final String CONTENT_TYPE_GIF = "image/gif";
  private static final String CONTENT_TYPE_PNG = "image/png";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private static final Uri IMAGE_URL = Uri.parse("http://www.example.com/image.gif");

  private ResponseRewriter rewriter;
  private IMocksControl mockControl;

  @Before
  public void setUp() throws Exception {
    rewriter = new BasicImageRewriter(new OptimizerConfig());
    mockControl = createControl();
  }

  /** Makes a new {@link HttpResponse} with an image content. */
  private HttpResponseBuilder getImageResponse(String contentType, byte[] imageBytes) {
    return new HttpResponseBuilder()
        .setHeader(CONTENT_TYPE_HEADER, contentType)
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponse(imageBytes);
  }

  /** Extracts an image by its resource name and converts it into a byte array. */
  private byte[] getImageBytes(String imageResourceName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    byte[] imageBytes = IOUtils.toByteArray(classLoader.getResourceAsStream(imageResourceName));
    assertNotNull(imageBytes);
    return imageBytes;
  }

  private BufferedImage getResizedHttpResponseContent(String sourceContentType,
      String targetContentType, String imageName, Integer width, Integer height, Integer quality)
      throws Exception {
    return getResizedHttpResponseContent(
        sourceContentType, targetContentType, imageName, width, height, quality, false);
  }

  private BufferedImage getResizedHttpResponseContent(String sourceContentType,
      String targetContentType, String imageName, Integer width, Integer height, Integer quality,
      boolean noExpand)
      throws Exception {
    HttpResponseBuilder response = getImageResponse(sourceContentType, getImageBytes(imageName));
    HttpRequest request = getMockRequest(width, height, quality, noExpand);

    mockControl.replay();
    rewriter.rewrite(request, response, null);
    mockControl.verify();

    assertEquals(targetContentType, response.getHeader(CONTENT_TYPE_HEADER));
    return ImageIO.read(response.getContentBytes());
  }

  private HttpRequest getMockRequest(Integer width, Integer height, Integer quality, boolean noExpand) {
    HttpRequest request = mockControl.createMock(HttpRequest.class);
    expect(request.getUri()).andReturn(IMAGE_URL).anyTimes();
    expect(request.getParamAsInteger(Param.RESIZE_QUALITY.getKey())).andReturn(quality).anyTimes();
    expect(request.getParamAsInteger(Param.RESIZE_WIDTH.getKey())).andReturn(width).anyTimes();
    expect(request.getParamAsInteger(Param.RESIZE_HEIGHT.getKey())).andReturn(height).anyTimes();
    expect(request.getParam(Param.NO_EXPAND.getKey())).andReturn(noExpand ? "1" : null).anyTimes();
    return request;
  }

  @Test
  public void testRewriteValidImageWithValidMimeAndExtn() throws Exception {
    byte[] bytes = getImageBytes("org/apache/shindig/gadgets/rewrite/image/inefficient.png");
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_PNG, bytes);
    int originalContentLength = response.getContentLength();

    rewriter.rewrite(new HttpRequest(Uri.parse("some.png")), response, null);
    assertEquals(HttpResponse.SC_OK, response.getHttpStatusCode());
    assertTrue(response.getContentLength() < originalContentLength);
  }

  @Test
  public void testRewriteValidImageWithInvalidMimeAndFileExtn() throws Exception {
    byte[] bytes = getImageBytes("org/apache/shindig/gadgets/rewrite/image/inefficient.png");
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_BOGUS, bytes);
    int originalContentLength = response.getContentLength();

    rewriter.rewrite(new HttpRequest(Uri.parse("some.junk")), response, null);
    assertEquals(HttpResponse.SC_OK, response.getHttpStatusCode());
    assertEquals(response.getContentLength(), originalContentLength);
  }

  @Test
  public void testRewriteInvalidImageContentWithValidMime() throws Exception {
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_PNG, "This is not a PNG".getBytes());
    rewriter.rewrite(new HttpRequest(Uri.parse("some.junk")), response, null);

    assertEquals(HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getHttpStatusCode());
    assertEquals(CONTENT_TYPE_AND_MIME_MISMATCH, response.create().getResponseAsString());
  }

  @Test
  public void testRewriteInvalidImageContentWithValidFileExtn() throws Exception {
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_BOGUS, "This is not an image".getBytes());
    rewriter.rewrite(new HttpRequest(Uri.parse("some.png")), response, null);

    assertEquals(HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE, response.getHttpStatusCode());
    assertEquals(CONTENT_TYPE_AND_EXTENSION_MISMATCH,
        response.create().getResponseAsString());
  }

  @Test
  public void testNoRewriteAnimatedGIF() throws Exception {
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_GIF,
        getImageBytes("org/apache/shindig/gadgets/rewrite/image/animated.gif"));
    int changesBefore = response.getNumChanges();
    rewriter.rewrite(new HttpRequest(Uri.parse("animated.gif")), response, null);
    assertEquals(changesBefore, response.getNumChanges());
  }

  @Test
  public void testRewriteUnAnimatedGIF() throws Exception {
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_GIF,
        getImageBytes("org/apache/shindig/gadgets/rewrite/image/large.gif"));
    rewriter.rewrite(new HttpRequest(Uri.parse("large.gif")), response, null);
    assertEquals(CONTENT_TYPE_PNG, response.getHeader(CONTENT_TYPE_HEADER));
  }

  // Resizing image tests
  //
  // Checks at least the basic image parameters.  It is rather nontrivial to check for the actual
  // image content, as the ImageIO implementations vary across JVMs, so we have to skip it.

  @Test
  public void testResize_width() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(CONTENT_TYPE_GIF, CONTENT_TYPE_JPG,
        SCALE_IMAGE, 100 /* width */, null /* height */, null /* quality */);
    assertEquals(100, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  public void testResize_height() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE,  null, 100, null);
    assertEquals(100, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  public void testResize_both() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE, 100, 100, null);
    assertEquals(100, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  public void testResize_all() throws Exception {
    // The quality hint apparently has no effect on the result here
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE, 100, 100, 10);
    assertEquals(100, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  public void testResize_wideImage() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE, 100, 50, null);
    assertEquals(100, image.getWidth());
    assertEquals(50, image.getHeight());
  }

  @Test
  public void testResize_tallImage() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE,  50, 100, null);
    assertEquals(50, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  public void testResize_skipResizeHugeOutputImage() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, SCALE_IMAGE, 10000, 10000, null);
    assertEquals(500, image.getWidth());
    assertEquals(500, image.getHeight());
  }

  @Test
  public void testResize_brokenParameter() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_GIF, SCALE_IMAGE, -1, null, null);
    assertEquals(500, image.getWidth());
    assertEquals(500, image.getHeight());
  }

  @Test
  public void testResize_expandImage() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_JPG, EXPAND_IMAGE, 120, 60, null);
    assertEquals(120, image.getWidth());
    assertEquals(60, image.getHeight());
  }

  @Test
  public void testResize_noExpandImage() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_PNG /* still optimized */,
        EXPAND_IMAGE, 120, 60, null, true /* no expand */);
    assertEquals(60, image.getWidth());
    assertEquals(30, image.getHeight());
  }

  @Test
  public void testResize_refuseHugeInputImages() throws Exception {
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_GIF, getImageBytes(HUGE_IMAGE));
    HttpRequest request = getMockRequest(120, 60, null, false);
    mockControl.replay();
    rewriter.rewrite(request, response, null);
    mockControl.verify();
    assertEquals(HttpResponse.SC_FORBIDDEN, response.getHttpStatusCode());
  }

  @Test
  public void testResize_acceptServeHugeImages() throws Exception {
    byte[] imageBytes = getImageBytes(HUGE_IMAGE);
    HttpResponseBuilder response = getImageResponse(CONTENT_TYPE_GIF, imageBytes);
    HttpRequest request = getMockRequest(null, null, null, false);
    mockControl.replay();
    rewriter.rewrite(request, response, null);
    mockControl.verify();
    assertEquals(HttpResponse.SC_OK, response.getHttpStatusCode());
    assertTrue(Arrays.equals(imageBytes, IOUtils.toByteArray(response.getContentBytes())));
  }

  @Test
  public void testResize_avoidFloatingPointRoundingProblems() throws Exception {
    BufferedImage image = getResizedHttpResponseContent(
        CONTENT_TYPE_GIF, CONTENT_TYPE_PNG, RATIO_IMAGE, 171, 171, null, true);
    assertEquals(171, image.getWidth());
    assertEquals(114, image.getHeight());
  }
}
