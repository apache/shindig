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

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.rewrite.image.BaseOptimizer.ImageIOOutputter;
import org.apache.shindig.gadgets.rewrite.image.BaseOptimizer.ImageOutputter;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

/**
 * Rewrite images to more efficiently compress their content. Can output to a different format file
 * for better efficiency.
 *
 * <p>Security Note: Uses the Sanselan library to parse image content and metadata to avoid security
 * issues in the ImageIO library. Uses ImageIO for output.
 */
public class BasicImageRewriter implements ResponseRewriter {

  static final String
      CONTENT_TYPE_AND_EXTENSION_MISMATCH =
        "Content is not an image but file extension asserts it is";
  static final String
      CONTENT_TYPE_AND_MIME_MISMATCH =
          "Content is not an image but mime type asserts it is";

  private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
  /** Returned as the output message if a huge image is submitted to be scaled */
  private static final String RESIZE_IMAGE_TOO_LARGE = "The image is too large to resize";
  /** With resizing active, all images become PNGs */
  private static final String RESIZE_OUTPUT_FORMAT = "png";

  private static final String CONTENT_LENGTH = "Content-Length";

  /** Parameter used to request image rendering quality */
  private static final String PARAM_RESIZE_QUALITY = Param.RESIZE_QUALITY.getKey();
  /** Parameter used to request image width change */
  private static final String PARAM_RESIZE_WIDTH = Param.RESIZE_WIDTH.getKey();
  /** Parameter used to request image height change */
  private static final String PARAM_RESIZE_HEIGHT = Param.RESIZE_HEIGHT.getKey();
  /** Parameter used to request resizing will not expand image */
  private static final String PARAM_NO_EXPAND = Param.NO_EXPAND.getKey();

  private static final int BITS_PER_BYTE = 8;
  private static final Color COLOR_TRANSPARENT = new Color(255, 255, 255, 0);
  public static final String CONTENT_TYPE = "Content-Type";
  //class name for logging purpose
  private static final String classname = BasicImageRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private static final Set<String> SUPPORTED_MIME_TYPES = ImmutableSet.of(
      "image/gif", CONTENT_TYPE_IMAGE_PNG, "image/jpeg", "image/bmp");

  private static final Set<String> SUPPORTED_FILE_EXTENSIONS = ImmutableSet.of(
      ".gif", ".png", ".jpeg", ".jpg", ".bmp");

  private final OptimizerConfig config;

  private static class ImageResizeData {
    private Integer requestedWidth;
    private Integer requestedHeight;
    private Integer widthDelta;
    private Integer heightDelta;

    protected ImageResizeData(Integer requestedWidth, Integer requestedHeight, Integer widthDelta,
        Integer heightDelta) {
      this.requestedWidth = requestedWidth;
      this.requestedHeight = requestedHeight;
      this.widthDelta = widthDelta;
      this.heightDelta = heightDelta;
    }

    public Integer getWidth() {
      return requestedWidth;
    }

    public Integer getHeight() {
      return requestedHeight;
    }

    public Integer getWidthDelta() {
      return widthDelta;
    }

    public Integer getHeightDelta() {
      return heightDelta;
    }
  }

  @Inject
  public BasicImageRewriter(OptimizerConfig config) {
    this.config = config;
  }

  /**
   * Predicate check for validating the Image Rewrite step. Images that are either too huge or
   * invalid resize URL parameters are specified are not fit for rewrite.
   *
   * @param request the HTTP request.
   * @param response the HTTP response for the original image fetched.
   * @param imageInfo the image information extracted via Apache's Sanselan APIs.
   * @param isResizeRequested boolean flag to indicate whether Image resize is requested or not.
   * Huge images for which resize is requested are not fit for rewrite.
   * @return true if the specified image can be rewriten; else it's set to false.
   */
  private Boolean canRewrite(HttpRequest request, HttpResponseBuilder response,
      ImageInfo imageInfo, Boolean isResizeRequested) {
    Uri uri = request.getUri();
    if (null == uri) return false;

    // Don't handle very small images, but check after parsing format to
    // detect attacks.
    if (response.getContentLength() < config.getMinThresholdBytes()) {
      return false;
    }

    // TODO(anyone): The following check has been retained to maintain the functional equivalency
    // with the old code (and testcases). In case resize parameters are not specified, still one
    // can apply optimization to the response (call to 'applyOptimizer').
    Integer resizeQuality = request.getParamAsInteger(PARAM_RESIZE_QUALITY);
    Integer requestedWidth = request.getParamAsInteger(PARAM_RESIZE_WIDTH);
    Integer requestedHeight = request.getParamAsInteger(PARAM_RESIZE_HEIGHT);
    if (!isUsableParameter(requestedWidth) || !isUsableParameter(requestedHeight) ||
        !isUsableParameter(resizeQuality)) {
      return false;
    }

    if (isResizeRequested && isImageTooLarge(imageInfo)) {
      errorResponse(response, HttpResponse.SC_FORBIDDEN, RESIZE_IMAGE_TOO_LARGE);
      return false;
    }

    // Don't handle animations.
    // TODO: This doesn't work as current Sanselan doesn't return accurate image counts.
    // See animated GIF detection below.
    if (imageInfo.getNumberOfImages() > 1 || isImageTooLarge(imageInfo)) {
      return false;
    }

    return true;
  }

