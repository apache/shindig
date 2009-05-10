<?php

require_once '../../../src/gadgets/oauth/OAuth.php';

$data = json_decode($_POST['data']);

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

