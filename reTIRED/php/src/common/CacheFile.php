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
 * This class impliments a basic on disk caching. That will work fine on a single host
 * but on a multi server setup this could lead to some problems due to inconsistencies
 * between the various cached versions on the different servers. Other methods like
 * memcached should be used instead really.
 * 
 * When using this file based backend, its adviced to make a cron job that scans thru the
 * cache dir, and removes all files that are older then 24 hours (or whatever your
 * config's CACHE_TIME is set too).
 */
class CacheFile extends Cache {

	function isLocked($cacheFile)
	{
		// our lock file convention is simple: /the/file/path.lock
		return file_exists($cacheFile . '.lock');
	}

	private function createLock($cacheFile)
	{
		$cacheDir = dirname($cacheFile);
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

	private function removeLock($cacheFile)
	{
		// suppress all warnings, if some other process removed it that's ok too
		@unlink($cacheFile . '.lock');
	}

	private function waitForLock($cacheFile)
	{
		// 20 x 250 = 5 seconds
		$tries = 20;
		$cnt = 0;
		do {
			// make sure PHP picks up on file changes. This is an expensive action but really can't be avoided
			clearstatcache();
			// 250 ms is a long time to sleep, but it does stop the server from burning all resources on polling locks..
			usleep(250);
			$cnt ++;
		} while ($cnt <= $tries && $this->isLocked($cacheFile));
		if ($this->isLocked($cacheFile)) {
			// 5 seconds passed, assume the owning process died off and remove it
			$this->removeLock($cacheFile);
		}
	}

	private function getCacheDir($hash)
	{
		// use the first 2 characters of the hash as a directory prefix
		// this should prevent slowdowns due to huge directory listings
		// and thus give some basic amount of scalability
		return Config::get('cache_root') . '/' . substr($hash, 0, 2);
	}

	private function getCacheFile($hash)
	{
		return $this->getCacheDir($hash) . '/' . $hash;
	}

	public function get($key, $expiration = false)
	{
		if (! $expiration) {
			// if no expiration time was given, fall back on the global config
			$expiration = Config::get('cache_time');
		}
		$cacheFile = $this->getCacheFile($key);
		// See if this cache file is locked, if so we wait upto 5 seconds for the lock owning process to
		// complete it's work. If the lock is not released within that time frame, it's cleaned up.
		// This should give us a fair amount of 'Cache Stampeding' protection
		if ($this->isLocked($cacheFile)) {
			$this->waitForLock($cacheFile);
		}
		if (file_exists($cacheFile) && is_readable($cacheFile)) {
			$now = time();
			if (($mtime = filemtime($cacheFile)) !== false && ($now - $mtime) < Config::get('cache_time')) {
				if (($data = @file_get_contents($cacheFile)) !== false) {
					$data = unserialize($data);
					return $data;
				}
			}
		}
		return false;
	}

	public function set($key, $value)
	{
		$cacheDir = $this->getCacheDir($key);
		$cacheFile = $this->getCacheFile($key);
		if ($this->isLocked($cacheFile)) {
			// some other process is writing to this file too, wait until it's done to prevent hickups
			$this->waitForLock($cacheFile);
		}
		if (! is_dir($cacheDir)) {
			if (! @mkdir($cacheDir, 0755, true)) {
				throw new CacheException("Could not create cache directory");
			}
		}
		// we serialize the whole request object, since we don't only want the
		// responseContent but also the postBody used, headers, size, etc
		$data = serialize($value);
		$this->createLock($cacheFile);
		if (! @file_put_contents($cacheFile, $data)) {
			$this->removeLock($cacheFile);
			throw new CacheException("Could not store data in cache file");
		}
		$this->removeLock($cacheFile);
	}

	public function delete($key)
	{
		$file = $this->getCacheFile($key);
		if (! @unlink($file)) {
			throw new CacheException("Cache file could not be deleted");
		}
	}
}