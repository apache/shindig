/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shindig.gadgets.http;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Key for use in an HttpCache
 *
 * The key is made up of name/value pairs.  By default only the HTTP method
 * and URL are included in the cache key.  Use the set() method to add
 * additional data to the cache key.
 */
public class HttpCacheKey {

  private Map<String, String> data;
  private boolean cacheable;

  /**
   * Create a cache key for the specified request.
   * 
   * @param request
   */
  public HttpCacheKey(HttpRequest request) {
    data = new HashMap<String,String>();
    setCacheable(true);
    if (!"GET".equals(request.getMethod()) ||
        request.getOptions().ignoreCache) {
      setCacheable(false);
    }
    // In theory we only cache GET, but including the method in the cache key
    // provides some additional insurance that we aren't mixing cache content.
    set("method", request.getMethod());
    set("url", request.getUri().toASCIIString());
  }
  
  /**
   * Add additional data to the cache key.
   */
  public void set(String key, String value) {
    data.put(key, value);
  }
  
  /**
   * Remove data from the cache key.
   */
  public void remove(String key) {
    data.remove(key);
  }
  
  public void setCacheable(boolean cacheable) {
    this.cacheable = cacheable;
  }
  
  public boolean isCacheable() {
    return cacheable;
  }
  
  /**
   * Figure out a string representation of this cache key.  The representation
   * will be:
   * 
   * canonical: identical sets of key/value pairs will always map to the same
   * string.
   * 
   * unique: different sets of key/value pairs will always map to different
   * strings.
   */
  @Override
  public String toString() {
    List<String> list = new ArrayList<String>();
    list.addAll(data.keySet());
    Collections.sort(list);
    JSONArray json = new JSONArray();
    for (String key : list) {
      json.put(Collections.singletonMap(key, data.get(key)));
    }
    return json.toString();
  }

}
