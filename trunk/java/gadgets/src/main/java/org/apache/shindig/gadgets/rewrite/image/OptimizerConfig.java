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

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Configuration settings for the optimizer
 */
public class OptimizerConfig {

  private final int maxInMemoryBytes;
  private final int maxPaletteSize;
  private final boolean jpegConversionAllowed;
  private final float jpegCompression;
  private final int minThresholdBytes;
  private final boolean jpegHuffmanOptimization;
  private final boolean jpegRetainSubsampling;

  @Inject
  public OptimizerConfig(
      @Named("shindig.image-rewrite.max-inmem-bytes") int maxInMemoryBytes,
      @Named("shindig.image-rewrite.max-palette-size") int maxPaletteSize,
      @Named("shindig.image-rewrite.allow-jpeg-conversion") boolean jpegConversionAllowed,
      @Named("shindig.image-rewrite.jpeg-compression") float jpegCompression,
      @Named("shindig.image-rewrite.min-threshold-bytes") int minThresholdBytes,
      @Named("shindig.image-rewrite.jpeg-huffman-optimization") boolean jpegHuffmanOptimization,
      @Named("shindig.image-rewrite.jpeg-retain-subsampling") boolean jpegRetainSubsampling) {
    this.maxInMemoryBytes = maxInMemoryBytes;
    this.maxPaletteSize = maxPaletteSize;
    this.jpegConversionAllowed = jpegConversionAllowed;
    // Constrain jpeg compression to between 0.9 and 0.5 so its not pointless
    // to attempt nor is it too lossy.
    this.jpegCompression = Math.min(0.9f,Math.max(0.5f, jpegCompression));
    this.minThresholdBytes = minThresholdBytes;
    this.jpegHuffmanOptimization = jpegHuffmanOptimization;
    this.jpegRetainSubsampling = jpegRetainSubsampling;
  }

  /**
   * Defaults for usage in tests.
   */
  public OptimizerConfig() {
    this(1024 * 1024, 256, true, 0.90f, 200, false, false);
  }

  /**
   * The maximum allowed in-memory size of a parsed image. Used to protect system
   * from very large memory allocations
   */
  public int getMaxInMemoryBytes() {
    return maxInMemoryBytes;
  }

  /**
   * The maximum no. of palette entries to create when attempting to palettize an
   * image before quitting.
   */
  public int getMaxPaletteSize() {
    return maxPaletteSize;
  }

  /**
   * Allow conversion from and to JPEG for other image types that are fully opaque.
   */
  public boolean isJpegConversionAllowed() {
    return jpegConversionAllowed;
  }

  /**
   * The compression ratio to use when compressing JPEG images
   * A value between 0.5 and 0.9.
   */
  public float getJpegCompression() {
    return jpegCompression;
  }

  /**
   * The threshold in bytes below which we do not attempt to rewite
   * an image. Value should be chosen based on knowledge of MTU sizes
   */
  public int getMinThresholdBytes() {
    return minThresholdBytes;
  }

  /**
   * Indicate if we want to do huffman optimization while enocding the jpeg's.
   */
  public boolean getJpegHuffmanOptimization() {
    return jpegHuffmanOptimization;
  }

  /**
   * Indicate if we want to do retian original jpeg subsampling while encoding the jpeg's.
   */
  public boolean getJpegRetainSubsampling() {
    return jpegRetainSubsampling;
  }

}