  /**
   * Predicate check for validating the Image Resizing step. Images with improper resize parameters
   * specified are not fit for resize.
   *
   * @param request the HTTP request.
   * @param response the HTTP response for the original image fetched.
   * @param imageInfo the image information extracted via Apache's Sanselan APIs.
   * @return true if the specified image can be rewriten; else it's set to false.
   */
  private Boolean isResizeRequested(HttpRequest request, HttpResponseBuilder response,
      ImageInfo imageInfo) {
    Integer requestedWidth = request.getParamAsInteger(PARAM_RESIZE_WIDTH);
    Integer requestedHeight = request.getParamAsInteger(PARAM_RESIZE_HEIGHT);

    boolean resizeRequested = ((requestedWidth != null) && isUsableParameter(requestedWidth) ||
                               (requestedHeight != null) && isUsableParameter(requestedHeight));
    boolean noExpand = "1".equals(request.getParam(PARAM_NO_EXPAND));
    if (noExpand &&
        (requestedHeight == null || imageInfo.getHeight() <= requestedHeight) &&
        (requestedWidth == null || imageInfo.getWidth() <= requestedWidth)) {
      // Don't do anything, since the current image fits within the bounding area.
      resizeRequested = false;
    }

    return resizeRequested;
  }

  /**
   * Get Image Resize data by honoring resizing parameters specified in the request.
   *
   * @param request the HTTP request.
   * @param response the HTTP response for the original image fetched.
   * @param image the image to resize and format conversion.
   * @param imageInfo the image information extracted via Apache's Sanselan APIs.
   * @return image resize data corresponding to the transformed width and height. The return value
   * is null for cases where image can't be resized.
   */
   private ImageResizeData getResizeData(HttpRequest request, HttpResponseBuilder response,
       BufferedImage image, ImageInfo imageInfo) throws IOException {
    int origWidth = imageInfo.getWidth();
    int origHeight = imageInfo.getHeight();
    int widthDelta = 0;
    int heightDelta = 0;
    Integer requestedWidth = request.getParamAsInteger(PARAM_RESIZE_WIDTH);
    Integer requestedHeight = request.getParamAsInteger(PARAM_RESIZE_HEIGHT);

    if (requestedWidth == null || requestedHeight == null) {
      // It is enough to cast only one int to double, Java will coerce all others to double
      // (JAVA spec, section 5.1.2).  In addition, interleave divisions and multiplications
      // to keep the end result at bay, and clip the requested dimensions from below to
      // compensate for small image dimensions.
      if (requestedWidth == null) {
        requestedWidth = max(1, (int) (origWidth / (double) origHeight * requestedHeight));
      }
      if (requestedHeight == null) {
        requestedHeight = max(1, (int) (origHeight / (double) origWidth * requestedWidth));
      }
    } else {
      // If both image dimensions are fixed, the two-step resizing process will need to know
      // how much it has to fix up the image.
      double ratio = getResizeRatio(requestedWidth, requestedHeight, origWidth, origHeight);
      int widthAfterStep1 = max(1, (int) Math.round(ratio * origWidth));
      widthDelta = requestedWidth - widthAfterStep1;

      int heightAfterStep1 = max(1, (int) Math.round(ratio * origHeight));
      heightDelta = requestedHeight - heightAfterStep1;

      boolean noExpand = "1".equals(request.getParam(PARAM_NO_EXPAND));
      if (noExpand) {
        // No expansion requested: make sure not to expand the resulting image on either axis,
        // even if both resize_[w,h] params are specified.
        if (widthDelta == 0) {
          requestedHeight = heightAfterStep1;
          heightDelta = 0;
        } else if (heightDelta == 0) {
          requestedWidth = widthAfterStep1;
          widthDelta = 0;
        }
      }
    }

    if (isResizeRequired(requestedWidth, requestedHeight, imageInfo)
        && !isTargetImageTooLarge(requestedWidth, requestedHeight, imageInfo)) {
      return new ImageResizeData(requestedWidth, requestedHeight, widthDelta, heightDelta);
    } else {
      return null;
    }
  }

