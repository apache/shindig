<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

/*
 * This class impliments memcached based caching It'll generally be more
 * usefull in a multi-server envirionment then the file based caching,
 * (in a single server setup file based caching is actually faster)
 */
class CacheMemcache extends Cache {
	private $connection = false;

	public function __construct()
	{
		if (! function_exists('memcache_connect')) {
			throw new CacheException("Memcache functions not available");
		}
		if (Config::get('cache_host') == '' || Config::get('cache_port') == '') {
			throw new CacheException("You need to configure a cache server host and port to use the memcache backend");
		}
		$this->host = Config::get('cache_host');
		$this->port = Config::get('cache_port');
	}

	private function isLocked($key)
	{
		$this->check();
		if ((@memcache_get($this->connection, $key . '.lock')) === false) {
			return false;
		}
		return true;
	}

	private function createLock($key)
	{
		$this->check();
		// the interesting thing is that this could fail if the lock was created in the meantime..
		// but we'll ignore that out of convenience
		@memcache_add($this->connection, $key . '.lock', '', 0, 5);
	}

	private function removeLock($key)
	{
		$this->check();
		// suppress all warnings, if some other process removed it that's ok too
		@memcache_delete($key . '.lock');
	}

	private function waitForLock($key)
	{
		$this->check();
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

	// I prefer lazy initalization since the cache isn't used every request
	// so this potentially saves a lot of overhead
	private function connect()
	{
		if (! $this->connection = @memcache_pconnect($this->host, $this->port)) {
			throw new CacheException("Couldn't connect to memcache server");
		}
	}

	private function check()
	{
		if (! $this->connection) {
			$this->connect();
		}
	}

	public function get($key, $expiration = false)
	{
		$this->check();
		if (! $expiration) {
			// default to global cache time
			$expiration = Config::Get('cache_time');
		}
		if (($ret = @memcache_get($this->connection, $key)) === false) {
			return false;
		}
		if (time() - $ret['time'] > $expiration) {
			$this->delete($key);
			return false;
		}
		return $ret['data'];
	}

	public function set($key, $value)
	{
		$this->check();
		// we store it with the cache_time default expiration so objects will atleast get cleaned eventually.
		if (@memcache_set($this->connection, $key, array('time' => time(), 'data' => $value), false, Config::Get('cache_time')) == false) {
			throw new CacheException("Couldn't store data in cache");
		}
	}

	public function delete($key)
	{
		$this->check();
		@memcache_delete($this->connection, $key);
	}
}
