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

import com.google.common.collect.Maps;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * Utility functions for image processing and introspection.
 */
public class ImageUtils {

  /**
   * Convert an image to a paletteized one. Will not create a palette above a fixed
   * number of entries
   */
  public static BufferedImage palettize(BufferedImage img, int maxEntries) {
    // Just because an image has a palette doesnt mean it has a good one
    // so we re-index even if its an IndexColorModel
    int addedCount = 0;
    Map<Integer, Integer> added = Maps.newHashMap();
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        if (!added.containsKey(img.getRGB(x, y))) {
          added.put(img.getRGB(x, y), addedCount++);
        }
        if (added.size() > maxEntries) {
          // Bail if palette becomes too large
          return null;
        }
      }
    }
    int[] cmap = new int[added.size()];
    for (int c : added.keySet()) {
      cmap[added.get(c)] = c;
    }

    int bitCount = 1;
    while (added.size() >> bitCount != 0) {
      bitCount *= 2;
    }

    IndexColorModel icm = new IndexColorModel(bitCount,
        added.size(), cmap, 0, DataBuffer.TYPE_BYTE, null);

    // Check if generated palette matched original
    if (img.getColorModel() instanceof IndexColorModel) {
      IndexColorModel originalModel = (IndexColorModel)img.getColorModel();
      if (originalModel.getPixelSize() == icm.getPixelSize() &&
          originalModel.getMapSize() == icm.getMapSize()) {
        // Old model already had efficient palette
        return null;
      }
    }

    // Be careful to assign correctly assign byte packing method based on pixel size
    BufferedImage dst =
        new BufferedImage(img.getWidth(), img.getHeight(),
            icm.getPixelSize() < 8 ? BufferedImage.TYPE_BYTE_BINARY :
                BufferedImage.TYPE_BYTE_INDEXED, icm);

    WritableRaster wr = dst.getRaster();
    for (int y = 0; y < dst.getHeight(); y++) {
      for (int x = 0; x < dst.getWidth(); x++) {
        wr.setSample(x, y, 0, added.get(img.getRGB(x, y)));
      }
    }
    return dst;
  }

  /**
   * Convert an image to a direct color map
   */
  public static BufferedImage depalettize(BufferedImage img, int maxBytes) {
    // Even is we use an RGB buffer instead of RGBA it still uses 4 bytes in memory
    if (img.getWidth() * img.getHeight() * 4 > maxBytes) {
      // Image is too large to depalettize
      return null;
    }
    ColorModel colorModel = img.getColorModel();
    if (colorModel instanceof IndexColorModel) {
      IndexColorModel indexModel = (IndexColorModel)colorModel;
      return indexModel.convertToIntDiscrete(img.getData(), false);
    }
    return null;
  }


  /**
   * Check if an image is completely opaque
   */
  public static boolean isOpaque(BufferedImage img) {
    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        if ((img.getRGB(x,y) & 0xff000000) != 0xff000000) {
          return false;
        }
      }
    }
    return true;
  }
}