   public void rewrite(HttpRequest request, HttpResponseBuilder response, Gadget gadget) {
     if (request == null || response == null) return;

     Uri uri = request.getUri();
     if (null == uri) return;

     try {
        // If the path or MIME type don't match, continue
       if (!isSupportedImageResult(response, uri)) {
         return;
       }

       // Content header checking is fast so this is fine to do for every response.
       ImageFormat imageFormat = Sanselan
           .guessFormat(new ByteSourceInputStream(response.getContentBytes(), uri.getPath()));

       if (imageFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
         enforceUnreadableImageRestrictions(uri, response);
         return;
       }

       ImageInfo imageInfo = Sanselan.getImageInfo(response.getContentBytes(), uri.getPath());

       Boolean resizeRequested = isResizeRequested(request, response, imageInfo);

       // Return in case image can't be rewriten.
       if (!canRewrite(request, response, imageInfo, resizeRequested)) {
         return;
       }

       JpegImageUtils.JpegImageParams jpegImageParams = null;
       if (imageFormat == ImageFormat.IMAGE_FORMAT_JPEG) {
         jpegImageParams = JpegImageUtils.getJpegImageData(response.getContentBytes(), uri.getPath());
       }

       // Step#1: Read the image using appropriate readers for the corresponding image format.
       BufferedImage image = readImage(imageFormat, response);

       // Proceed to Resize in case image can be resized.
       if (resizeRequested) {
         // Step#2: Get the Resize Data
         ImageResizeData resizeData = getResizeData(request, response, image, imageInfo);

         if (resizeData != null) {
           // Step#3: Resize (Scale+Stretch) Image using Java AWT Graphics2D package.
           image = resizeImage(image, resizeData.getWidth(), resizeData.getHeight(),
               resizeData.getWidthDelta(), resizeData.getHeightDelta());

           // Step#4: Convert the image format (MIME_TYPE) using javax.imageio package.
           updateResponse(response, image);
         }
       }

       // Step#5: Optimize the supported image formats viz PNG, GIF, JPG & BMP using 'BaseOptimizer'
       // and it's subclass implementations for the above four formats.
       applyOptimizer(response, imageFormat, jpegImageParams, image, config);
     } catch (IOException ioe) {
       if (LOG.isLoggable(Level.WARNING)) {
         LOG.logp(Level.WARNING, classname, "rewrite", MessageKeys.IO_ERROR_REWRITING_IMG, new Object[] {request.toString(),ioe.getMessage()});
       }
     } catch (RuntimeException re) {
       // This is safe to recover from and necessary because the ImageIO/Sanselan calls can
       // throw a very wide variety of exceptions
       if (LOG.isLoggable(Level.INFO)) {
         LOG.logp(Level.INFO, classname, "rewrite", MessageKeys.UNKNOWN_ERROR_REWRITING_IMG, new Object[] {request.toString(),re.getMessage()});
       }
     } catch (ImageReadException ire) {
       if (LOG.isLoggable(Level.INFO)) {
         LOG.logp(Level.INFO, classname, "rewrite", MessageKeys.FAILED_TO_READ_IMG, new Object[] {request.toString(),ire.getMessage()});
       }
     }
   }

  /**
   * If the image is resized, the request needs to change so that the optimizer can
   * make sensible image size-related decisions down the pipeline.  GIF images are rewritten
   * as PNGs though, so as not to include the dependency on the GIF decoder.
   *
   * @param response the base response that will be modified with the resized image
   * @param image the resized image that needs to be substituted for the original image from
   *        the response
   */
  public void updateResponse(HttpResponseBuilder response, BufferedImage image) throws IOException {
    ImageWriter imageWriter = ImageIO.getImageWritersByFormatName(RESIZE_OUTPUT_FORMAT).next();
    ImageOutputter outputter = new ImageIOOutputter(imageWriter, null);
    byte[] imageBytes = outputter.toBytes(image);
    response
        .setResponse(imageBytes)
        .setHeader(CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG)
        .setHeader(CONTENT_LENGTH, String.valueOf(imageBytes.length));
  }

