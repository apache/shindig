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
class FrontController {

    private $localConfig;

    public function setLocalConfig($localConfig) {
        $this->localConfig = $localConfig;
    }

    public function run() {
        Config::loadConfig($this->localConfig);

        $this->checkServerConfig();

        //get servlet map and prefix the servlet paths
        $configServletMap = Config::get('servlet_map');
        $webPrefix = Config::get('web_prefix');
        $servletMap = array();
        foreach ($configServletMap as $path => $servlet) {
            $servletMap[$webPrefix . $path] = $servlet;
        }

        // Try to match the request url to our servlet mapping
        $servlet = false;
        $uri = $_SERVER["REQUEST_URI"];
        foreach ($servletMap as $url => $class) {
            if (substr($uri, 0, strlen($url)) == $url) {
                //FIXME temporary hack to support both /proxy and /makeRequest with the same event handler
                // /makeRequest == /proxy?output=js
                if ($url == $webPrefix . '/gadgets/makeRequest') {
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
    }

    private function checkServerConfig() {
        if (!Config::get('debug')) {
            // Basic sanity check if we have all required modules
            $modules = array('json', 'SimpleXML', 'libxml', 'curl', 'openssl');
            // if plain text tokens are disallowed we require mcrypt
            if (!Config::get('allow_plaintext_token')) {
                $modules[] = 'mcrypt';
            }
            // if you selected the memcache caching backend, you need the memcache extention too :)
            if (Config::get('data_cache') == 'CacheMemcache') {
                $modules[] = 'memcache';
            }
            foreach ($modules as $module) {
                if (!extension_loaded($module)) {
                    die("Shindig requires the {$module} extention, see <a href='http://www.php.net/{$module}'>http://www.php.net/{$module}</a> for more info");
                }
            }

            if (get_magic_quotes_gpc()) {
                die("Your environment has magic_quotes_gpc enabled which will interfere with Shindig.  Please set 'magic_quotes_gpc' to 'Off' in php.ini");
            }

            $populate_raw_post = strtolower(ini_get("always_populate_raw_post_data"));
            if (!isset($populate_raw_post) || $populate_raw_post === "0" || $populate_raw_post === "Off") {
                die("Your environment does not have always_populate_raw_post_data enabled which will interfere with Shindig.  Please set 'always_populate_raw_post_data' to 'On' in php.ini");
            }
        }
    }

}