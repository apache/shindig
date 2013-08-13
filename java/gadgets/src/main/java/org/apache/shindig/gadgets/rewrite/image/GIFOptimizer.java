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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Optimize GIF images by converting them to PNGs or even JPEGs depending on content
 */
public class GIFOptimizer extends PNGOptimizer {

  public static BufferedImage readGif(InputStream is)
      throws ImageReadException, IOException {
    return Sanselan.getBufferedImage(is);
  }

  private boolean usePng;

  public GIFOptimizer(OptimizerConfig config, HttpResponseBuilder response) {
    super(config, response);
  }

  @Override
  protected void rewriteImpl(BufferedImage image) throws IOException {
    if (!ImageUtils.isOpaque(image)) {
      // We can rewrite transparent GIFs to PNG but for IE6 it requires the use of
      // the AlphaImageReader and some pain. Deferring this until that is proven to work

      // Write to strip any metadata and re-compute the palette. We allow arbitrary large palettes
      // here as if the image is already in a direct color model it will already have been
      // constrained by the max in-mem constraint.
      write(ImageUtils.palettize(image, Integer.MAX_VALUE));
    } else {
      usePng = true;
      outputter = new ImageIOOutputter(ImageIO.getImageWritersByFormatName("png").next(), null);
      super.rewriteImpl(image);
    }
  }

  @Override
  protected String getOriginalContentType() {
    return "image/gif";
  }

  @Override
  protected String getOutputContentType() {
    if (usePng) {
      return super.getOutputContentType();
    }
    return "image/gif";
  }

  @Override
  protected String getOriginalFormatName() {
    return "gif";
  }
}
