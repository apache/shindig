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
 */
package org.apache.shindig.gadgets.features;

import com.google.inject.Inject;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class that loads FeatureResource objects used to populate JS feature code.
 */
public class FeatureResourceLoader {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");
  
  private HttpFetcher fetcher;

  @Inject
  public void setHttpFetcher(HttpFetcher fetcher) {
    this.fetcher = fetcher;
  }
  
  /**
   * Primary, and only public, method of FeatureResourceLoader. Loads the resource
   * keyed at the given {@code uri}, which was decorated with the provided list of attributes.
   * 
   * The default implementation loads both file and res-schema resources using
   * ResourceLoader, attempting to load optimized content for files named [file].js as [file].opt.js.
   * 
   * Override this method to provide custom functionality. Basic loadFile, loadResource, and loadUri
   * methods are kept protected for easy reuse.
   * 
   * @param uri Uri of resource to be loaded.
   * @param attribs Attributes decorating the resource in the corresponding feature.xml
   * @return FeatureResource object providing content and debugContent loading capability.
   * @throws GadgetException If any failure occurs during this process.
   */
  public FeatureResource load(Uri uri, Map<String, String> attribs) throws GadgetException {
    try {
      if ("file".equals(uri.getScheme())) {
        return loadFile(uri.getPath(), attribs);
      } else if ("res".equals(uri.getScheme())) {
        return loadResource(uri.getPath(), attribs);
      }
      return loadUri(uri, attribs);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, e);
    }
  }
  
  protected FeatureResource loadFile(String path, Map<String, String> attribs) throws IOException {
    return new DualModeStaticResource(path, getFileContent(new File(getOptPath(path))),
        getFileContent(new File(path)));
  }
  
  protected String getFileContent(File file) {
    try {
      return ResourceLoader.getContent(file);
    } catch (IOException e) {
      // This is fine; errors happen downstream.
      return null;
    }
  }
  
  protected FeatureResource loadResource(
      String path, Map<String, String> attribs) throws IOException {
    return new DualModeStaticResource(path, getResourceContent(getOptPath(path)),
        getResourceContent(path));
  }
  
  protected String getResourceContent(String resource) {
    try {
      return ResourceLoader.getContent(resource);
    } catch (IOException e) {
      return null;
    }
  }
  
  protected FeatureResource loadUri(Uri uri, Map<String, String> attribs) {
    String inline = attribs.get("inline");
    inline = inline != null ? inline : "";
    return new UriResource(fetcher, uri, "1".equals(inline) || "true".equalsIgnoreCase(inline));
  }
  
  protected String getOptPath(String orig) {
    if (orig.endsWith(".js") && !orig.endsWith(".opt.js")) {
      return orig.substring(0, orig.length() - 3) + ".opt.js";
    }
    return orig;
  }
  
  private static class DualModeStaticResource extends FeatureResource.Default {
    private final String content;
    private final String debugContent;
    
    private DualModeStaticResource(String path, String content, String debugContent) {
      this.content = content != null ? content : debugContent;
      this.debugContent = debugContent != null ? debugContent : content;
      if (this.content == null) {
        throw new IllegalArgumentException("Problems reading resource: " + path);
      }
    }

    public String getContent() {
      return content;
    }

    public String getDebugContent() {
      return debugContent;
    }
  }
  
  private static class UriResource implements FeatureResource {
    private final HttpFetcher fetcher;
    private final Uri uri;
    private final boolean isInline;
    private String content;
    private long lastLoadTryMs;
    
    private UriResource(HttpFetcher fetcher, Uri uri, boolean isInline) {
      this.fetcher = fetcher;
      this.uri = uri;
      this.isInline = isInline;
      this.lastLoadTryMs = 0;
      this.content = getContent();
    } 

    public String getContent() {
      if (isExternal()) {
        return uri.toString();
      } else if (content != null) {
        // Variable content is a one-time content cache for inline JS features.
        return content;
      }
      
      // Try to load the content. Ideally, and most of the time, this
      // will happen immediately at startup. However, if the target server is
      // down it shouldn't hose the entire server, so in that case we defer
      // and try at most once per minute thereafter, the delay in place to
      // avoid overwhelming a server down on its heels.
      long now = System.currentTimeMillis();
      if (fetcher != null && now > (lastLoadTryMs + (60 * 1000))) {
        lastLoadTryMs = now;
        try {
          HttpRequest request = new HttpRequest(uri);
          HttpResponse response = fetcher.fetch(request);
          if (response.getHttpStatusCode() == HttpResponse.SC_OK) {
            content = response.getResponseAsString();
          } else {
            logger.warning("Unable to retrieve remote library from " + uri);
          }
        } catch (GadgetException e) {
          logger.warning("Unable to retrieve remote library from " + uri);
        }
      }
      
      return content;
    }

    public String getDebugContent() {
      return getContent();
    }

    public boolean isExternal() {
      return !isInline;
    }
    
    public boolean isProxyCacheable() {
      return content != null;
    }

  }
}
