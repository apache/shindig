/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

// File copied directly from Closure Library (http://code.google.com/p/closure-library)
// Imported into Shindig w/ slight namespacing modifications and change from
// prototype-style to closure (ironically) style JS objects.

/**
 * @fileoverview SHA-1 cryptographic hash.
 * Variable names follow the notation in FIPS PUB 180-3:
 * http://csrc.nist.gov/publications/fips/fips180-3/fips180-3_final.pdf.
 *
 * Usage:
 *   var sha1 = shindig.sha1();
 *   sha1.update(bytes);
 *   var hash = sha1.digest();
 */

/**
 * SHA-1 cryptographic hash constructor.
 *
 * The properties declared here are discussed in the above algorithm document.
 * @constructor
 */
shindig.sha1 = (function() {
  /**
   * Holds the previous values of accumulated variables a-e in the compress_
   * function.
   * @type {Array.<number>}
   * @private
   */
  var chain_ = [];

  /**
   * A buffer holding the partially computed hash result.
   * @type {Array.<number>}
   * @private
   */
  var buf_ = [];

  /**
   * An array of 80 bytes, each a part of the message to be hashed.  Referred to
   * as the message schedule in the docs.
   * @type {Array.<number>}
   * @private
   */
  var W_ = [];

  /**
   * Contains data needed to pad messages less than 64 bytes.
   * @type {Array.<number>}
   * @private
   */
  var pad_ = [];

  pad_[0] = 128;
  for (var i = 1; i < 64; ++i) {
    pad_[i] = 0;
  }

  /**
   * Resets the internal accumulator.
   */
  function reset() {
    chain_[0] = 0x67452301;
    chain_[1] = 0xefcdab89;
    chain_[2] = 0x98badcfe;
    chain_[3] = 0x10325476;
    chain_[4] = 0xc3d2e1f0;

    inbuf_ = 0;
    total_ = 0;
  }

  /**
   * Internal helper performing 32 bit left rotate.
   * @param {number} w 32-bit integer to rotate.
   * @param {number} r Bits to rotate left by.
   * @return {number} w rotated left by r bits.
   * @private
   */
  function rotl_(w, r) {
    return ((w << r) | (w >>> (32 - r))) & 0xffffffff;
  }

  /**
   * Internal compress helper function.
   * @param {Array} buf containing block to compress.
   * @private
   */
  function compress_(buf) {
    var W = W_;

    // get 16 big endian words
    for (var i = 0; i < 64; i += 4) {
      var w = (buf[i] << 24) |
              (buf[i + 1] << 16) |
              (buf[i + 2] << 8) |
              (buf[i + 3]);
      W[i / 4] = w;
    }

    // expand to 80 words
    for (var i = 16; i < 80; i++) {
      W[i] = rotl_(W[i - 3] ^ W[i - 8] ^ W[i - 14] ^ W[i - 16], 1);
    }

    var a = chain_[0];
    var b = chain_[1];
    var c = chain_[2];
    var d = chain_[3];
    var e = chain_[4];
    var f, k;

    for (var i = 0; i < 80; i++) {
      if (i < 40) {
        if (i < 20) {
          f = d ^ (b & (c ^ d));
          k = 0x5a827999;
        } else {
          f = b ^ c ^ d;
          k = 0x6ed9eba1;
        }
      } else {
        if (i < 60) {
          f = (b & c) | (d & (b | c));
          k = 0x8f1bbcdc;
        } else {
          f = b ^ c ^ d;
          k = 0xca62c1d6;
        }
      }

      var t = (rotl_(a, 5) + f + e + k + W[i]) & 0xffffffff;
      e = d;
      d = c;
      c = rotl_(b, 30);
      b = a;
      a = t;
    }

    chain_[0] = (chain_[0] + a) & 0xffffffff;
    chain_[1] = (chain_[1] + b) & 0xffffffff;
    chain_[2] = (chain_[2] + c) & 0xffffffff;
    chain_[3] = (chain_[3] + d) & 0xffffffff;
    chain_[4] = (chain_[4] + e) & 0xffffffff;
  }

  /**
   * Adds a byte array to internal accumulator.
   * @param {Array.<number>} bytes to add to digest.
   * @param {number=} opt_length is # of bytes to compress.
   */
  function update(bytes, opt_length) {
    if (!opt_length) {
      opt_length = bytes.length;
    }

    var n = 0;

    // Optimize for 64 byte chunks at 64 byte boundaries.
    if (inbuf_ == 0) {
      while (n + 64 < opt_length) {
        compress_(bytes.slice(n, n + 64));
        n += 64;
        total_ += 64;
      }
    }

    while (n < opt_length) {
      buf_[inbuf_++] = bytes[n++];
      total_++;

      if (inbuf_ == 64) {
        inbuf_ = 0;
        compress_(buf_);

        // Pick up 64 byte chunks.
        while (n + 64 < opt_length) {
          compress_(bytes.slice(n, n + 64));
          n += 64;
          total_ += 64;
        }
      }
    }
  }

  /**
   * @return {Array} byte[20] containing finalized hash.
   */
  function digest() {
    var digest = [];
    var totalBits = total_ * 8;

    // Add pad 0x80 0x00*.
    if (inbuf_ < 56) {
      update(pad_, 56 - inbuf_);
    } else {
      update(pad_, 64 - (inbuf_ - 56));
    }

    // Add # bits.
    for (var i = 63; i >= 56; i--) {
      buf_[i] = totalBits & 255;
      totalBits >>>= 8;
    }

    compress_(buf_);

    var n = 0;
    for (var i = 0; i < 5; i++) {
      for (var j = 24; j >= 0; j -= 8) {
        digest[n++] = (chain_[i] >> j) & 255;
      }
    }

    return digest;
  }

  reset();

  return {
    reset: reset,
    update: update,
    digest: digest
  };
});
