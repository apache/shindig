<?php
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
?><?php
function errorPage($code, $message) {
    header("Status: $code");
    header("Content-Type: text/plain");
    echo $message;
}

error_log('----------------------------starting API call-----------------------------------');
$op = $_POST['op'];
if (!$op || $op == '') {
	//try from GET
	$op = $_GET['op'];
}

$appOrigin = $_POST['origin'];
if (preg_match('@[\W]@', $appOrigin)) {
    errorPage(400, 'Bogus origin param');
    return;
}

$curl = curl_init();

if ($op == 'proxy') {
	$url = $_GET['url'];
	curl_setopt($curl,CURLOPT_GET,true);
}
else if ($appOrigin){
	$url = 'http://' . $appOrigin . XN_AtomHelper::$DOMAIN_SUFFIX . '/gadgets/index/backendApi';
	curl_setopt($curl,CURLOPT_POST,true);
	curl_setopt($curl,CURLOPT_POSTFIELDS,$_POST);
}
else {
	error_log("Bogus API call: $appOrigin -> $op");
	return;
}

error_log('api: url=' . $url);

curl_setopt($curl,CURLOPT_USERAGENT,"Mozilla/4.0 (Compatible; Shindig Auth)");
curl_setopt($curl, CURLOPT_TIMEOUT, 30);
curl_setopt($curl, CURLOPT_MAXREDIRS, 5);
curl_setopt($curl,CURLOPT_URL,$url);
ob_start();
$result = curl_exec($curl);
if ($result == false) {
    $errno = curl_errno($curl);
    error_log("Error excuting api request $op $appOrigin $url : $errno");
    header("HTTP/1.1 500 Error");
    echo "Error executing request";
}
$data = ob_get_contents();
ob_end_clean();

$retcode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
curl_close($curl);
header('Content-Type: text/plain');
echo $data;
error_log('----------------------------END API call-----------------------------------');

?>
