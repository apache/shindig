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

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

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
  final HttpResponse originalResponse;
  final OptimizerConfig config;

  ImageWriter writer;

  private ByteArrayOutputStream baos;
  protected byte[] minBytes;
  protected int minLength;
  int reductionPct;

  public BaseOptimizer(OptimizerConfig config, HttpResponse original)
      throws IOException {
    this.config = config;
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(getOriginalFormatName());
    if (writers.hasNext()) {
      this.writer = writers.next();
    }
    this.originalResponse = original;
    this.minLength = original.getContentLength();
  }

  /**
   * Write the image using its default write parameters
   */
  protected void write(BufferedImage image) throws IOException {
    write(image, null);
  }

  /**
   * Write the image using a specified write param
   */
  protected void write(BufferedImage image, ImageWriteParam writeParam) throws IOException {
    if (image == null) {
      return;
    }
    ByteArrayOutputStream stream = getOutputStream();
    writer.setOutput(ImageIO.createImageOutputStream(stream));
    if (writeParam == null) {
      writeParam = writer.getDefaultWriteParam();
    }
    // Create a new empty metadata set
    IIOMetadata metadata = writer.getDefaultImageMetadata(
        new ImageTypeSpecifier(image.getColorModel(), image.getSampleModel()),
        writeParam);
    writer.write(new IIOImage(image, Collections.<BufferedImage>emptyList(), metadata));
    if (minLength > stream.size()) {
      minBytes = stream.toByteArray();
      minLength = minBytes.length;
      reductionPct = ((originalResponse.getContentLength() - minLength) * 100) /
          originalResponse.getContentLength();
    }
  }

  public HttpResponse rewrite(BufferedImage image) throws IOException {
    if (writer == null) {
      return originalResponse;
    }

    long time = System.currentTimeMillis();
    rewriteImpl(image);
    time = System.currentTimeMillis() - time;
    if (minBytes != null) {
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

  // Lazily initialize stream
  private ByteArrayOutputStream getOutputStream() {
    if (baos == null) {
      baos = new ByteArrayOutputStream(originalResponse.getContentLength());
    }
    baos.reset();
    return baos;
  }

  /**
   * Get the rewritten image if available
   * @return
   */
  protected final byte[] getRewrittenImage() {
    return minBytes;
  }

  protected abstract void rewriteImpl(BufferedImage image) throws IOException;

  protected abstract String getOutputContentType();

  protected abstract String getOriginalContentType();

  protected abstract String getOriginalFormatName();
}
