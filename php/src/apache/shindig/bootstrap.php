<?php
namespace apache\shindig;

use apache\shindig\common\Config;

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

// Some people forget to set their timezone in their php.ini,
// this prevents that from generating warnings
@date_default_timezone_set(@date_default_timezone_get());

// generally disable loading of external entities in xml files to prevent
// remote file inclusions, see http://websec.io/2012/08/27/Preventing-XEE-in-PHP.html
libxml_disable_entity_loader(true);

require_once __DIR__ . '/../../../external/Symfony/Component/ClassLoader/UniversalClassLoader.php';

$loader = new \Symfony\Component\ClassLoader\UniversalClassLoader();
$loader->registerNamespaces(array(
  'Symfony' => __DIR__ . '/../../../external',
  'apache\shindig' => __DIR__ . '/../../'

));
$loader->registerPrefixes(array(
  'Zend_' => __DIR__ . '/../../../external',
));
$loader->register();


$mapperLoader = new \Symfony\Component\ClassLoader\MapClassLoader(array(
  'JsMin' => __DIR__ . '/../../../external/jsmin-php/jsmin.php',
  'OAuthRequest' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthSignatureMethod_RSA_SHA1' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthSignatureMethod_PLAINTEXT' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthSignatureMethod_HMAC_SHA1' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthSignatureMethodRSA_SHA1' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthUtil' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthToken' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthConsumer' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthDataStore' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthException' => __DIR__ . '/../../../external/OAuth/OAuth.php',
  'OAuthServer' => __DIR__ . '/../../../external/OAuth/OAuth.php',
));

$mapperLoader->register();

//$extensionClassPaths = \apache\shindig\common\Config::get('extension_class_paths');
//
//if (! is_array($extensionClassPaths)) {
//    $extensionClassPaths = array($extensionClassPaths);
//}
//
//$loader->registerPrefixFallbacks($extensionClassPaths);