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

/*
 * This class impliments APC based caching. While this doesn't share
 * the cache through multiple servers, having the cache local is quite
 * efficient (and faster then file based caching in a high-io env)
 */
class CacheApc extends Cache {

  public function __construct() {
    if (! function_exists('apc_add')) {
      throw new CacheException("Apc functions not available");
    }
  }

  private function isLocked($key) {
    if ((@apc_fetch($key . '.lock')) === false) {
      return false;
    }
    return true;
  }

  private function createLock($key) {
    // the interesting thing is that this could fail if the lock was created in the meantime..
    // but we'll ignore that out of convenience
    @apc_add($key . '.lock', '', 5);
  }

  private function removeLock($key) {
    // suppress all warnings, if some other process removed it that's ok too
    @apc_delete($key . '.lock');
  }

  private function waitForLock($key) {
    // 10 x 100ms = 1 second
    $tries = 10;
    $cnt = 0;
    do {
      usleep(100);
      $cnt ++;
    } while ($cnt <= $tries && $this->isLocked());
    if ($this->isLocked()) {
      $this->removeLock($key);
    }
  }

  public function get($key) {
    if (($ret = @apc_fetch($key)) === false) {
      return false;
    }
    return $ret;
  }

  public function set($key, $value, $ttl = false) {
    if (! $ttl) {
      $ttl = Config::Get('cache_time');
    }
    if ($this->isLocked($key)) {
      $this->waitForLock($key);
    }
    $this->createLock($key);
    if (@apc_store($key, $value, $ttl) == false) {
      $this->removeLock($key);
      throw new CacheException("Couldn't store data in cache");
    }
    $this->removeLock($key);
  }

  public function delete($key) {
    @apc_delete($key);
  }
}
