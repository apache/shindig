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

// Some people forget to set their timezone in their php.ini,
// this prevents that from generating warnings
@date_default_timezone_set(@date_default_timezone_get());

include_once ('src/common/Config.php');
include_once ('src/common/File.php');

if (Config::get('debug')) {
  // Basic sanity check if we have all required modules
  $modules = array('json', 'SimpleXML', 'libxml', 'curl', 'openssl');
  // if plain text tokens are disallowed we require mcrypt
  if (! Config::get('allow_plaintext_token')) {
    $modules[] = 'mcrypt';
  }
  // if you selected the memcache caching backend, you need the memcache extention too :)
  if (Config::get('data_cache') == 'CacheMemcache') {
    $modules[] = 'memcache';
  }
  foreach ($modules as $module) {
    if (! extension_loaded($module)) {
      die("Shindig requires the {$module} extention, see <a href='http://www.php.net/{$module}'>http://www.php.net/{$module}</a> for more info");
    }
  }
}

// All configurable classes are autoloaded (see config.php for the configurable classes)
// To load these, we scan our entire directory structure
function __autoload($className) {
  $locations = array(
    'src/common',
    'src/common/sample',
    'src/gadgets',
    'src/gadgets/servlet', 
    'src/gadgets/oauth',
    'src/gadgets/sample',
    'src/social',
    'src/social/servlet', 
    'src/social/service',
    'src/social/opensocial',
    'src/social/model',
    'src/social/spi', 
    'src/social/converters',
    'src/social/oauth',
    'src/social/sample'
  );
  $extension_class_paths = Config::get('extension_class_paths');
  if (! empty($extension_class_paths)) {
    $locations = array_merge(explode(',', $extension_class_paths), $locations);
  }
  // Check for the presense of this class in our all our directories.
  $fileName = $className . '.php';
  foreach ($locations as $path) {
    if (file_exists("{$path}/$fileName")) {
      require $path.'/'.$fileName;
      break;
    }
  }
}

$servletMap = array(
    Config::get('web_prefix') . '/gadgets/files' => 'FilesServlet', 
    Config::get('web_prefix') . '/gadgets/js' => 'JsServlet', 
    Config::get('web_prefix') . '/gadgets/proxy' => 'ProxyServlet', 
    Config::get('web_prefix') . '/gadgets/makeRequest' => 'ProxyServlet', 
    Config::get('web_prefix') . '/gadgets/ifr' => 'GadgetRenderingServlet', 
    Config::get('web_prefix') . '/gadgets/metadata' => 'MetadataServlet', 
    Config::get('web_prefix') . '/social/rest' => 'DataServiceServlet', 
    Config::get('web_prefix') . '/social/rpc' => 'JsonRpcServlet', 
    Config::get('web_prefix') . '/public.crt' => 'CertServlet', 
    Config::get('web_prefix') . '/public.cer' => 'CertServlet'
);

// Try to match the request url to our servlet mapping
$servlet = false;
$uri = $_SERVER["REQUEST_URI"];
foreach ($servletMap as $url => $class) {
  if (substr($uri, 0, strlen($url)) == $url) {
    //FIXME temporary hack to support both /proxy and /makeRequest with the same event handler
    // /makeRequest == /proxy?output=js
    if ($url == Config::get('web_prefix') . '/gadgets/makeRequest') {
      $_GET['output'] = 'js';
    }
    $servlet = $class;
    break;
  }
}

// If we found a correlating servlet, instance and call it. Otherwise give a 404 error
if ($servlet) {
  $class = new $class();
  $method = $_SERVER['REQUEST_METHOD'];
  // Not all clients support the PUT, HEAD & DELETE http methods, they depend on the X-HTTP-Method-Override instead 
  if ($method == 'POST' && isset($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'])) {
    $method = $_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'];
  }
  $method = 'do' . ucfirst(strtolower($method));
  if (is_callable(array($class, $method))) {
    $class->$method();
  } else {
    header("HTTP/1.0 405 Method Not Allowed");
    echo "<html><body><h1>405 Method Not Allowed</h1></body></html>";
  }
} else {
  // Unhandled event, display simple 404 error
  header("HTTP/1.0 404 Not Found");
  echo "<html><body><h1>404 Not Found</h1></body></html>";
}
