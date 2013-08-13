<?php
namespace apache\shindig\gadgets\servlet;
use apache\shindig\common\sample\BasicBlobCrypter;
use apache\shindig\gadgets\oauth\OAuthCallbackState;
use apache\shindig\common\HttpServlet;

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

class OAuthCallbackServlet extends HttpServlet {
  public function doGet() {
    $state = isset($_GET["state"]) ? $_GET["state"] : "";
    $token = isset($_GET["oauth_token"]) ? $_GET["oauth_token"] : "";
    $verifier = isset($_GET["oauth_verifier"]) ? $_GET["oauth_verifier"] : "";
    $code = isset($_GET["code"]) ? $_GET["code"] : "";
    if (strlen($state) > 0) {
      $BBC = new BasicBlobCrypter();
      $crypter = new BasicBlobCrypter(srand($BBC->MASTER_KEY_MIN_LEN));
      $clientState = new OAuthCallbackState($crypter, $state);
      $url = $clientState->getRealCallbackUrl();
      $callbackUrl = "http://" . $_SERVER['HTTP_HOST'] . "/gadgets/oauthcallback";
      if ($url = $callbackUrl) {
        unset($_GET['state']);
        header('Location: '.$callbackUrl.'?'.http_build_query($_GET));
        exit;
      }
    } else if ((strlen($token) > 0  || strlen($code) > 0) && strlen($state) == 0 ) {
      $this->setCacheTime(3600);
      echo "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " .
      "\"http://www.w3.org/TR/html4/loose.dtd\">" .
      "<html>" .
      "<head>" .
      "<title>Close this window</title>" .
      "</head>" .
      "<body>" .
      "<script type=\"text/javascript\">" .
      "try {" .
      "  window.opener.gadgets.io.oauthReceivedCallbackUrl_ = document.location.href;" .
      "} catch (e) {" .
      "}" .
      "window.close();" .
      "</script>" .
      "Close this window." .
      "</body>" .
      "</html>";
      exit;
    }
    header("HTTP/1.0 400 Bad Request", true);
    echo "<html><body><h1>" . "400 - Bad Request Error" . "</h1></body></html>";
    die();
  }
}
