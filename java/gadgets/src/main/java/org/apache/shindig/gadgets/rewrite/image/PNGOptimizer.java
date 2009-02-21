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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

/**
 * Optimize a PNG image and possibly convert it to a JPEG.
 */
class PNGOptimizer extends BaseOptimizer {

  private boolean useJpeg;

  public PNGOptimizer(OptimizerConfig config, HttpResponse original)
      throws IOException {
    super(config, original);
  }

  protected void rewriteImpl(BufferedImage bufferedImage) throws IOException {
    BufferedImage palettized = ImageUtils.palettize(bufferedImage, config.getMaxPaletteSize());
    if (palettized != null) {
      write(palettized);
    }

    if (palettized == null) {
      // If we are efficiently palettized then only JPEG can really win
      if  (this.minBytes == null) {
        // nothing has been written yet, so just strip metadata
        write(bufferedImage);
      }

      // Depalettized images can win when a large palette has unused entries
      BufferedImage depalettized = ImageUtils.depalettize(bufferedImage,
          config.getMaxInMemoryBytes());
      if (depalettized != null) {
        write(depalettized);
      }
    }

    // Try JPEG for truly opaque images
    if (config.isJpegConversionAllowed()) {
      boolean isOpaque;
      if (palettized != null){
        bufferedImage = palettized;
        isOpaque = bufferedImage.getColorModel().getTransparency() == ColorModel.OPAQUE;
      } else {
        isOpaque = ImageUtils.isOpaque(bufferedImage);
      }

      if (isOpaque) {
        byte[] lastBytes = minBytes;
        int prevReductionPct = reductionPct;
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(config.getJpegCompression());
        outputter = new ImageIOOutputter(writer, param);
        write(bufferedImage);
        // Only use JPEG if it offers a significant reduction over other methods
        if (reductionPct > prevReductionPct + 20) {
          useJpeg = true;
        } else {
          minBytes = lastBytes;
        }
      }
    }
  }

  protected String getOutputContentType() {
    if (useJpeg) {
      return "image/jpeg";
    }
    return "image/png";
  }

  protected String getOriginalContentType() {
    return "image/png";
  }

  protected String getOriginalFormatName() {
    return "png";
  }
}
