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
import org.apache.sanselan.common.BinaryFileParser;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.sanselan.formats.jpeg.JpegUtils;
import org.apache.sanselan.formats.jpeg.JpegConstants;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.plugins.jpeg.JPEGQTable;

/**
 * Utility functions for jpeg image introspection.
 */
public class JpegImageUtils {
  private static final Logger LOG = Logger.getLogger(ImageUtils.class.getName());
  private static final int END_OF_IMAGE_MARKER = 0xffd9;
  private static final String INVALID_JPEG_ERROR_MSG = "Not a Valid JPEG File";
  private static final int HUFFMAN_TABLE_MARKER = 0xffc4;
  private static final int QUANTIZATION_TABLE_MARKER = 0xffdb;
  private static final int MAX_DC_SYMBOLS = 12;
  private static final int MAX_AC_SYMBOLS = 162;

  private JpegImageUtils() {}

  /**
   * Various subsampling modes supported by Jpeg and the corresponding values for
   * this integer.
   *   4:4:4 subsampling -> 0x11 -> 17
   *   4:2:2 subsampling -> 0x21 -> 33
   *   4:2:0 subsampling -> 0x22 -> 34
   *   4:1:1 subsampling -> 0x41 -> 65
   */
  public static enum SamplingModes {
    UNKNOWN(-2),
    DEFAULT(-1),
    YUV444(17),
    YUV422(33),
    YUV420(34),
    YUV411(65);

    private SamplingModes(int mode) {
      this.mode = mode;
    }

    public int getModeValue() {
      return mode;
    }

    private int mode;
  }

  public static class JpegImageParams {
    private SamplingModes mode;
    private boolean huffmanOptimized;
    private float approxQualityFactor;
    private float lumaQualityFactor = -1;
    private float chromaQualityFactor = -1;

    private final int[] k1LumaQuantTable = JPEGQTable.K1Luminance.getTable();
    private final int[] k2ChromaQuantTable = JPEGQTable.K2Chrominance.getTable();

    private int[][] tables = new int[2][64];
    private int lumaIndex = -1;
    private int chromaIndex = -1;

    JpegImageParams(SamplingModes mode, boolean huffmanOptimized, float approxQualityFactor) {
      this.mode = mode;
      this.huffmanOptimized = huffmanOptimized;
      this.approxQualityFactor = approxQualityFactor;
    }

    public SamplingModes getSamplingMode() {
      return mode;
    }

    public void setSamplingMode(int samplingMode) {
      for (SamplingModes mode : SamplingModes.values()) {
        if (samplingMode == mode.getModeValue()) {
          this.mode = mode;
          return;
        }
      }

      mode = SamplingModes.UNKNOWN;
      LOG.log(Level.WARNING, "Unable to read subsampling information for Jpeg Image");
    }

    public boolean isHuffmanOptimized() {
      return huffmanOptimized;
    }

    public void setHuffmanOptimized(boolean huffmanOptimized) {
      this.huffmanOptimized = huffmanOptimized;
    }

    public void setLumaIndex(int index) {
      this.lumaIndex = index;
    }

    public void setChromaIndex(int index) {
      this.chromaIndex = index;
    }

    /**
     * Quality is defined in terms of the base quantization tables used by encoder.
     * Q = quant table, q = compression quality  and S = table used by encoder,
     * Encoder does the following.
     * if q > 0.5 then Q = 2 - 2*q*S otherwise Q = (0.5/q)*S.
     *
     * Since we dont have access to the table used by encoder. But it is generally close
     * to the standard table defined by JPEG. Hence, we approx by taking sum of all values
     * of the standard JPEG table and comparing with sum of all values of quant table.
     *
     * @param table quantization table specified in the jpeg header.
     * @param stdTable reference quantization table specified in jpeg standard.
     * @return approximate compression quality which lies in interval [0.0, 1.0].
     */
    public float approximateQuality(int[] table, int[] stdTable) {
      int total = 0;
      int stdTotal = 0;
      for (int i = 0; i < 64; i++) {
        total += table[i];
        stdTotal += stdTable[i];
      }

      float scaleFactor = (total - 32F)/stdTotal;

      float approxChannelQuality;
      if (scaleFactor > 1.0) {
        approxChannelQuality = 0.5F / scaleFactor;
      } else {
        approxChannelQuality = (2.0F - scaleFactor) / 2.0F;
      }
      return approxChannelQuality;
    }

    /**
     * Adds quantization table to image data.
     *
     * @param tableIndex quantization table index.
     * @param table quantization table that is used in while encoding.
     */
    public void addQTable(int tableIndex, int[] table) {
      if (tableIndex == 0 || tableIndex == 1) {
        System.arraycopy(table, 0, tables[tableIndex], 0, table.length);
      }
    }

