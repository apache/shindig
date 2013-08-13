<?php
namespace apache\shindig\common\sample;
use apache\shindig\common\BlobCrypter;
use apache\shindig\common\Config;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This class provides basic binary blob encryption and decryption, for use with the security token
 *
 */
class BasicBlobCrypter extends BlobCrypter {
  //FIXME make this compatible with the java's blobcrypter


  // Labels for key derivation
  protected $CIPHER_KEY_LABEL = 0;
  protected $HMAC_KEY_LABEL = 1;

  /** Key used for time stamp (in seconds) of data */
  public $TIMESTAMP_KEY = "t";

  /** minimum length of master key */
  public $MASTER_KEY_MIN_LEN = 16;

  /** allow three minutes for clock skew */
  protected $CLOCK_SKEW_ALLOWANCE = 180;

  protected $UTF8 = "UTF-8";

  protected $cipherKey;
  protected $hmacKey;
  protected $allowPlaintextToken;

  public function __construct() {
    $this->cipherKey = Config::get('token_cipher_key');
    $this->hmacKey = Config::get('token_hmac_key');
    $this->allowPlaintextToken = Config::get('allow_plaintext_token');
  }

  /**
   * {@inheritDoc}
   */
  public function wrap(Array $in) {
    $encoded = $this->serializeAndTimestamp($in);
    if (! function_exists('mcrypt_module_open') && $this->allowPlaintextToken) {
      $cipherText = base64_encode($encoded);
    } else {
      $cipherText = Crypto::aes128cbcEncrypt($this->cipherKey, $encoded);
    }
    $hmac = Crypto::hmacSha1($this->hmacKey, $cipherText);
    $b64 = base64_encode($cipherText . $hmac);
    return $b64;
  }

  /**
   *
   * @param array $in
   * @return string
   */
  protected function serializeAndTimestamp(Array $in) {
    $encoded = "";
    foreach ($in as $key => $val) {
      $encoded .= urlencode($key) . "=" . urlencode($val) . "&";
    }
    $encoded .= $this->TIMESTAMP_KEY . "=" . time();
    return $encoded;
  }

  /**
   * {@inheritDoc}
   */
  public function unwrap($in, $maxAgeSec) {
    //TODO remove this once we have a better way to generate a fake token in the example files
    if ($this->allowPlaintextToken && count(explode(':', $in)) >= 7) {
      //Parses the security token in the form st=o:v:a:d:u:m:c
      $data = $this->parseToken($in);
      $out = array();
      $out['o'] = $data[0];
      $out['v'] = $data[1];
      $out['a'] = $data[2];
      $out['d'] = $data[3];
      $out['u'] = $data[4];
      $out['m'] = $data[5];
    } else {
      $bin = base64_decode($in);
      if (is_callable('mb_substr')) {
        $cipherText = mb_substr($bin, 0, - Crypto::$HMAC_SHA1_LEN, 'latin1');
        $hmac = mb_substr($bin, mb_strlen($cipherText, 'latin1'), Crypto::$HMAC_SHA1_LEN, 'latin1');
      } else {
        $cipherText = substr($bin, 0, - Crypto::$HMAC_SHA1_LEN);
        $hmac = substr($bin, strlen($cipherText));
      }
      Crypto::hmacSha1Verify($this->hmacKey, $cipherText, $hmac);
      if (! function_exists('mcrypt_module_open') && $this->allowPlaintextToken) {
        $plain = base64_decode($cipherText);
      } else {
        $plain = Crypto::aes128cbcDecrypt($this->cipherKey, $cipherText);
      }
      $out = $this->deserialize($plain);
      $this->checkTimestamp($out, $maxAgeSec);
    }
    return $out;
  }

  /**
   * Parses the security token
   * @param string $stringToken
   * @return array
   */
  protected function parseToken($stringToken) {
    $data = explode(":", $stringToken);
       $url_number = count($data)-6;

	//get array elements conrresponding to broken url - http://host:port/gadget.xml -> ["http","//host","port/gadget.xml"]
	$url_array = array_slice($data,4,$url_number) ;
	$url = implode(":",$url_array);
	array_splice($data,4,$url_number,$url);
    return $data;
  }

  /**
   * @param string $plain
   * @return array
   */
  protected function deserialize($plain) {
    $map = array();
    parse_str($plain, $map);
    return $map;
  }

  /**
   *
   * @param array $out
   * @param int $maxAge
   * @throws BlobExpiredException
   */
  protected function checkTimestamp(Array $out, $maxAge) {
    $minTime = (int)$out[$this->TIMESTAMP_KEY] - $this->CLOCK_SKEW_ALLOWANCE;
    $maxTime = (int)$out[$this->TIMESTAMP_KEY] + $maxAge + $this->CLOCK_SKEW_ALLOWANCE;
    $now = time();
    if (! ($minTime < $now && $now < $maxTime)) {
      throw new BlobExpiredException("Security token expired");
    }
  }
}
