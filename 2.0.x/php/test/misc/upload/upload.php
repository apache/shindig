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
 * Post the file to the specified REST api endpoint. It's used to test the REST 
 * api for content upload.
 */
function curlRest($url, $fileName, $contentType) {
  $fp = fopen($fileName, 'r');
  $fileSize = filesize($fileName);

  $ch = curl_init();
  curl_setopt($ch, CURLOPT_URL, $url);
  curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-Type: $contentType", "Expect:"));
  curl_setopt($ch, CURLOPT_HEADER, 0);
  curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
  curl_setopt($ch, CURLOPT_UPLOAD, 1);

  curl_setopt($ch, CURLOPT_INFILESIZE, $fileSize);
  curl_setopt($ch, CURLOPT_INFILE, $fp);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  $ret = curl_exec($ch);
  curl_close($ch);
  return $ret;
}

// The title is 'opensocial' and the description is 'icon'.
$url = "http://shindig/social/rest/mediaitems/@me/@self/1?st=1:1:1:partuza:test.com:1:0&mediaType=IMAGE&title=opensocial&description=icon";
// Create a media item with a image file uploaded. 
$ret = curlRest($url, "icon.jpg", "image/jpg");

var_dump($ret);