  private boolean isUsableParameter(Integer parameterValue) {
    if (parameterValue == null) {
      return true;
    }
    return parameterValue.intValue() > 0;
  }

  /** Gets the feasible resize ratio. */
  private double getResizeRatio(int requestedWidth, int requestedHeight, int origWidth,
      int origHeight) {
    return min(requestedWidth / (double) origWidth,
              requestedHeight / (double) origHeight);
  }

  /**
   * Two-step image resize.
   *
   * <p>The first step scales the image so that the smaller of the vertical and horizontal
   * scaling ratios is satisfied.  For square images the two ratios are equal and we leave it
   * at that.  For rectangular images, this leaves a part of the target image rectangle that is
   * not covered, and we need to proceed to step 2.
   *
   * <p>The second step stretches the image along the dimension that came in short after the first
   * step to fully cover the target image rectangle.
   *
   * @param image the image to resize
   * @param requestedWidth the width in pixels of the requested resulting image
   * @param requestedHeight the height in pixels of the requested resulting image
   * @param extraWidth the width (in pixels) to add on top of the original image
   * @param extraHeight the height (in pixels) to add on top of the original image
   * @return the image obtained by stretching the original image so that its new dimensions
   *        are {@code requestedWidth} and {@code requestedHeight}
   */
  public BufferedImage resizeImage(BufferedImage image, Integer requestedWidth,
      Integer requestedHeight, int extraWidth, int extraHeight) {
    int widthStretch = requestedWidth - extraWidth;
    int heightStretch = requestedHeight - extraHeight;
    int imageType = ImageUtils.isOpaque(image)
        ? BufferedImage.TYPE_3BYTE_BGR
        : BufferedImage.TYPE_INT_ARGB;

    image = ImageUtils.getScaledInstance(image, widthStretch, heightStretch,
        VALUE_INTERPOLATION_BICUBIC, true /* higherQuality */, imageType);

    if (image.getWidth() != requestedWidth || image.getHeight() != requestedHeight) {
      image = stretchImage(image, requestedWidth, requestedHeight, imageType);
    }
    return image;
  }

  private BufferedImage stretchImage(BufferedImage image, Integer requestedWidth,
      Integer requestedHeight, int imageType) {
    BufferedImage scaledImage = new BufferedImage(requestedWidth, requestedHeight, imageType);

    Graphics2D g2d = scaledImage.createGraphics();
    g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
    fillWithTransparent(g2d, requestedWidth, requestedHeight);

    g2d.drawImage(image, 0, 0, requestedWidth, requestedHeight, null);
    image = scaledImage;
    return image;
  }

  private void fillWithTransparent(Graphics2D g2d, Integer requestedWidth,
      Integer requestedHeight) {
    g2d.setComposite(AlphaComposite.Clear);
    g2d.setColor(COLOR_TRANSPARENT);
    g2d.fillRect(0, 0, requestedWidth, requestedHeight);
    g2d.setComposite(AlphaComposite.SrcOver);
  }

  protected void applyOptimizer(HttpResponseBuilder response, ImageFormat imageFormat,
      JpegImageUtils.JpegImageParams jpegImageParams, BufferedImage image,
      OptimizerConfig config) throws IOException {
    if (imageFormat == ImageFormat.IMAGE_FORMAT_GIF) {
      // Detecting the existence of the NETSCAPE2.0 extension by string comparison
      // is not exactly clean but is good enough to determine if a GIF is animated
      // Remove once Sanselan returns image count
      if (!response.create().getResponseAsString().contains("NETSCAPE2.0")) {
        new GIFOptimizer(config, response).rewrite(image);
      }
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_PNG) {
      new PNGOptimizer(config, response).rewrite(image);
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_JPEG) {
      new JPEGOptimizer(config, response, jpegImageParams).rewrite(image);
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_BMP) {
      new BMPOptimizer(config, response).rewrite(image);
    }
  }

  private boolean isImageTooLarge(ImageInfo imageInfo) {
    return isTargetImageTooLarge(imageInfo.getWidth(), imageInfo.getHeight(), imageInfo);
  }

