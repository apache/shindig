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
 * The default configuration settings
 *
 * Put any site specific configuration in a config/local.php file, this way
 * your configuration won't be lost when upgrading shindig. If your site don't
 * support any services just use empty string as the service name. i.e.
 *  'messages_service' => ''
 *
 * in local.php you only have to specificy the fields you want to overwrite
 * with other values, for example on a production system you would probably have:
 * $shindigConfig = array(
 * 	'debug' => false,
 * 	'allow_plaintext_token' => false,
 * 	'token_cipher_key' => 'MySecretKey',
 * 	'token_hmac_key' => 'MyOtherSecret',
 * 	'private_key_phrase' => 'MyCertificatePassword',
 * 	'person_service' => 'MyPeopleService',
 * 	'activity_service' => 'MyActivitiesService',
 * 	'app_data_service' => 'MyAppDataService',
 * 	'messages_service' => 'MyMessagesService',
 * 	'oauth_lookup_service' => 'MyOAuthLookupService'
 * 	'xrds_location' => 'http://www.mycontainer.com/xrds',
 * 	'check_file_exists' => false
 * );
 *
 */
$shindigConfig = array(
  // Show debug backtrace's. Disable this on a production site
  'debug' => true,
  // do real file_exist checks? Turning this off can be a big performance gain on prod servers but also risky & less verbose errors
  'check_file_exists' => true,

  // Allow plain text security tokens, this is only here to allow the sample files to work. Disable on a production site
  'allow_plaintext_token' => true,

  // Is a valid security token required to render a gadget? The token is required for doing signed preloads, but disallowing this
  // can also help prevent external parties using your rendering server (only for the paranoid :)
  'render_token_required' => false,

  // Normally we would only rewrite the gadget's html if it has the <Optional feature="content-rewrite"> set, however with this you can
  // force the content to always be rewritten
  'rewrite_by_default' => false,

  // Should we sanitize (remove scripts) from certain views? Right now this is useless, but once service sided templating and OSML is done
  // this could be useful to force (fast) html only gadgets on the profile and/or home view. Set this to false or to an array of view names like: array('profile', 'home')
  'sanitize_views' => false,

  // Compress the inlined javascript, saves upto 50% of the document size
  'compress_javascript' => true,

  // Default refresh interval for proxy/makeRequest's if none is specified in the query
  'default_refresh_interval' => 1209587,

  // The URL Prefix under which shindig lives ie if you have http://myhost.com/shindig/php set web_prefix to /shindig/php
  'web_prefix' => '',
  // If you changed the web prefix, add the prefix to these too
  'default_js_prefix' => '/gadgets/js/',
  'default_iframe_prefix' => '/gadgets/ifr?',

 'servlet_map' => array( 
   '/container' => 'ContentFilesServlet',
   '/samplecontainer' => 'ContentFilesServlet',
   '/gadgets/resources' => 'ResourcesFilesServlet',
   '/gadgets/js' => 'JsServlet', 
   '/gadgets/proxy' => 'ProxyServlet', 
   '/gadgets/makeRequest' => 'MakeRequestServlet', 
   '/gadgets/ifr' => 'GadgetRenderingServlet', 
   '/gadgets/metadata' => 'MetadataServlet', 
   '/gadgets/oauthcallback' => 'OAuthCallbackServlet', 
   '/gadgets/api/rpc' => 'JsonRpcServlet', 
   '/gadgets/api/rest' => 'DataServiceServlet', 
   '/social/rest' => 'DataServiceServlet',
   '/social/rpc' => 'CompatibilityJsonRpcServlet',
   '/rpc' => 'JsonRpcServlet', 
   '/public.crt' => 'CertServlet', 
   '/public.cer' => 'CertServlet', 
 ), 
 
  // The X-XRDS-Location value for your implementing container, see http://code.google.com/p/partuza/source/browse/trunk/Library/XRDS.php for an example
  'xrds_location' => '',

  // Allow anonymous (READ) access to the profile information? (aka REST and JSON-RPC interfaces)
  // setting this to false means you have to be authenticated through OAuth to read the data
  'allow_anonymous_token' => true,

  // The encryption keys for encrypting the security token, and the expiration of it. Make sure these match the keys used in your container/site
  'token_cipher_key' => 'INSECURE_DEFAULT_KEY',
  'token_hmac_key' => 'INSECURE_DEFAULT_KEY',
  'token_max_age' => 60 * 60,

  // Ability to customize the style thats injected into the gadget document. Don't forget to put the link/etc colors in shindig/config/container.js too!
  'gadget_css' => 'body,td,div,span,p{font-family:arial,sans-serif;} a {color:#0000cc;}a:visited {color:#551a8b;}a:active {color:#ff0000;}body{margin: 0px;padding: 0px;background-color:white;}',

  // P3P privacy policy to use for the iframe document
  'P3P' => 'CP="CAO PSA OUR"',

  // The locations of the various required components on disk. If you did a normal svn checkout there's no need to change these
  'base_path' => realpath(dirname(__FILE__) . '/..') . '/',
  'features_path' => array(
    realpath(dirname(__FILE__) . '/../../features/src/main/javascript/features') . '/',
    realpath(dirname(__FILE__) . '/../../extras/src/main/javascript/features-extras') . '/',
  ),
  'container_path' => realpath(dirname(__FILE__) . '/../../config') . '/',
  'javascript_path' => realpath(dirname(__FILE__) . '/../../content') . '/',
  'resources_path' => realpath(dirname(__FILE__) . '/../external/resources') . '/',

  // The OAuth SSL certificates to use, and the pass phrase for the private key
  'private_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/private.key',
  'public_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/public.crt',
  'private_key_phrase' => 'partuza',

  // the path to the json db file, used only if your using the JsonDbOpensocialService example/demo service
  'jsondb_path' => realpath(dirname(__FILE__) . '/../../content/sampledata') . '/canonicaldb.json',

  // Force these libraries to be external (included through <script src="..."> tags), this way they could be cached by the browser
  // these libraries will be included regardless of the features the gadget requests
  // example: 'dynamic-height:views' includes the features dynamic-height and views
  'forcedJsLibs' => '',

  // Force these js libraries to be appended to each gadget regardless if the gadget requested them or not
  // This can be useful to overwrite existing methods of other javascript packages
  'forcedAppendedJsLibs' => array(),

  // After checking the internal __autoload function, shindig can also call the 'extension_autoloader' function to load an
  // unknown custom class, this is particuarly useful for when intergrating shindig into an existing framework that also depends on autoloading
  'extension_autoloader' => false,

  // Configurable classes. Change these to the class name to use, and make sure the auto-loader can find them
  'blacklist_class' => 'BasicGadgetBlacklist',
  'remote_content' => 'BasicRemoteContent',
  'remote_content_fetcher' => 'BasicRemoteContentFetcher',
  'security_token_signer' => 'BasicSecurityTokenDecoder',
  'security_token' => 'BasicSecurityToken',
  'oauth_lookup_service' => 'BasicOAuthLookupService',
  // The OAuth Store is used to store the (gadgets/)oauth proxy credentials it obtained on behalf of the user/gadget combo
  'oauth_store' => 'BasicOAuthStore',

 
  // handler for ApiServlet
  'service_handler' => array(
    'people' => 'PersonHandler',
    'activities' => 'ActivityHandler',
    'appdata' => 'AppDataHandler',
    'groups' => 'GroupHandler',
    'messages' => 'MessagesHandler',
    'cache'  => 'InvalidateHandler',
    'system' => 'SystemHandler',
    'albums' => 'AlbumHandler',
    'mediaitems' => 'MediaItemHandler',
    'http' => 'HttpHandler',
  ),
 
  // class is the name of the concrete input converter class
  // targetField is the name of the field where the decoded array will be inserted
  // into the params array or null if you want to overwrite params with the decoded
  // array or false if you do not want to add the decoded params
  'service_input_converter' => array(
    'people' => array('class' => 'InputPeopleConverter', 'targetField' => false),
    'activities' => array('class' => 'InputActivitiesConverter', 'targetField' => 'activity'),
    'appdata' => array('class' => 'InputAppDataConverter', 'targetField' => 'data'),
    'messages' => array('class' => 'InputMessagesConverter', 'targetField' => 'entity'),
    'cache'  => array('class' => 'InputInvalidateConverter', 'targetField' => null),
    'albums' => array('class' => 'InputAlbumsConverter', 'targetField' => 'album'),
    'mediaitems' => array('class' => 'InputMediaItemsConverter', 'targetField' => 'mediaItem'),
  ),
 
 
  'gadget_class' => 'Gadget',
  'gadget_context_class' => 'GadgetContext',
  'gadget_factory_class' => 'GadgetFactory',
  'gadget_spec_parser' => 'GadgetSpecParser',
  'gadget_spec_class' => 'GadgetSpec',
  'substitution_class' => 'Substitutions',
  'proxy_handler' => 'ProxyHandler',
  'makerequest_handler' => 'MakeRequestHandler',
  'makerequest_class' => 'MakeRequest',
  'container_config_class' => 'ContainerConfig',

  // Caching back-end's to use. Shindig ships with CacheStorageFile, CacheStorageApc and CacheStorageMemcache support
  // The data cache is primarily used for remote content (proxied files, gadget spec, etc)
  // and the feature_cache is used to cache the parsed features xml structure and javascript
  // On a production system you probably want to use CacheStorageApc for features, and CacheStorageMemcache for the data cache
  'data_cache' => 'CacheStorageFile',
  'feature_cache' => 'CacheStorageFile',

  // RESTful API data service classes to use
  // See http://code.google.com/p/partuza/source/browse/#svn/trunk/Shindig for a MySql powered example
  'person_service' => 'JsonDbOpensocialService',
  'activity_service' => 'JsonDbOpensocialService',
  'app_data_service' => 'JsonDbOpensocialService',
  'group_service' => 'JsonDbOpensocialService',
  'messages_service' => 'JsonDbOpensocialService',
  'invalidate_service' => 'DefaultInvalidateService',
  'album_service' => 'JsonDbOpensocialService',
  'media_item_service' => 'JsonDbOpensocialService',

  // Also scan these directories when looking for <Class>.php files. You can include multiple paths by seperating them with a ,
  'extension_class_paths' => '',

  'userpref_param_prefix' => 'up_',
  'libs_param_name' => 'libs',

  // If you use CacheStorageMemcache as caching backend, change these to the memcache server settings
  'cache_host' => 'localhost',
  'cache_port' => 11211,
  // When using CacheStorageMemcache, should we use pconnect? There are some reports that apache/mpm + memcache_pconnect can lead to segfaults
  'cache_memcache_pconnect' => true,
  'cache_time' => 24 * 60 * 60,
  // If you use CacheStorageFile as caching backend, this is the directory where it stores the temporary files
  'cache_root' => '/tmp/shindig',

  // connection timeout setting for all curl requests, set this time something low if you want errors reported
  // quicker to the end user, and high (between 10 and 20) if your on a slow connection
  'curl_connection_timeout' => '10',
  'curl_request_timeout' => '10',

  // If your development server is behind a proxy, enter the proxy details here in 'proxy.host.com:port' format.
  'proxy' => '',

  // If your server is behind a reverse proxy, set the real hostname here so that OAuth signatures match up, for example:
  // 'http_host' => 'modules.partuza.nl'
  'http_host' => false,

  // Container id, used for security token
  'container_id' => 'default'
);
