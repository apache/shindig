/*

        Copyright 2006-2009 OpenAjax Alliance

        Licensed under the Apache License, Version 2.0 (the "License"); 
        you may not use this file except in compliance with the License. 
        You may obtain a copy of the License at
        
                http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software 
        distributed under the License is distributed on an "AS IS" BASIS, 
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
        See the License for the specific language governing permissions and 
        limitations under the License.
*/
// SMASH.CRYPTO
//
// Small library containing some minimal crypto functionality for a
// - a hash-function: SHA-1 (see FIPS PUB 180-2 for definition)
//     BigEndianWord[5] <- smash.crypto.sha1( BigEndianWord[*] dataWA, int lenInBits)
//
// - a message authentication code (MAC): HMAC-SHA-1 (RFC2104/2202)
//     BigEndianWord[5] <- smash.crypto.hmac_sha1(
//                            BigEndianWord[3-16] keyWA, 
//                            Ascii or Unicode string dataS,
//		 		 		       int chrsz (8 for Asci/16 for Unicode)
//
// - pseudo-random number generator (PRNG): HMAC-SHA-1 in counter mode, following
//   Barak & Halevi, An architecture for robust pseudo-random generation and applications to /dev/random, CCS 2005
//     rngObj <- smash.crypto.newPRNG( String[>=12] seedS)
//   where rngObj has methods
//     addSeed(String seed)
//     BigEndianWord[len] <- nextRandomOctets(int len)
//     Base64-String[len] <- nextRandomB64Str(int len)
//   Note: HMAC-SHA1 in counter-mode does not provide forward-security on corruption. 
//         However, the PRNG state is kept inside a closure. So if somebody can break the closure, he probably could
//         break a whole lot more and forward-security of the prng is not the highest of concerns anymore :-)

if ( typeof OpenAjax._smash == 'undefined' ) { OpenAjax._smash = {}; }

