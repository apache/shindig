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

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageWriteParam;

/**
 * Optimize JPEG images by either converting them to PNGs or re-encoding them with a more
 * appropriate compression level.
 */
public class JPEGOptimizer extends BaseOptimizer {

  private boolean usePng;

  public JPEGOptimizer(OptimizerConfig config, HttpResponse original)
      throws IOException, ImageReadException {
    super(config, original);
  }

  protected void rewriteImpl(BufferedImage image) throws IOException {
    // Create a new optimizer config and disable JPEG conversion
    OptimizerConfig pngConfig = new OptimizerConfig(config.getMaxInMemoryBytes(),
        config.getMaxPaletteSize(), false, config.getJpegCompression(),
        config.getMinThresholdBytes());

    // Output the image as PNG
    PNGOptimizer pngOptimizer = new PNGOptimizer(pngConfig, originalResponse);
    pngOptimizer.rewriteImpl(image);

    int pngLength = Integer.MAX_VALUE;
    if (pngOptimizer.getRewrittenImage()  != null) {
      // PNG was better than original so use it
      minBytes = pngOptimizer.getRewrittenImage();
      minLength = minBytes.length;
      pngLength = minLength;
    }

    // Write as standard JPEG using the configured default compression level
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(config.getJpegCompression());
    write(image, param);

    // JPEG did not beat PNG
    if (pngLength == minLength) {
      usePng = true;
    }
  }

  protected String getOutputContentType() {
    if (usePng) {
      return "image/png";
    }
    return "image/jpeg";
  }

  protected String getOriginalContentType() {
    return "image/jpeg";
  }

  protected String getOriginalFormatName() {
    return "jpeg";
  }
}
