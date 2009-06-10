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
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.base.Preconditions;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

/**
 * Base class for image optimizers
 */
abstract class BaseOptimizer {

  static final Map<String, ImageFormat> formatNameToImageFormat = ImmutableMap.of(
      "png", ImageFormat.IMAGE_FORMAT_PNG,
      "gif", ImageFormat.IMAGE_FORMAT_GIF,
      "jpeg", ImageFormat.IMAGE_FORMAT_JPEG);

  final HttpResponse originalResponse;
  final OptimizerConfig config;

  protected ImageOutputter outputter;
  protected byte[] minBytes;
  protected int minLength;
  int reductionPct;


  public BaseOptimizer(OptimizerConfig config, HttpResponse original) {
    this.config = config;
    this.originalResponse = original;
    this.minLength = original.getContentLength();
    this.outputter = getOutputter();
  }

  protected ImageOutputter getOutputter() {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(getOriginalFormatName());
    if (writers.hasNext()) {
      ImageWriter writer = writers.next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      if (getOriginalFormatName().equals("jpeg")) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(config.getJpegCompression());
      }
      return new ImageIOOutputter(writer, param);
    }
    return new SanselanOutputter(formatNameToImageFormat.get(getOriginalFormatName()));
  }

  /**
   * Write the image using a specified write param
   */
  protected void write(BufferedImage image) throws IOException {
    if (image == null) {
      return;
    }
    byte[] bytes = outputter.toBytes(image);
    if (minLength > bytes.length) {
      minBytes = bytes;
      minLength = minBytes.length;
      reductionPct = ((originalResponse.getContentLength() - minLength) * 100) /
          originalResponse.getContentLength();
    }
  }

  public HttpResponse rewrite(BufferedImage image) throws IOException {
    if (outputter == null) {
      return originalResponse;
    }

    long time = System.currentTimeMillis();
    rewriteImpl(image);
    time = System.currentTimeMillis() - time;
    if (minBytes != null && minBytes.length != 0) {
      StringBuilder rewriteMsg = new StringBuilder();
      rewriteMsg.append("c=").append(
          ((minBytes.length * 100) / originalResponse.getContentLength()));
      if (!getOutputContentType().equals(getOriginalContentType())) {
        rewriteMsg.append(";o=").append(getOriginalContentType());
      }
      rewriteMsg.append(";t=").append(time);
      return new HttpResponseBuilder(originalResponse)
          .setHeader("Content-Type", getOutputContentType())
          .setHeader("X-Shindig-Rewrite", rewriteMsg.toString())
          .setResponse(minBytes).create();
    }
    return originalResponse;
  }

  /**
   * Get the rewritten image if available
   */
  protected final byte[] getRewrittenImage() {
    return minBytes;
  }

  protected abstract void rewriteImpl(BufferedImage image) throws IOException;

  protected abstract String getOutputContentType();

  protected abstract String getOriginalContentType();

  protected abstract String getOriginalFormatName();

  /**
   * Interface to allow for different serialization libraries to be used
   */
  public static interface ImageOutputter {
    byte[] toBytes(BufferedImage image) throws IOException;
  }

  /**
   * Standard ImageIO based image outputter
   */
  public static class ImageIOOutputter implements ImageOutputter {

    ImageWriter writer;
    ByteArrayOutputStream baos;
    ImageWriteParam writeParam;
    public ImageIOOutputter(ImageWriter writer, ImageWriteParam writeParam) {
      this.writer = writer;
      this.writeParam = firstNonNull(writeParam, writer.getDefaultWriteParam());
    }

    public byte[] toBytes(BufferedImage image) throws IOException {
      if (baos == null) {
        baos = new ByteArrayOutputStream();
      } else {
        baos.reset();
      }
      writer.setOutput(ImageIO.createImageOutputStream(baos));
      // Create a new empty metadata set
      IIOMetadata metadata = writer.getDefaultImageMetadata(
          new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel()),
          writeParam);
      writer.write(new IIOImage(image, Collections.<BufferedImage>emptyList(), metadata));
      return baos.toByteArray();
    }
  }

  /**
   * Sanselan based image outputter
   */
  public static class SanselanOutputter implements ImageOutputter {

    ImageFormat format;
    ByteArrayOutputStream baos;
    public SanselanOutputter(ImageFormat format) {
      this.format = format;
    }

    public byte[] toBytes(BufferedImage image) throws IOException {
      if (baos == null) {
        baos = new ByteArrayOutputStream();
      } else {
        baos.reset();
      }
      try {
        Sanselan.writeImage(image, baos, format, Maps.newHashMap());
        return baos.toByteArray();
      } catch (ImageWriteException iwe) {
        throw new IOException(iwe.getMessage());
      }
    }
  }
  private static <T> T firstNonNull(T first, T second) {
    return first != null ? first : Preconditions.checkNotNull(second);
  }
}
