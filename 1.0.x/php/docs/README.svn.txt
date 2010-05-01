                          Apache Shindig PHP

  Running PHP Shindig from SVN
  -----------

  This file is intended to be a supplement to the general README file, the release version
  has a slightly different file path configuration 
  
  Differences between the release version and a svn checkout
  -----------
  
  Shindig's svn repository contains both the Java and PHP versions of shindig, and the shared
  javascript and features code.
   
  To make PHP Shindig work from svn, its default file path configurations in 
      <shindig>/php/config/container.php
  are all configured for a file path layout where the features and javascript code is contained
  in a directory level above the php folder (ie <shindig>/php/../{features, javascript}), resulting in a 
  folder layout like:
  
  shindig/             (contains the shared README, NOTICE, LICENSE, etc files)
  shindig/javascript   (contains shared javascript code)
  shindig/features     (contains shared features code)
  shindig/config       (contains the shared configuration)
  shindig/java         (contains the java-shindig implementation)
  shindig/php          (contains the php-shindig implementation)
  
  The release script moves these folders to the top level php folder and makes the php folder the top
  level folder when building it's archives, so in other words the javascript and features code will be
  located in <shindig>/{features, javascript}, resulting in the folowing layout:

  shindig/             (contains the php implementation(!) & the php specific README, NOTICE, LICENSE, etc files)
  shindig/javascript   (contains shared javascript code)
  shindig/features     (contains shared features code)
  shindig/config       (contains both the shared as wel as php specific configuration)
  .. etc ..
  
  Switching from release to svn, and back
  -----------
  
  There are 2 configurations that need to be updated to switch from release to a svn version:
  
  1) Apache's virtual host configuration:
  
  The DirectoryRoot for the release version is <shindig>/, while the DirectoryRoot for the svn
  version is <shindig>/php, ie:
  
  RELEASE
  
  <VirtualHost your_ip:your_port>
    ServerName your.host
    DocumentRoot /var/www/html/shindig
    ... other normal settings in vhosts...
  <Directory>
    AllowOverride All
  </Directory>
  </VirtualHost>
  
  SVN
  
  <VirtualHost your_ip:your_port>
    ServerName your.host
    DocumentRoot /var/www/html/shindig/php
    ... other normal settings in vhosts...
  <Directory>
    AllowOverride All
  </Directory>
  </VirtualHost>
   
   2) PHP Shindig's configuration
   
   The file paths of all the shared resources are different between the released and svn versions in the config/container.php config file
   (notice the extra ../ for the javascript, features and jsondb path's with the SVN version)
   
   RELEASE
   
  'base_path' => realpath(dirname(__FILE__) . '/..') . '/',
  'features_path' => realpath(dirname(__FILE__) . '/../features/src/main/javascript/features') . '/',
  'container_path' => realpath(dirname(__FILE__) . '/../config') . '/',
  'javascript_path' => realpath(dirname(__FILE__) . '/../javascript') . '/',
  'private_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/private.key',
  'public_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/public.crt',
  'private_key_phrase' => 'SOMEKEY',
  'jsondb_path' => realpath(dirname(__FILE__) . '/../javascript/sampledata') . '/canonicaldb.json',
   
   SVN
   
  'base_path' => realpath(dirname(__FILE__) . '/..') . '/',
  'features_path' => realpath(dirname(__FILE__) . '/../../features/src/main/javascript/features') . '/',
  'container_path' => realpath(dirname(__FILE__) . '/../../config') . '/',
  'javascript_path' => realpath(dirname(__FILE__) . '/../../javascript') . '/',
  'private_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/private.key',
  'public_key_file' => realpath(dirname(__FILE__) . '/../certs') . '/public.crt',
  'private_key_phrase' => 'SOMEKEY',
  'jsondb_path' => realpath(dirname(__FILE__) . '/../../javascript/sampledata') . '/canonicaldb.json',
  
  
