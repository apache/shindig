#!/usr/bin/php -Cq
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
 * Quick and dirty REST API test script that excersises the basic REST calls.
 */

/* Modify this if you want to test in a different social graph then partuza (user id 1 = me on in my local db).
 * the security token only works if ALLOW_PLAINTEXT_TOKEN is set to true in the php-shindig config
 * format of the plain text token is owner:viewer:appid:container:url:modid
 */
$securityToken = '1:1:1:partuza:test.com:0';
// The server to test against
$restUrl = 'http://shindig/social/rest';

function curlRest($url, $postData, $contentType, $method = 'POST') {
  global $securityToken, $restUrl;
  $ch = curl_init();
  if (substr($url, 0, 1) != '/') {
    $url = '/' . $url;
  }
  $sep = strpos($url, '?') !== false ? '&' : '?';
  curl_setopt($ch, CURLOPT_URL, $restUrl . $url . $sep . 'st=' . $securityToken);
  curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-Type: $contentType"));
  curl_setopt($ch, CURLOPT_HEADER, 0);
  curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
  curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  $ret = curl_exec($ch);
  curl_close($ch);
  return $ret;
}

/* -- NOT_IMPLEMENTED in partuza and the sample json db, so skipping this by default
// ************** Set App Data using XML post payload ********************** //
$postData = '<entry xmlns="http://www.w3.org/2005/Atom"
         xmlns:osapi="http://opensocial.org/2008/opensocialapi">
  <osapi:recipient>1</osapi:recipient>
  <osapi:recipient>2</osapi:recipient>
  <osapi:recipient>3</osapi:recipient>
  <title>You have an invitation from Joe</title>
  <id>{msgid}</id>
  <link rel="alternate" href="http://app.example.org/invites/{msgid}"/>
  <content>Click <a href="http://app.example.org/invites/{msgid}">here</a> to review your invitation.</content>
</entry>';
echo "Sending a message via POST.. ";
$ret = curlRest('/messages/1/outbox', $postData, 'application/atom+xml', 'POST');
if (! empty($ret)) {
	echo "FAILURE:\n[$ret]\n";
	die();
} else {
	echo "OK\n";
}

*/

// ************** Set App Data using XML post payload ********************** //
$postData = '<appdata xmlns="http://ns.opensocial.org/2008/opensocial">
  <entry>
    <key>pokes</key>
    <value>1</value>
  </entry>
  <entry>
    <key>last_poke</key>
    <value>2008-02-13T18:30:02Z</value>
  </entry>
</appdata>';
echo "Setting pokes and last_poke app data using XML.. ";
$ret = curlRest('/appdata/1/@self/1', $postData, 'application/xml');
if (! empty($ret)) {
  echo "FAILURE:\n[$ret]\n";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying XML set app data.. ";
  $ret = curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  if (isset($retDecoded['entry']) && isset($retDecoded['entry'][1]) && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes']) && $retDecoded['entry'][1]['last_poke'] == '2008-02-13T18:30:02Z' && $retDecoded['entry'][1]['pokes'] == '1') {
    echo "OK\n";
  } else {
    echo "FAILURE, unexpected return value: $ret\n";
    die();
  }
}

// ************** Set App Data using ATOM post payload ********************** //
$postData = '<entry xmlns="http://www.w3.org/2005/Atom">
  <content type="text/xml">
    <appdata xmlns="http://opensocial.org/2008/opensocial">  
        <pokes>2</pokes>
        <last_poke>2003-12-14T18:30:02Z</last_poke>
      </appdata>
  </content>
  <title/>
  <updated>2003-12-14T18:30:02Z</updated>
  <author><url>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</url></author>
  <id>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</id>
</entry>';
echo "Setting pokes and last_poke app data using Atom.. ";
$ret = curlRest('/appdata/1/@self/1', $postData, 'application/atom+xml');
if (! empty($ret)) {
  echo "FAILURE:\n$ret\n\n";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying Atom set app data.. ";
  $ret = curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  if (isset($retDecoded['entry']) && isset($retDecoded['entry'][1]) && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes']) && $retDecoded['entry'][1]['last_poke'] == '2003-12-14T18:30:02Z' && $retDecoded['entry'][1]['pokes'] == '2') {
    echo "OK\n";
  } else {
    echo "FAILURE, unexpected return value: $ret\n";
    die();
  }
}

// ************** Set App Data using JSON post payload ********************** //
$postData = '{
  "pokes" : 4,
  "last_poke" : "2008-06-13T18:30:02Z"
}';
echo "Setting pokes and last_poke app data using JSON.. ";
$ret = curlRest('/appdata/1/@self/1', $postData, 'application/json');
if (! empty($ret)) {
  echo "FAILURE:\n$ret\n\n";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying Atom set app data.. ";
  $ret = curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  if (isset($retDecoded['entry']) && isset($retDecoded['entry'][1]) && isset($retDecoded['entry'][1]['last_poke']) && isset($retDecoded['entry'][1]['pokes']) && $retDecoded['entry'][1]['last_poke'] == '2008-06-13T18:30:02Z' && $retDecoded['entry'][1]['pokes'] == '4') {
    echo "OK\n";
  } else {
    echo "FAILURE, unexpected return value: $ret\n";
    die();
  }
}

// ************** Delete app data ********************** //
echo "Deleting app data.. ";
$ret = curlRest('/appdata/1/@self/1?fields=pokes,last_poke', '', 'application/json', 'DELETE');
if (! empty($ret)) {
  echo "FAILURE:\n$ret\n";
  die();
} else {
  echo "OK\n";
}