    public float getChromaQualityFactor() {
      if (chromaQualityFactor < 0 && chromaIndex >= 0) {
        chromaQualityFactor = approximateQuality(tables[chromaIndex], k2ChromaQuantTable);
      }
      return chromaQualityFactor;
    }

    public float getLumaQualityFactor() {
     if (lumaQualityFactor < 0 && lumaIndex >= 0) {
        lumaQualityFactor = approximateQuality(tables[lumaIndex], k1LumaQuantTable);
      }
      return lumaQualityFactor;
    }

    public float getApproxQualityFactor() {
      if (approxQualityFactor < 0) {
        approxQualityFactor = (getLumaQualityFactor() + 2 * getChromaQualityFactor()) / 3.0F;
      }

      return approxQualityFactor;
    }
  }

  /**
   * This function tries to extract various information from jpeg image like subsampling, jpeg
   * compression quality and whether huffman optimzation is applied on the image data.
   *
   * @param is input stream comprisng the image data.
   * @param filename of the image.
   */
  public static JpegImageParams getJpegImageData(InputStream is, String filename)
      throws IOException, ImageReadException {
    final JpegImageParams imageParams = new JpegImageParams(SamplingModes.UNKNOWN, false, -1);

    JpegUtils.Visitor visitor = new JpegUtils.Visitor() {
      BinaryFileParser binaryParser = new BinaryFileParser();

      // return false to exit before reading image data.
      public boolean beginSOS() {
        return false;
      }

      public void visitSOS(int marker, byte markerBytes[],
          byte imageData[]) {
      }

      // return false to exit traversal.
      public boolean visitSegment(int marker, byte markerBytes[], int markerLength,
          byte markerLengthBytes[], byte segmentData[]) throws ImageReadException, IOException {

        if (marker == END_OF_IMAGE_MARKER)
          return false;

        if ((marker == JpegConstants.SOF0Marker) || (marker == JpegConstants.SOF2Marker)) {
          parseSOFSegment(markerLength, segmentData);
        } else if (marker == HUFFMAN_TABLE_MARKER) {
          parseHuffmanTables(markerLength, segmentData);
        } else if (marker == QUANTIZATION_TABLE_MARKER) {
          parseQuantizationTables(markerLength, segmentData);
        }

        return true;
      }

      /**
       * This function tries to extract the subsampling information from the JPEG image using
       * either 'SOF0' or 'SOF2' segment.
       * The structure of the 'SOF' marker is as follows.
       *   - data precision (1 byte) in bits/sample,
       *   - image height (2 bytes, little endian),
       *   - image width (2 bytes, little endian),
       *   - number of components (1 byte), usually 1 = grey scaled, 3 = color YCbCr
       *     or YIQ, 4 = color CMYK)
       *   - for each component: 3 bytes
       *     - component id (1 = Y, 2 = Cb, 3 = Cr, 4 = I, 5 = Q)
       *     - sampling factors (bit 0-3 vertical sampling, 4-7 horizontal sampling)
       *     - quantization table index
       *
       * @param markerLength length of the SOF marker.
       * @param segmentData actual bytes representing the segment.
       */
      private void parseSOFSegment(int markerLength, byte[] segmentData)
          throws IOException, ImageReadException {
        // parse the SOF Marker.
        int toBeProcessed = markerLength - 2;
        int numComponents = 0;
        InputStream is = new ByteArrayInputStream(segmentData);

        // Skip precision(1 Byte), height(2 Bytes), width(2 bytes) bytes.
        if (toBeProcessed > 6) {
          binaryParser.skipBytes(is, 5, INVALID_JPEG_ERROR_MSG);
          numComponents = binaryParser.readByte("Number_of_components", is,
              "Unable to read Number of components from SOF marker");
          toBeProcessed -= 6;
        } else {
          LOG.log(Level.WARNING, "Failed to SOF marker");
          return;
        }

        // TODO(satya): Extend this library to gray scale images.
        if (numComponents == 3 && toBeProcessed == 9) {
          // Process 'Luma' Channel.
          // Skipping the component Id field.
          binaryParser.skipBytes(is, 1, INVALID_JPEG_ERROR_MSG);
          imageParams.setSamplingMode(binaryParser.readByte("Sampling Factors", is,
              "Unable to read the sampling factor from the 'Y' channel component spec"));
          imageParams.setLumaIndex(binaryParser.readByte("Quantization Table Index", is,
              "Unable to read Quantization table index of 'Y' channel"));

          // Process 'Chroma' Channel.
          // Skipping the component Id and sampling factor fields.
          binaryParser.skipBytes(is, 2, INVALID_JPEG_ERROR_MSG);
          imageParams.setChromaIndex(binaryParser.readByte("Quantization Table Index", is,
              "Unable to read Quantization table index of 'Cb' Channel"));
        } else {
          LOG.log(Level.WARNING, "Failed to Component Spec from SOF marker");
        }
      }


      /**
       * This function tries to parse the Quantizations tables and adds them to JpegImageData
       * object. If segmentData has more bytes after parsing first QT, that means DQT segment has
       * multiple quantization tables. We allow multiple quant tables to have same tableIndex,
       * and the latter one overrides the previous one. we currently parse upto 2 quantization
       * tables.
       * The structure of the DQT (Define Quantization Table) segment.
       *   - QT information (1 byte): (bit 0 = LSB and bit 7 = MSB)
       *     bit 3..0: index of QT (3..0, otherwise error)
       *     bit 7..4: precision of QT, 0 means 8 bit, 1 means 16 bit, otherwise bad input
       *   - n bytes QT values, n = 64*(precision+1)
       *
       * @param markerLength length of the DQT marker.
       * @param segmentData actual bytes representing the segment.
       */
      private void parseQuantizationTables(int markerLength, byte[] segmentData)
          throws ImageReadException, IOException {
        InputStream is = new ByteArrayInputStream(segmentData);
        int toBeProcessed = markerLength - 2;
        while (toBeProcessed > 1) {
          int tableInfo = binaryParser.readByte("Quantization Table Info", is,
                                                "Not able to read Quantization Table Info");
          toBeProcessed--;
          int tableIndex = tableInfo & 0x0f;
          int precision = tableInfo >> 4;
          if (toBeProcessed < 64*(precision + 1)) {
            return;
          }

          int[] quanTable = new int[64];
          for (int i = 0; i < 64; i++) {
            quanTable[i] = (precision == 0) ?
                binaryParser.readByte("Reading", is, "Reading Quanization Table Failed") :
                binaryParser.read2Bytes("Reading", is, "Reading Quantization Table Failed");
          }
          imageParams.addQTable(tableIndex, quanTable);
          toBeProcessed -= 64*(precision + 1);
        }
      }

      /**
       * This functions parses the huffman table and try to figure out if huffman
       * optimizations are applied on the image or not. If segmentData has more bytes after
       * parsing first HT, that means DHT segment has multiple huffman tables.
       * Structure of DHT (Define Huffman Table) segment.
       *   - HT information (1 byte): (bit 0 = LSB and bit 7 = MSB)
       *     bit 3..0: index of HT (3..0, otherwise error)
       *     bit 4   : type of HT, 0 = DC table, 1 = AC table
       *     bit 7..5: not used, must be 0
       *   - 16 bytes: number of symbols with codes of length 1..16, the sum of these
       *     bytes is the total number of codes, which must be <= 256
       *   - n bytes: table containing the symbols in order of increasing code length
       *     (n = total number of codes)
       *
       * @param markerLength length of the DHT marker.
       * @param segmentData actual bytes representing the segment.
       */
      private void parseHuffmanTables(int markerLength, byte[] segmentData)
          throws ImageReadException, IOException {
        InputStream is = new ByteArrayInputStream(segmentData);

        int toBeProcessed = markerLength -2;
        while (toBeProcessed > 1) {
          // Reading the table info byte.
          int tableInfo = binaryParser.readByte("Huffman Table Info", is,
                                                "Not able to read Huffman Table Info");
          toBeProcessed--;

          // Reading the counts of symbols from length 1...16.
          if (toBeProcessed < 16) {
            return;
          }
          int numSymbols =0;
          for (int i = 0; i < 16; i++) {
            numSymbols += binaryParser.readByte("Num symbols", is,
                                                "Not able to read num symbols");
          }
          toBeProcessed -= 16 + numSymbols;

          // It is highly unlikely that a huffman optimized image has same number of
          // symbols as the standard huffman table. So, if DC tables has less than 12 symbols
          // (OR) an AC table has less than 162 symbols it is most likely optimized.
          int tableType = (tableInfo>>4) & 1;
          if ((tableType == 0 && numSymbols != MAX_DC_SYMBOLS) ||
              (tableType == 1 && numSymbols != MAX_AC_SYMBOLS)) {
            imageParams.setHuffmanOptimized(true);
            return;
          }
        }
      }
    };

    new JpegUtils().traverseJFIF(new ByteSourceInputStream(is, filename), visitor);
    return imageParams;
  }
}
