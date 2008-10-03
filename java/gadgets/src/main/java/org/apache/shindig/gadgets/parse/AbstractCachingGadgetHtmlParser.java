/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetException;

import java.util.List;

/**
 * Abstract base class for {@code GadgetHtmlParser} classes that provides a caching
 * layer for parse trees. The cache is provided by a given {@code CacheProvider},
 * with the configured size. The class is also configured with the minimum parse
 * time, in milliseconds, for which it's worth caching contents at all. This
 * is a rough heuristic, but useful to avoid caching contents that are parsed faster
 * than a cache hit and serialization would take. A value <= 0 turns this feature off.
 * 
 * Essentially any real {@code GadgetHtmlParser} should extend this base class, as
 * its abstract method's signature is identical to the interface.
 */
public abstract class AbstractCachingGadgetHtmlParser implements GadgetHtmlParser {
  protected abstract List<ParsedHtmlNode> doParse(String source) throws GadgetException;
  
  private final Cache<String, byte[]> parseTreeCache;
  private final ParseTreeSerializer pts;
  private final int cacheTimeMsCutoff;
  
  protected AbstractCachingGadgetHtmlParser(CacheProvider cacheProvider, 
      int capacity, int cacheTimeMsCutoff) {
    if (cacheProvider != null && capacity > 0) {
      parseTreeCache = cacheProvider.createCache(capacity);
    } else {
      // Cache can be configured to do nothing for test instances, etc.
      parseTreeCache = new DoNothingCache();
    }
    pts = new ParseTreeSerializer();
    this.cacheTimeMsCutoff = cacheTimeMsCutoff;
  }

  public List<ParsedHtmlNode> parse(String source) throws GadgetException {
    // Cache key is MD5 of String
    String cacheKey = HashUtil.checksum(source.getBytes());
    byte[] cached = parseTreeCache.getElement(cacheKey);
    if (cached != null) {
      List<ParsedHtmlNode> ret = pts.deserialize(cached);
      if (ret != null) {
        // This might be null if the cached blob has timed out or has a different version.
        return ret;
      }
    }
    
    long parseStart = System.currentTimeMillis();
    List<ParsedHtmlNode> parsed = doParse(source);
    if (parsed == null) {
      return null;
    }
    
    if ((System.currentTimeMillis() - parseStart) > cacheTimeMsCutoff) {
      parseTreeCache.addElement(cacheKey, pts.serialize(parsed));
    }
    
    return parsed;
  }
  
  private static class DoNothingCache implements Cache<String, byte[]> {
    public void addElement(String key, byte[] value) { }
    public byte[] getElement(String key) { return null; }
    public byte[] removeElement(String key) { return null; }
  }
}