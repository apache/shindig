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
    // 20 x 250 = 5 seconds
    $tries = 20;
    $cnt = 0;
    do {
      // 250 ms is a long time to sleep, but it does stop the server from burning all resources on polling locks..
      usleep(250);
      $cnt ++;
    } while ($cnt <= $tries && $this->isLocked());
    if ($this->isLocked()) {
      // 5 seconds passed, assume the owning process died off and remove it
      $this->removeLock($key);
    }
  }

  public function get($key, $expiration = false) {
    if (! $expiration) {
      // default to global cache time
      $expiration = Config::Get('cache_time');
    }
    if (($ret = @apc_fetch($key)) === false) {
      return false;
    }
    if (time() - $ret['time'] > $expiration) {
      $this->delete($key);
      return false;
    }
    return unserialize($ret['data']);
  }

  public function set($key, $value) {
    // we store it with the cache_time default expiration so objects will atleast get cleaned eventually.
    if (@apc_store($key, array('time' => time(), 'data' => serialize($value)), Config::Get('cache_time')) == false) {
      throw new CacheException("Couldn't store data in cache");
    }
  }

  public function delete($key) {
    @apc_delete($key);
  }
}
