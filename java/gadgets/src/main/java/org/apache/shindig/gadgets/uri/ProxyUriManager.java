/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.uri;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;

import com.google.common.collect.Lists;

import java.util.List;

public interface ProxyUriManager {
  /**
   * Generate a Uri that proxies the given resource Uri.
   * 
   * @param gadget Context for the rewrite
   * @param resource Resource Uri to proxy
   * @param forcedRefresh Forced expires value to use for resource
   * @return Uri of proxied resource
   */
  List<Uri> make(List<ProxyUri> resource, Integer forcedRefresh);
  
  public static class ProxyUri extends ProxyUriBase {
    private final Uri resource;
    
    public ProxyUri(Gadget gadget, Uri resource) {
      super(gadget);
      this.resource = resource;
    }
    
    public ProxyUri(UriStatus status, Uri resource, Uri base) {
      super(status, base);
      this.resource = resource;
    }
    
    public Uri getResource() {
      return resource;
    }
    
    public static List<ProxyUri> fromList(Gadget gadget, List<Uri> uris) {
      List<ProxyUri> res = Lists.newArrayListWithCapacity(uris.size());
      for (Uri uri : uris) {
        res.add(new ProxyUri(gadget, uri));
      }
      return res;
    }
  }
  
  /**
   * Parse and validate the proxied Uri.
   * 
   * @param uri A Uri presumed to be a proxied Uri generated
   *     by this class or in a compatible way
   * @return Status of the Uri passed in
   */
  ProxyUri process(Uri uri) throws GadgetException;
  
  public interface Versioner {
    /**
     * Generates a version for each of the provided resources.
     * @param resources Resources to version.
     * @param container Container making the request
     * @return Index-correlated list of version strings
     */
    List<String> version(List<Uri> resources, String container);
    
    /**
     * Validate the version of the resource.
     * @param resource Uri of a proxied resource
     * @param container Container requesting the resource
     * @param value Version value to validate.
     * @return Status of the version.
     */
    UriStatus validate(Uri resource, String container, String value);
  }
}
