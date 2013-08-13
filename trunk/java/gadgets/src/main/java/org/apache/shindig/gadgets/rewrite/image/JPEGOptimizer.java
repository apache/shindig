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

import org.apache.commons.io.IOUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import javax.imageio.ImageIO;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Optimize JPEG images by either converting them to PNGs or re-encoding them with a more
 * appropriate compression level.
 */
public class JPEGOptimizer extends BaseOptimizer {

  public static BufferedImage readJpeg(InputStream is)
      throws ImageReadException, IOException {
    byte[] bytes = IOUtils.toByteArray(is);
    // We cant use Sanselan to read JPEG but we can use it to read all the metadata which is
    // where most security issues reside anyway in ImageIO
    Sanselan.getMetadata(bytes, null);
    byte[] iccBytes = Sanselan.getICCProfileBytes(bytes);
    if (iccBytes != null && iccBytes.length > 0) {
      ICC_Profile iccProfile = Sanselan.getICCProfile(bytes, null);
      if (iccProfile == null) {
        throw new ImageReadException("Image has ICC but it is corrupt and cannot be read");
      }
    }
    return ImageIO.read(new ByteArrayInputStream(bytes));
  }

  private boolean usePng;

  public JPEGOptimizer(OptimizerConfig config, HttpResponseBuilder response) {
    super(config, response);
  }

  public JPEGOptimizer(OptimizerConfig config, HttpResponseBuilder response,
                       JpegImageUtils.JpegImageParams sourceImageParams) {
    super(config, response, sourceImageParams);
  }

  @Override
  protected void rewriteImpl(BufferedImage image) throws IOException {
    int pngLength = Integer.MAX_VALUE;
    if (config.isJpegConversionAllowed()) {
      // Create a new optimizer config and disable JPEG conversion
      OptimizerConfig pngConfig = new OptimizerConfig(config.getMaxInMemoryBytes(),
          config.getMaxPaletteSize(), false, config.getJpegCompression(),
          config.getMinThresholdBytes(), config.getJpegHuffmanOptimization(),
          config.getJpegRetainSubsampling());

      // Output the image as PNG
      PNGOptimizer pngOptimizer = new PNGOptimizer(pngConfig, response);
      pngOptimizer.rewriteImpl(image);

      if (pngOptimizer.getRewrittenImage()  != null) {
        // PNG was better than original so use it
        minBytes = pngOptimizer.getRewrittenImage();
        minLength = minBytes.length;
        pngLength = minLength;
      }
    }

    // Write as standard JPEG using the configured default compression level
    write(image);

    // JPEG did not beat PNG
    if (pngLength == minLength) {
      usePng = true;
    }
  }

  @Override
  protected String getOutputContentType() {
    if (usePng) {
      return "image/png";
    }
    return "image/jpeg";
  }

  @Override
  protected String getOriginalContentType() {
    return "image/jpeg";
  }

  @Override
  protected String getOriginalFormatName() {
    return "jpeg";
  }
}
