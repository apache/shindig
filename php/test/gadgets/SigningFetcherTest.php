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

require_once 'src/gadgets/oauth/OAuth.php';

class MockSignatureMethod extends OAuthSignatureMethod_RSA_SHA1 {
  protected function fetch_public_cert(&$request) {
    return <<<EOD
-----BEGIN CERTIFICATE-----
MIICsDCCAhmgAwIBAgIJALlpyqPEjwvvMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV
BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX
aWRnaXRzIFB0eSBMdGQwHhcNMDkwNDAxMDgzMzQzWhcNMTIwMzMxMDgzMzQzWjBF
MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50
ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB
gQD8pipiScqep1T8e531ieuseKR1GPaVWmduMBXzrIhMYfD2x+hWy6ocGkcNxVIE
dopIo238YtSde/T3JiSE/Ho5uQ/os4mzVM+uZSkNyknZkzEmCkIg+kz6P91SMF5j
ioxdRcT0rg7d+DvsUd2Gt3UPdMf1GtcBGd8bxfjuNQQtyQIDAQABo4GnMIGkMB0G
A1UdDgQWBBQNTYnsqvzJ192fs03xJhjwlIVOQTB1BgNVHSMEbjBsgBQNTYnsqvzJ
192fs03xJhjwlIVOQaFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt
U3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJALlpyqPE
jwvvMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEA2HUzlfAvZ1ELSa1V
k1QBQQWEnXI7ST7jtsqflyErJW2SekMu0ReLAeVqYkVfeJG/7FZ18i7/LMOEV6uY
3k3kOKRcgbfa/k1j3siRbpNdyD3qzGxo3ggtE32P7l8IdWLkWcMvkAqfROXhay5W
nbpJMipy62GBW7yBbG+ypSasgI0=
-----END CERTIFICATE-----
EOD;
  }
}

/**
 * SigningFetcher test case.
 */
class SigningFetcherTest extends PHPUnit_Framework_TestCase {
  
  /**
   * @var SigningFetcher
   */
  private $signingFetcher;
  
  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $private_key = <<<EOD
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: DES-EDE3-CBC,2BB1348F45867303

9+e/kJCKUTnJLrNYY1iSjX+e6IVPo31dN20ab3O1BknT5c28PLjJbQkJz479VCX8
zJen/OyugesHXiQe5skPaG6+xwWGnztIxjHCLT5WtRE755UT3K83IeDde1zsK9xy
Iy8aRZbfBKCkgriIRNgD496gaVgEOGljEhCCIBLWERNZntcGmaBmN6CUdg75uuTI
HMX+2cA68yzRx31cU6EYdzB2vN93aLNuPI1u2ebFe7kuNYhW3d9Bc5MJh7iQdOfO
Yf94Xuic+2vIvwxi30Htz0wTBmTdEolDsSWzuyj7pjtUa0zZqaawCwLMYJFtz8lm
M2c5PXv8VvLBFIsTXWdy5+qDWMeROl1PaSDQ7HfAq8BtwNqV2yMKLE6cwHIWbYr/
lyIcBEhAZ8jfM81AWCgyAyeGSi4xGoCljxptExEwVzBJGjH93Ly6M7tjLBLmEQJM
nGmcY/3lmSMQIbxHV4ktXukPMrYYaTu5DW9jE+sNUHj+iUN/jJMTdOGh8zUtOQTs
qGuZBJbmjxdfSogCBL3f+JqOtRYUIIsZWEgb/AC10PC4pBit+9Cs9Z1LDMynFjKH
kGX/qgro2rPLiqR8o2dI/wCIa5sJhUT5vFC5N+Jn0jyhROK+eom4yEF0xX3DxSZY
iiclKgIOL/iB7FYEYFO17kUjFj8g53QWKh4tML/UG4GTIetNjD2u8wbobE7SxzZf
HHJXc4OblK/6GVpLn7yxZ5/EG7vtX/R4aPA70VFSkJYUd0xHWjUihss+9/TSIj/K
Cgpm3sdinamuC5b40tVhFhrfZyfUlqmssjU1nOsbnS+EqFgQJimbDg==
-----END RSA PRIVATE KEY-----
EOD;
    $rsa_private_key = @openssl_pkey_get_private($private_key, 'shindig');
    $basicFetcher = $this->getMock('RemoteContentFetcher');
    $this->signingFetcher = SigningFetcher::makeFromPrivateKey($basicFetcher, 'http://shindig/public.cer', $rsa_private_key);
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->Substitutions = null;
    parent::tearDown();
  }

  /**
   * Tests SigningFetcher->fetchRequest
   */
  public function testFetchRequest() {
    $request = new RemoteContentRequest('http://example.org/signed');
    $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $request->setToken(BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default'));
    $request->setPostBody('key=value&anotherkey=value');
    $this->signingFetcher->fetchRequest($request);
    $this->verifySignedRequest($request);
  }

  /**
   * Tests SigningFetcher->fetchRequest
   */
  public function testFetchRequestForBodyHash() {
    $request = new RemoteContentRequest('http://example.org/signed');
    $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $request->setToken(BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default'));
    $request->setPostBody('Hello World!');
    $request->setHeaders('Content-Type: text/plain');
    $this->signingFetcher->fetchRequest($request);
    $this->verifySignedRequest($request);
    $url = parse_url($request->getUrl());
    $query = array();
    parse_str($url['query'], $query);
    // test example 'Hello World!' and 'Lve95gjOVATpfV8EL5X4nxwjKHE=' are from
    // OAuth Request Body Hash 1.0 Draft 4 Example
    $this->assertEquals('Lve95gjOVATpfV8EL5X4nxwjKHE=', $query['oauth_body_hash']);
  }
  
  /**
   * Tests SigningFetcher->fetchRequest
   */
  public function testFetchRequestWithEmptyPath() {
    $request = new RemoteContentRequest('http://example.org');
    $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
    $request->setToken(BasicSecurityToken::createFromValues('owner', 'viewer', 'app', 'domain', 'appUrl', '1', 'default'));
    $request->setPostBody('key=value&anotherkey=value');
    $this->signingFetcher->fetchRequest($request);
    $this->verifySignedRequest($request);
  }
  
  private function verifySignedRequest(RemoteContentRequest $request) {
    $url = parse_url($request->getUrl());
    $query = array();
    parse_str($url['query'], $query);
    $post = array();
    $contentType = $request->getHeader('Content-Type');
    if ((stripos($contentType, 'application/x-www-form-urlencoded') !== false || $contentType == null)) {
      parse_str($request->getPostBody(), $post);
    } else {
      $this->assertEquals(base64_encode(sha1($request->getPostBody(), true)), $query['oauth_body_hash']);
    }
    $oauthRequest = OAuthRequest::from_request($request->getMethod(), $request->getUrl(), array_merge($query, $post));
    $signature_method = new MockSignatureMethod();
    $signature_valid = $signature_method->check_signature($oauthRequest, null, null, $query['oauth_signature']);
    $this->assertTrue($signature_valid);
  }
}

