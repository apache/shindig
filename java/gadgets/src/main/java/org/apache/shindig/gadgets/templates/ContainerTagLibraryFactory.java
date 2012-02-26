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
package org.apache.shindig.gadgets.templates;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Serves up a per-container tag library to the TemplateRewriter.
 */
@Singleton
public class ContainerTagLibraryFactory {
  private static final Logger LOG = Logger.getLogger(
      ContainerTagLibraryFactory.class.getName());

  private final ContainerConfig config;
  private final LoadingCache<String, TemplateLibrary> osmlLibraryCache = CacheBuilder
      .newBuilder()
      .build(new CacheLoader<String, TemplateLibrary>() {
          public TemplateLibrary load(String resourceName) {
            return loadTrustedLibrary(resourceName);
          }
        });

  @Inject
  public ContainerTagLibraryFactory(ContainerConfig config) {
    this.config = config;
  }

  /**
   * Return a per-container tag registry.
   */
  public TemplateLibrary getLibrary(String container) {
    return getOsmlLibrary(container);
  }

  private TemplateLibrary getOsmlLibrary(String container) {
    String library = config.getString(container,
        "${Cur['gadgets.features'].osml.library}");
    if (Strings.isNullOrEmpty(library)) {
      return NullTemplateLibrary.INSTANCE;
    }

    return osmlLibraryCache.getUnchecked(library);
  }

  static private TemplateLibrary loadTrustedLibrary(String resource) {
    try {
      String content = ResourceLoader.getContent(resource);
      return new XmlTemplateLibrary(Uri.parse("#OSML"), XmlUtil.parse(content),
          content, true);
    } catch (IOException ioe) {
      LOG.log(Level.WARNING, null, ioe);
    } catch (XmlException xe) {
      LOG.log(Level.WARNING, null, xe);
    } catch (GadgetException tpe) {
      LOG.log(Level.WARNING, null, tpe);
    }

    return NullTemplateLibrary.INSTANCE;
  }
}