OpenAjax._smash.crypto = {

  // Some utilities
  // convert a string to an array of big-endian words
  'strToWA': function (/* Ascii or Unicode string */ str, /* int 8 for Asci/16 for Unicode */ chrsz){
    var bin = Array();
    var mask = (1 << chrsz) - 1;
    for(var i = 0; i < str.length * chrsz; i += chrsz)
      bin[i>>5] |= (str.charCodeAt(i / chrsz) & mask) << (32 - chrsz - i%32);
    return bin;
  },


  // MAC
  'hmac_sha1' : function(
        /* BigEndianWord[3-16]*/             keyWA,
       /* Ascii or Unicode string */       dataS,
       /* int 8 for Asci/16 for Unicode */ chrsz)
  {
    // write our own hmac derived from paj's so we do not have to do constant key conversions and length checking ...
    var ipad = Array(16), opad = Array(16);
    for(var i = 0; i < 16; i++) {
      ipad[i] = keyWA[i] ^ 0x36363636;
      opad[i] = keyWA[i] ^ 0x5C5C5C5C;
    }

    var hash = this.sha1( ipad.concat(this.strToWA(dataS, chrsz)), 512 + dataS.length * chrsz);
    return     this.sha1( opad.concat(hash), 512 + 160);
  },


  // PRNG factory method
  // see below 'addSeed', 'nextRandomOctets' & 'nextRandomB64Octets' for public methods of returnd prng object
  'newPRNG' : function (/* String[>=12] */ seedS) {
    var that = this;

    // parameter checking
    // We cannot really verify entropy but obviously the string must have at least a minimal length to have enough entropy
    // However, a 2^80 security seems ok, so we check only that at least 12 chars assuming somewhat random ASCII
    if ( (typeof seedS != 'string') || (seedS.length < 12) ) {
      alert("WARNING: Seed length too short ...");
    }

    // constants
    var __refresh_keyWA = [ 0xA999, 0x3E36, 0x4706, 0x816A,
    		 		 		     0x2571, 0x7850, 0xC26C, 0x9CD0,
    		 		 		     0xBA3E, 0xD89D, 0x1233, 0x9525,
    		 		 		     0xff3C, 0x1A83, 0xD491, 0xFF15 ]; // some random key for refresh ...

    // internal state
    var _keyWA = []; // BigEndianWord[5]
    var _cnt = 0;  // int

    function extract(seedS) {
      return that.hmac_sha1(__refresh_keyWA, seedS, 8);
    }

    function refresh(seedS) {
      // HMAC-SHA1 is not ideal, Rijndal 256bit block/key in CBC mode with fixed key might be better
      // but to limit the primitives and given that we anyway have only limited entropy in practise
      // this seems good enough
      var uniformSeedWA = extract(seedS);
      for(var i = 0; i < 5; i++) {
        _keyWA[i] ^= uniformSeedWA[i];
      }
    }

    // inital state seeding
    refresh(seedS);

    // public methods
    return {
      // Mix some additional seed into the PRNG state
      'addSeed'         : function (/* String */ seed) {
        // no parameter checking. Any added entropy should be fine ...
        refresh(seed);
      },


      // Get an array of len random octets
      'nextRandomOctets' : /* BigEndianWord[len] <- */ function (/* int */ len) {
		 var randOctets = [];
		 while (len > 0) {
		   _cnt+=1;
		   var nextBlock = that.hmac_sha1(_keyWA, (_cnt).toString(16), 8);
		   for (i=0; (i < 20) & (len > 0); i++, len--) {
		     randOctets.push( (nextBlock[i>>2] >> (i % 4) ) % 256);
		   }
		   // Note: if len was not a multiple 20, some random octets are ignored here but who cares ..
		 }
		 return randOctets;
      },


      // Get a random string of Base64-like (see below) chars of length len
      // Note: there is a slightly non-standard Base64 with no padding and '-' and '_' for '+' and '/', respectively
      'nextRandomB64Str' : /* Base64-String <- */ function (/* int */ len) {
		 var b64StrMap = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

		 var randOctets = this.nextRandomOctets(len);
		 var randB64Str = '';
		 for (var i=0; i < len; i++) {
		   randB64Str += b64StrMap.charAt(randOctets[i] & 0x3F);
		 }
        return randB64Str;
      }

    }
  },


  // Digest function:
  // BigEndianWord[5] <- sha1( BigEndianWord[*] dataWA, int lenInBits)
  'sha1' : function(){
    // Note: all Section references below refer to FIPS 180-2.

    // private utility functions

    // - 32bit addition with wrap-around
    var add_wa = function (x, y){
      var lsw = (x & 0xFFFF) + (y & 0xFFFF);
      var msw = (x >> 16) + (y >> 16) + (lsw >> 16);
      return (msw << 16) | (lsw & 0xFFFF);
    }

    // - 32bit rotatate left
    var rol = function(num, cnt) {
      return (num << cnt) | (num >>> (32 - cnt));
    }

    // - round-dependent function f_t from Section 4.1.1
    function sha1_ft(t, b, c, d) {
      if(t < 20) return (b & c) | ((~b) & d);
      if(t < 40) return b ^ c ^ d;
      if(t < 60) return (b & c) | (b & d) | (c & d);
      return b ^ c ^ d;
    }

    // - round-dependent SHA-1 constants from Section 4.2.1
    function sha1_kt(t) {
      return (t < 20) ?  1518500249 :
             (t < 40) ?  1859775393 :
             (t < 60) ? -1894007588 :
          /* (t < 80) */ -899497514 ;
    }

    // main algorithm. 
    return function( /* BigEndianWord[*] */ dataWA, /* int */ lenInBits) {

      // Section 6.1.1: Preprocessing
      //-----------------------------
      // 1. padding:  (see also Section 5.1.1)
      //  - append one 1 followed by 0 bits filling up 448 bits of last (512bit) block
      dataWA[lenInBits >> 5] |= 0x80 << (24 - lenInBits % 32);
      //  - encode length in bits in last 64 bits
      //    Note: we rely on javascript to zero file elements which are beyond last (partial) data-block
      //    but before this length encoding!
      dataWA[((lenInBits + 64 >> 9) << 4) + 15] = lenInBits;

      // 2. 512bit blocks (actual split done ondemand later)
      var W = Array(80);

      // 3. initial hash using SHA-1 constants on page 13
      var H0 =  1732584193;
      var H1 = -271733879;
      var H2 = -1732584194;
      var H3 =  271733878;
      var H4 = -1009589776;

      // 6.1.2 SHA-1 Hash Computation
      for(var i = 0; i < dataWA.length; i += 16) {
        // 1. Message schedule, done below
        // 2. init working variables
        var a = H0; var b = H1; var c = H2; var d = H3; var e = H4;

        // 3. round-functions
        for(var j = 0; j < 80; j++)
        {
      		 // postponed step 2
          W[j] = ( (j < 16) ? dataWA[i+j] : rol(W[j-3] ^ W[j-8] ^ W[j-14] ^ W[j-16], 1));

          var T = add_wa( add_wa( rol(a, 5), sha1_ft(j, b, c, d)),
                          add_wa( add_wa(e, W[j]), sha1_kt(j)) );
          e = d;
          d = c;
          c = rol(b, 30);
          b = a;
          a = T;
        }

		 // 4. intermediate hash
        H0 = add_wa(a, H0);
        H1 = add_wa(b, H1);
        H2 = add_wa(c, H2);
        H3 = add_wa(d, H3);
        H4 = add_wa(e, H4);
      }

      return Array(H0, H1, H2, H3, H4);
    }
  }()

};