  /**
   * @param requestedHeight the requested image height, assumed always nonnegative
   * @param requestedWidth the requested image width, assumed always nonnegative
   * @param imageInfo the image information to analyze
   * @return {@code true} if the image size given by the parameters is too large to be acceptable
   *         for serving
   */
  private boolean isTargetImageTooLarge(int requestedHeight, int requestedWidth,
      ImageInfo imageInfo) {
    long imagePixels = abs(requestedHeight) * abs(requestedWidth);
    long imageSizeBits = imagePixels * imageInfo.getBitsPerPixel();
    return imageSizeBits > config.getMaxInMemoryBytes() * BITS_PER_BYTE;
  }

  protected boolean isSupportedImageResult(HttpResponseBuilder response, Uri uri) {
    return isSupportedContent(response) || isImageUri(uri);
  }

  protected boolean isSupportedContent(HttpResponseBuilder response) {
    return SUPPORTED_MIME_TYPES.contains(response.getHeader(CONTENT_TYPE));
  }

  /**
   * Ensures that the URI points to an image, before continuing.
   *
   *  @param uri the URI to check
   */
  protected boolean isImageUri(Uri uri) {
    boolean pathExtMatches = false;
    for (String ext: SUPPORTED_FILE_EXTENSIONS) {
      if (uri.getPath().endsWith(ext)) {
        pathExtMatches = true;
        break;
      }
    }
    return pathExtMatches;
  }

  private boolean isResizeRequired(int resize_w, int resize_h, ImageInfo imageInfo) {
    return resize_w != imageInfo.getWidth() || resize_h != imageInfo.getHeight();
  }

  /**
   * An image could not be read from the content. Normally this is fine unless the content-type
   * states that this is an image in which case it could be an attack. If either the filetype or the
   * MIME-type indicate that image content should be available but we failed to read it, then return
   * an error response.
   */
  protected void enforceUnreadableImageRestrictions(Uri uri, HttpResponseBuilder response) {
    String contentType = response.getHeader(CONTENT_TYPE);
    if (contentType != null) {
      contentType = contentType.toLowerCase();
      for (String expected : SUPPORTED_MIME_TYPES) {
        if (contentType.contains(expected)) {
          // MIME type says its a supported image but we can't read it. Reject.
          errorResponse(response, HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE,
              CONTENT_TYPE_AND_MIME_MISMATCH);
          return;
        }
      }
    }

    String path = uri.getPath().toLowerCase();
    for (String supportedExtension : SUPPORTED_FILE_EXTENSIONS) {
      if (path.endsWith(supportedExtension)) {
        // The file extension says its a supported image but we can't read it. Reject.
        errorResponse(response, HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE,
            CONTENT_TYPE_AND_EXTENSION_MISMATCH);
        return;
      }
    }
  }

  private void errorResponse(HttpResponseBuilder response, int status, String msg) {
    response.clearAllHeaders().setHttpStatusCode(status).setResponseString(msg);
  }

  protected BufferedImage readImage(ImageFormat imageFormat, HttpResponseBuilder response)
      throws ImageReadException, IOException{
    if (imageFormat == ImageFormat.IMAGE_FORMAT_GIF) {
      return readGif(response);
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_PNG) {
      return readPng(response);
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_JPEG) {
      return readJpeg(response);
    } else if (imageFormat == ImageFormat.IMAGE_FORMAT_BMP) {
      return readBmp(response);
    } else {
      throw new ImageReadException("Unsupported format " + imageFormat.name);
    }
  }

  // The following methods are intended to be overridden by implementors if they need to
  // implement additional security constraints or use their own more efficient
  // image reading mechanisms

  protected BufferedImage readBmp(HttpResponseBuilder response) throws ImageReadException, IOException {
    return BMPOptimizer.readBmp(response.getContentBytes());
  }

  protected BufferedImage readPng(HttpResponseBuilder response) throws ImageReadException, IOException {
    return PNGOptimizer.readPng(response.getContentBytes());
  }

  protected BufferedImage readGif(HttpResponseBuilder response) throws ImageReadException, IOException {
    return GIFOptimizer.readGif(response.getContentBytes());
  }

  protected BufferedImage readJpeg(HttpResponseBuilder response) throws ImageReadException, IOException {
    return JPEGOptimizer.readJpeg(response.getContentBytes());
  }
}
