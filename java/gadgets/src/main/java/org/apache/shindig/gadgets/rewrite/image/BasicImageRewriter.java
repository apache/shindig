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

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Rewrite images to more efficiently compress their content. Can rewrite images
 * from one format to another for better efficiency.
 *
 * Security Note:
 * Uses the Sanselan library to parse image content and metadata to avoid security
 * issues in the ImageIO library. Uses ImageIO for output
 */
public class BasicImageRewriter implements ImageRewriter {

  private static final Logger log = Logger.getLogger(BasicImageRewriter.class.getName());

  private static final Set<String> SUPPORTED_MIME_TYPES = ImmutableSet.of(
      "image/gif", "image/png", "image/jpeg", "image/bmp");

  private static final Set<String> SUPPORTED_FILE_EXTNS = ImmutableSet.of(
      ".gif", ".png", ".jpeg", ".jpg", ".bmp");

  private final OptimizerConfig config;

  private final AtomicLong originalImageBytes = new AtomicLong();

  private final AtomicLong rewrittenImageBytes = new AtomicLong();

  @Inject
  public BasicImageRewriter(OptimizerConfig config) {
    this.config = config;
  }

  /**
   * Rewrite an HTTP response that contains image data to a more efficient representation
   *
   * @param uri of original content
   * @param response to rewrite
   * @return Rewritten response or original content
   */
  public HttpResponse rewrite(Uri uri, HttpResponse response) {
    try {
      // Content header checking is fast so this is fine to do for every response.
      ImageFormat imageFormat = Sanselan
          .guessFormat(new ByteSourceInputStream(response.getResponse(), uri.getPath()));

      if (imageFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
        return enforceUnreadableImageRestrictions(uri, response);
      }

      // Dont handle very small images but check after parsing format to
      // detect attacks
      if (response.getContentLength() < config.getMinThresholdBytes()) {
        return response;
      }

      ImageInfo imageInfo = Sanselan.getImageInfo(response.getResponse(), uri.getPath());

      // Dont handle animations
      // TODO: This doesnt work as current Sanselan doesnt accurately return image counts
      // See animated gif detection below
      if (imageInfo.getNumberOfImages() > 1) {
        return response;
      }

      // Check if reading the image into memory would exceed max in-mem threshold
      // Note that pixel size is in bits. Imge dimensions can be unsigned 32-bit
      // which can cause problems reading them in Java. Use absolute value to protect
      // against this.
      if (Math.abs((long)imageInfo.getWidth()) * Math.abs((long)imageInfo.getHeight()) *
          imageInfo.getBitsPerPixel() >
          config.getMaxInMemoryBytes() * 8) {
        return response;
      }

      int originalBytes = response.getContentLength();
      originalImageBytes.addAndGet(originalBytes);
      if (imageFormat == ImageFormat.IMAGE_FORMAT_GIF) {
        // Detecting the existence of the NETSCAPE2.0 extension by string comparison
        // is not exactly clean but is good enough to determine if a GIF is animated
        // Remove once Sanselan returns image count
        if (!response.getResponseAsString().contains("NETSCAPE2.0")) {
          response = new GIFOptimizer(config, response).rewrite(
              Sanselan.getBufferedImage(response.getResponse()));
        }
      } else if (imageFormat == ImageFormat.IMAGE_FORMAT_PNG) {
        response = new PNGOptimizer(config, response)
            .rewrite(Sanselan.getBufferedImage(response.getResponse()));
      } else if (imageFormat == ImageFormat.IMAGE_FORMAT_JPEG) {
        // We cant use Sanselan to read JPEG but we can use it to read all the metadata which is
        // where we have issues anyway
        Sanselan.getMetadata(response.getResponse(), null);
        Sanselan.getICCProfile(response.getResponse(), null);
        response = new JPEGOptimizer(config, response)
            .rewrite(ImageIO.read(response.getResponse()));
      } else if (imageFormat == ImageFormat.IMAGE_FORMAT_BMP) {
        response = new BMPOptimizer(config, response)
            .rewrite(Sanselan.getBufferedImage(response.getResponse()));
      }
      rewrittenImageBytes.addAndGet(response.getContentLength());
    } catch (IOException ioe) {
      log.log(Level.WARNING, "IO Error rewriting image " + uri.toString(), ioe);
    } catch (RuntimeException re) {
      // This is safe to recover from and necessary because the ImageIO/Sanselan calls can
      // throw a very wide variety of exceptions
      log.log(Level.INFO, "Unknown error rewriting image " + uri.toString(), re);
    } catch (ImageReadException ire) {
      log.log(Level.INFO, "Failed to read image. Skipping " + uri.toString(), ire);
    }
    return response;
  }

  /**
   * An image could not be read from the content. Normally this is fine unless the content-type
   * states that this is an image in which case it could be an attack. If either the filetype or the
   * mime-type indicate that image content should be available but we failed to read it then return
   * an error response.
   */
  HttpResponse enforceUnreadableImageRestrictions(Uri uri, HttpResponse original) {
    String contentType = original.getHeader("Content-Type");
    if (contentType != null) {
      contentType = contentType.toLowerCase();
      for (String expected : SUPPORTED_MIME_TYPES) {
        if (contentType.contains(expected)) {
          // Mime type says its a supported image but we cant read it. Reject
          return new HttpResponseBuilder(original)
              .setHttpStatusCode(HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE)
              .setResponseString("Content is not an image but mime type asserts it is")
              .create();
        }
      }
    }

    String path = uri.getPath().toLowerCase();
    for (String expected : SUPPORTED_FILE_EXTNS) {
      if (path.endsWith(expected)) {
        // Mime type says its a supported image but we cant read it. Reject
        return new HttpResponseBuilder(original)
            .setHttpStatusCode(HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE)
            .setResponseString("Content is not an image but file extension asserts it is")
            .create();
      }
    }
    return original;
  }

  /** The total number of image bytes prior to rewrite */
  public long getOriginalImageBytes() {
    return originalImageBytes.get();
  }

  /** The total number of images bytes after rewriting */
  public long getRewrittenImageBytes() {
    return rewrittenImageBytes.get();
  }
}
