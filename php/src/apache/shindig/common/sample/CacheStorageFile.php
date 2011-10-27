<?php
namespace apache\shindig\common\sample;
use apache\shindig\common\CacheException;
use apache\shindig\common\CacheStorage;
use apache\shindig\common\File;
use apache\shindig\common\Config;

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

class CacheStorageFile extends CacheStorage {
  private $prefix = null;

  /**
   * {@inheritDoc}
   */
  public function __construct($name) {
    $this->prefix = $name;
  }

  /**
   * {@inheritDoc}
   */
  public function store($key, $value) {
    $cacheDir = CacheStorageFile::getCacheDir($key);
    $cacheFile = CacheStorageFile::getCacheFile($key);
    if (! is_dir($cacheDir)) {
      if (! @mkdir($cacheDir, 0755, true)) {
        throw new CacheException("Could not create cache directory");
      }
    }

    return file_put_contents($cacheFile, $value);
  }

  /**
   * {@inheritDoc}
   */
  public function fetch($key) {
    $cacheFile = CacheStorageFile::getCacheFile($key);
    if (File::exists($cacheFile) && File::readable($cacheFile)) {
      return @file_get_contents($cacheFile);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public function delete($key) {
    $cacheFile = CacheStorageFile::getCacheFile($key);
    if (! @unlink($cacheFile)) {
      throw new CacheException("Cache file could not be deleted");
    }
  }

  /**
   * {@inheritDoc}
   */
  public function isLocked($key) {
    // our lock file convention is simple: /the/file/path.lock
    clearstatcache();
    return file_exists($key . '.lock');
  }

  /**
   * {@inheritDoc}
   */
  public function lock($key) {
    $cacheDir = CacheStorageFile::getCacheDir($key);
    $cacheFile = CacheStorageFile::getCacheFile($key);
    if (! is_dir($cacheDir)) {
      if (! @mkdir($cacheDir, 0755, true)) {
        // make sure the failure isn't because of a concurency issue
        if (! is_dir($cacheDir)) {
          throw new CacheException("Could not create cache directory");
        }
      }
    }
    @touch($cacheFile . '.lock');
  }

  /**
   * {@inheritDoc}
   */
  public function unlock($key) {
    // suppress all warnings, if some other process removed it that's ok too
    $cacheFile = CacheStorageFile::getCacheFile($key);
    @unlink($cacheFile . '.lock');
  }

  /**
   *
   * @param string $key
   * @return string
   */
  private function getCacheDir($key) {
    // use the first 2 characters of the hash as a directory prefix
    // this should prevent slowdowns due to huge directory listings
    // and thus give some basic amount of scalability
    return Config::get('cache_root') . '/' . $this->prefix . '/' .
        substr($key, 0, 2);
  }

  /**
   *
   * @param string $key
   * @return string
   */
  private function getCacheFile($key) {
    return $this->getCacheDir($key) . '/' . $key;
  }
}
