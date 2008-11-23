<?php
/**
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
 * Crypto test case.
 */
class CryptoTest extends PHPUnit_Framework_TestCase {

  /**
   * Tests Crypto::aes128cbcEncrypt()
   */
  public function testAes128() {
    $string = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit';
    $key = 'Aliquam erat volutpat';
    $encrypted = Crypto::aes128cbcEncrypt($key, $string);
    $decrypted = Crypto::aes128cbcDecrypt($key, $encrypted);
    $this->assertEquals($decrypted, $string);
  }

  /**
   * Tests Crypto::hmacSha1()
   */
  public function testHmacSha1() {
    $string = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit';
    $key = 'Aliquam erat volutpat';
    $expected = '%16%E7%E0E%22%08%5C%2B48%85d%FE%DE%C7%3A%C3%0D%11c';
    $hmac = urlencode(Crypto::hmacSha1($key, $string));
    $this->assertEquals($expected, $hmac);
  }

  /**
   * Tests Crypto::hmacSha1Verify()
   */
  public function testHmacSha1VerifyException() {
    $string = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit';
    $key = 'Aliquam erat volutpat';
    $expected = 'foo';
    $this->setExpectedException('GeneralSecurityException');
    Crypto::hmacSha1Verify($key, $string, $expected);
  }

  /**
   * Tests Crypto::hmacSha1Verify()
   */
  public function testHmacSha1Verify() {
    $string = 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit';
    $key = 'Aliquam erat volutpat';
    $expected = '%16%E7%E0E%22%08%5C%2B48%85d%FE%DE%C7%3A%C3%0D%11c';
    try {
      Crypto::hmacSha1Verify($key, $string, urldecode($expected));
      $success = true;
    } catch (GeneralSecurityException $e) {
      $success = false;
    }
    $this->assertTrue($success);
  }

}