// ************** Create Activity using JSON post payload ********************** //
echo "Creating activity using JSON.. ";
$randomTitle = "[" . rand(0, 2048) . "] test activity";
$postData = '{
  "id" : "http://example.org/activities/example.org:87ead8dead6beef/self/af3778",
  "title" : "' . $randomTitle . '",
  "updated" : "2008-02-20T23:35:37.266Z",
  "body" : "Some details for some activity",
  "bodyId" : "383777272",
  "url" : "http://api.example.org/activity/feeds/.../af3778",
  "userId" : "example.org:34KJDCSKJN2HHF0DW20394"
}';
$ret = curlRest('/activities/1/@self', $postData, 'application/json');
if (! empty($ret)) {
  echo "FAILURE:\n$ret";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying JSON created activity.. ";
  $ret = curlRest('/activities/1/@self?count=20', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  $found = false;
  // see if we can find our just created activity
  if (isset($retDecoded['entry'])) {
    foreach ($retDecoded['entry'] as $entry) {
      if ($entry['title'] == $randomTitle) {
        $found = true;
        $activityId = $entry['id'];
        break;
      }
    }
    echo "OK\n";
  }
  if (! $found) {
    echo "FAILURE, couldn't find activity, or unexpected return value: $ret\n";
    die();
  } else {
    echo "Deleting created activity..";
    $ret = curlRest("/activities/1/@self/@app/$activityId", '', 'application/json', 'DELETE');
    if (! empty($ret)) {
      die("FAILED\n");
    } else {
      echo "OK\n";
    }
  }
}

// ************** Create Activity using XML post payload ********************** //
echo "Creating activity using XML.. ";
$randomTitle = "[" . rand(0, 2048) . "] test activity";
$postData = '<activity xmlns="http://ns.opensocial.org/2008/opensocial">
  <id>http://example.org/activities/example.org:87ead8dead6beef/self/af3778</id>
  <title>' . $randomTitle . '</title>
  <updated>2008-02-20T23:35:37.266Z</updated>
  <body>Some details for some activity</body>
  <bodyId>383777272</bodyId>
  <url>http://api.example.org/activity/feeds/.../af3778</url>
  <userId>example.org:34KJDCSKJN2HHF0DW20394</userId>
</activity>';
$ret = curlRest('/activities/1/@self', $postData, 'application/xml');
if (! empty($ret)) {
  echo "FAILURE:\n$ret";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying XML created activity.. ";
  $ret = curlRest('/activities/1/@self?count=4', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  $found = false;
  // see if we can find our just created activity
  if (isset($retDecoded['entry'])) {
    foreach ($retDecoded['entry'] as $entry) {
      if ($entry['title'] == $randomTitle) {
        $found = true;
        $activityId = $entry['id'];
        break;
      }
    }
    echo "OK\n";
  }
  if (! $found) {
    echo "FAILURE, couldn't find activity, or unexpected return value: $ret\n";
    die();
  } else {
    echo "Deleting created activity..";
    $ret = curlRest("/activities/1/@self/@app/$activityId", '', 'application/json', 'DELETE');
    if (! empty($ret)) {
      die("FAILED\n");
    } else {
      echo "OK\n";
    }
  }
}

// ************** Create Activity using Atom post payload ********************** //
echo "Creating activity using Atom.. ";
$randomTitle = "[" . rand(0, 2048) . "] test activity";
$postData = '<entry xmlns="http://www.w3.org/2005/Atom">
  <category term="status"/>
  <id>http://example.org/activities/example.org:87ead8dead6beef/self/af3778</id>
  <title>' . $randomTitle . '</title>
  <summary>Some details for some activity</summary>
  <updated>2008-02-20T23:35:37.266Z</updated>
  <link rel="self" type="application/atom+xml" href="http://api.example.org/activity/feeds/.../af3778"/>
  <author><uri>urn:guid:example.org:34KJDCSKJN2HHF0DW20394</uri></author>
  <content>
    <activity xmlns="http://ns.opensocial.org/2008/opensocial">
      <bodyId>383777272</bodyId>
    </activity>
  </content>
</entry>';
$ret = curlRest('/activities/1/@self', $postData, 'application/atom+xml');
if (! empty($ret)) {
  echo "FAILURE:\n$ret";
  die();
} else {
  echo "OK\n";
  // verify data was written correctly
  echo "Verifying Atom created activity.. ";
  $ret = curlRest('/activities/1/@self?count=4', '', 'application/json', 'GET');
  $retDecoded = json_decode($ret, true);
  if ($ret == $retDecoded) {
    die("Invalid json string in return: $ret\n");
  }
  $found = false;
  // see if we can find our just created activity
  if (isset($retDecoded['entry'])) {
    foreach ($retDecoded['entry'] as $entry) {
      if ($entry['title'] == $randomTitle) {
        $found = true;
        $activityId = $entry['id'];
        break;
      }
    }
    echo "OK\n";
  }
  if (! $found) {
    echo "FAILURE, couldn't find activity, or unexpected return value: $ret\n";
    die();
  } else {
    echo "Deleting created activity..";
    $ret = curlRest("/activities/1/@self/@app/$activityId", '', 'application/json', 'DELETE');
    if (! empty($ret)) {
      die("FAILED: $ret\n");
    } else {
      echo "OK\n";
    }
  }
}
