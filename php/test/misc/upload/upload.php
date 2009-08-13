<?php

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
