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
require_once '../../src/gadgets/oauth/OAuth.php';

$data = json_decode(stripslashes(stripslashes($_POST['data'])));

switch ($data->type) {
  case 'activity':
    $consumer = new OAuthConsumer($data->key, $data->secret);
    $signature_method = new OAuthSignatureMethod_HMAC_SHA1();
    $params = array();
    $params['oauth_consumer_key'] = $data->key;
    $params['startIndex'] = $data->first;
    $params['count'] = $data->max;
    $http_query = http_build_query($params);
    $oauth_request = OAuthRequest::from_consumer_and_token($consumer, null, 'GET', $data->url, $params);
    $oauth_request->sign_request($signature_method, $consumer, null);

    $result = $oauth_request->to_url();
    header('ContentType: application/json');
    echo '{"url" : "' . $result . '"}';
  break;
  case 'invalidation':
    $consumer = new OAuthConsumer($data->key, $data->secret);
    $signature_method = new OAuthSignatureMethod_HMAC_SHA1();
    $params = array();
    $params['oauth_body_hash'] = base64_encode(sha1(stripslashes($data->postdata), true));
    $params['oauth_consumer_key'] = $data->key;
    $oauth_request = OAuthRequest::from_consumer_and_token($consumer, null, 'POST', $data->url, $params);
    $oauth_request->sign_request($signature_method, $consumer, null);

    $result = $oauth_request->to_url();
    header('ContentType: application/json');
    echo '{"url" : "' . $result . '"}';
  break;
}
