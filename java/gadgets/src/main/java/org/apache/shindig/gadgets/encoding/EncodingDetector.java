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
package org.apache.shindig.gadgets.encoding;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Attempts to determine the encoding of a given string.
 *
 * Highly skewed towards common encodings (UTF-8 and Latin-1).
 */
public final class EncodingDetector {
  private EncodingDetector() {}

  public static class FallbackEncodingDetector {
    public Charset detectEncoding(byte[] input) {
      // Fall back to the incredibly slow ICU. It might be better to just skip this entirely.
      CharsetDetector detector = new CharsetDetector();
      detector.setText(input);
      CharsetMatch match = detector.detect();
      return Charset.forName(match.getName().toUpperCase());
    }
  }

  /**
   * Returns the detected encoding of the given byte array.
   *
   * @param input The data to detect the encoding for.
   * @param assume88591IfNotUtf8 True to assume that the encoding is ISO-8859-1 (the standard
   *     encoding for HTTP) if the bytes are not valid UTF-8. Only recommended if you can reasonably
   *     expect that other encodings are going to be specified. Full encoding detection is very
   *     expensive!
   * @param alternateDecoder specify a fallback encoding detection.
   *     Only used if assume88591IfNotUtf8 is false.
   * @return The detected encoding.
   */
  public static Charset detectEncoding(byte[] input, boolean assume88591IfNotUtf8,
      FallbackEncodingDetector alternateDecoder) {
    if (looksLikeValidUtf8(input)) {
      return Charsets.UTF_8;
    }

    if (assume88591IfNotUtf8) {
      return Charsets.ISO_8859_1;
    }

    // Fall back encoding:
    return alternateDecoder.detectEncoding(input);
  }

  /**
   * A pretty good test that something is UTF-8. There are many sequences that will pass here that
   * aren't valid UTF-8 due to the requirement that the shortest possible sequence always be used.
   * We're ok with this behavior because the main goal is speed.
   */
  private static boolean looksLikeValidUtf8(byte[] input) {
    int i = 0;
    if (input.length >= 3 &&
       (input[0] & 0xFF) == 0xEF &&
       (input[1] & 0xFF) == 0xBB &&
       (input[2] & 0xFF) == 0xBF) {
      // Skip BOM.
      i = 3;
    }

    int endOfSequence;
    for (int j = input.length; i < j; ++i) {
      int bite = input[i];
      if ((bite & 0x80) == 0) {
        continue; // ASCII
      }

      // Determine number of bytes in the sequence.
      if ((bite & 0x0E0) == 0x0C0) {
        endOfSequence = i + 1;
      } else if ((bite & 0x0F0) == 0x0E0) {
        endOfSequence = i + 2;
      } else if ((bite & 0x0F8) == 0xF0) {
        endOfSequence = i + 3;
      } else {
        // Not a valid utf-8 byte sequence. Skip.
        return false;
      }

      if (endOfSequence >= j) {
        // End of sequence reached, not a valid sequence
        return false;
      }

      while (i < endOfSequence) {
        i++;
        bite = input[i];
        if ((bite & 0xC0) != 0x80) {
          // High bit not set, not a valid sequence
          return false;
        }
      }
    }
    return true;
  }
}
