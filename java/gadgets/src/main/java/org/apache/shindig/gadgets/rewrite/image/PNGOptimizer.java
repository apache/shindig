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
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;

/**
 * Optimize a PNG image and possibly convert it to a JPEG.
 */
class PNGOptimizer extends BaseOptimizer {

  public static BufferedImage readPng(InputStream is)
      throws ImageReadException, IOException {
    return Sanselan.getBufferedImage(is);
  }

  private boolean useJpeg;

  public PNGOptimizer(OptimizerConfig config, HttpResponseBuilder response) {
    super(config, response);
  }

  @Override
  protected void rewriteImpl(BufferedImage bufferedImage) throws IOException {
    BufferedImage palettized = ImageUtils.palettize(bufferedImage, config.getMaxPaletteSize());
    if (palettized != null) {
      write(palettized);
    }

    if (palettized == null) {
      // If we are efficiently palletized then only JPEG can really win
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

        // Workaround for bug in JPEG image writer
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6444933
        // Writer seems to think color space is CMYK and not RGBA. In this
        // case the image is fully opaque so we can just down-convert to just RGB.
        BufferedImage rgbOnlyImage = new BufferedImage(bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        rgbOnlyImage.getGraphics().drawImage(bufferedImage, 0, 0, null);

        JPEGOptimizer jpegOptimizer = new JPEGOptimizer(config, response);
        outputter = jpegOptimizer.getOutputter();
        write(rgbOnlyImage);
        // Only use JPEG if it offers a significant reduction over other methods
        if (reductionPct > prevReductionPct + 20) {
          useJpeg = true;
        } else {
          minBytes = lastBytes;
        }
      }
    }
  }

  @Override
  protected String getOutputContentType() {
    if (useJpeg) {
      return "image/jpeg";
    }
    return "image/png";
  }

  @Override
  protected String getOriginalContentType() {
    return "image/png";
  }

  @Override
  protected String getOriginalFormatName() {
    return "png";
  }
}
