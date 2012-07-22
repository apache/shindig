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
package org.apache.shindig.gadgets.features;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that loads FeatureResource objects used to populate JS feature code.
 */
public class FeatureResourceLoader {

  // Class name for logging purpose
  private static final String classname = FeatureResourceLoader.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  private final HttpFetcher fetcher;
  private final TimeSource timeSource;
  private final FeatureFileSystem fileSystem;
  private int updateCheckFrequency = 0;  // <= 0 -> only load data once, don't check for updates.

  @Inject
  public FeatureResourceLoader(
      HttpFetcher fetcher, TimeSource timeSource, FeatureFileSystem fileSystem) {
    this.fetcher = fetcher;
    this.timeSource = timeSource;
    this.fileSystem = fileSystem;
  }

  @Inject(optional = true)
  public void setSupportFileUpdates(
      @Named("shindig.features.loader.file-update-check-frequency-ms") int updateCheckFrequency) {
    this.updateCheckFrequency = updateCheckFrequency;
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

  @SuppressWarnings("unused")
  protected FeatureResource loadFile(String path, Map<String, String> attribs) throws IOException {
    return new DualModeFileResource(getOptPath(path), path, attribs);
  }

  protected String getFileContent(FeatureFile file) {
    try {
      return file.getContent();
    } catch (IOException e) {
      // This is fine; errors happen downstream.
      return null;
    }
  }

  @SuppressWarnings("unused")
  protected FeatureResource loadResource(
      String path, Map<String, String> attribs) throws IOException {
    String optContent = null, debugContent = null;
    try {
      optContent = getResourceContent(getOptPath(path));
    } catch (IOException e) {
      // OK - optContent can be null. Error thrown downstream if both are null.
    }
    try {
      debugContent = getResourceContent(path);
    } catch (IOException e) {
      // See above; OK for debugContent to be null.
    }
    return new DualModeStaticResource(path, optContent, debugContent, attribs);
  }

  public String getResourceContent(String resource) throws IOException {
    return fileSystem.getResourceContent(resource);
  }

  /**
   * @throws IOException if failed to load uri (by derived classes)
   */
  protected FeatureResource loadUri(Uri uri, Map<String, String> attribs) throws IOException {
    String inline = attribs.get("inline");
    inline = inline != null ? inline : "";
    return new UriResource(fetcher, uri,
        "1".equals(inline) || "true".equalsIgnoreCase(inline),
        attribs);
  }

  protected String getOptPath(String orig) {
    if (orig.endsWith(".js") && !orig.endsWith(".opt.js")) {
      return orig.substring(0, orig.length() - 3) + ".opt.js";
    }
    return orig;
  }

  // Overridable for easier testing.
  protected boolean fileHasChanged(FeatureFile file, long lastModified) {
    return file.lastModified() > lastModified;
  }

  private class DualModeFileResource extends FeatureResource.Attribute {
    private final FileContent optContent;
    private final FileContent dbgContent;
    private final String fileName;

    protected DualModeFileResource(String optFilePath, String dbgFilePath,
        Map<String, String> attribs) {
      super(attribs);
      this.optContent = new FileContent(optFilePath);
      this.dbgContent = new FileContent(dbgFilePath);
      this.fileName = dbgFilePath;
      Preconditions.checkArgument(optContent.get() != null || dbgContent.get() != null,
        "Problems reading resource: %s", dbgFilePath);
    }

    public String getContent() {
      String opt = optContent.get();
      return opt != null ? opt : dbgContent.get();
    }

    public String getDebugContent() {
      String dbg = dbgContent.get();
      return dbg != null ? dbg : optContent.get();
    }

    public String getName() {
      return fileName;
    }

    private final class FileContent {
      private final String filePath;
      private long lastModified;
      private long lastUpdateCheckTime;
      private String content;

      private FileContent(String filePath) {
        this.filePath = filePath;
        this.lastModified = 0;
        this.lastUpdateCheckTime = 0;
      }

      private String get() {
        long nowTime = timeSource.currentTimeMillis();
        if (content == null ||
            (updateCheckFrequency > 0 &&
             (lastUpdateCheckTime + updateCheckFrequency) < nowTime)) {
          // Only check for file updates at preconfigured intervals. This prevents
          // overwhelming the file system while maintaining a reasonable update rate w/o
          // implementing a full event-driven mechanism.
          lastUpdateCheckTime = nowTime;
          FeatureFile file;
          try {
            file = fileSystem.getFile(filePath);
          } catch (IOException e) {
            return null;
          }
          if (fileHasChanged(file, lastModified)) {
            // Only reload file content if it's changed (or if it's the first
            // load, when this check will succeed).
            String newContent = getFileContent(file);
            if (newContent != null) {
              content = newContent;
              lastModified = file.lastModified();
            } else if (content != null) {
              // Content existed before, file removed - log error.
              if (LOG.isLoggable(Level.WARNING)) {
                LOG.logp(Level.WARNING, classname, "get", MessageKeys.MISSING_FILE,
                    new Object[] {filePath});
              }
            }
          }
        }
        return content;
      }
    }
  }

  private static final class DualModeStaticResource extends FeatureResource.Attribute {
    private final String content;
    private final String debugContent;
    private final String path;

    private DualModeStaticResource(
        String path, String content, String debugContent, Map<String, String> attribs) {
      super(attribs);
      this.content = content != null ? content : debugContent;
      this.debugContent = debugContent != null ? debugContent : content;
      this.path = path;
      Preconditions.checkArgument(this.content != null, "Problems reading resource: %s", path);
    }

    public String getContent() {
      return content;
    }

    public String getDebugContent() {
      return debugContent;
    }

    public String getName() {
      return path;
    }
  }

  private static final class UriResource extends FeatureResource.Attribute {
    private final HttpFetcher fetcher;
    private final Uri uri;
    private final boolean isInline;
    private String content;
    private long lastLoadTryMs;

    private UriResource(HttpFetcher fetcher, Uri uri, boolean isInline,
        Map<String, String> attribs) {
      super(attribs);
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
          HttpRequest request = new HttpRequest(uri).setInternalRequest(true);
          HttpResponse response = fetcher.fetch(request);
          if (response.getHttpStatusCode() == HttpResponse.SC_OK) {
            content = response.getResponseAsString();
          } else {
            if (LOG.isLoggable(Level.WARNING)) {
              LOG.logp(Level.WARNING, classname, "getContent", MessageKeys.UNABLE_RETRIEVE_LIB,
                  new Object[] {uri});
            }
          }
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "getContent", MessageKeys.UNABLE_RETRIEVE_LIB,
                new Object[] {uri});
          }
        }
      }

      return content;
    }

    public String getDebugContent() {
      return getContent();
    }

    @Override
    public boolean isExternal() {
      return !isInline;
    }

    @Override
    public boolean isProxyCacheable() {
      return content != null;
    }

    public String getName() {
      return uri.toString();
    }

  }
}
