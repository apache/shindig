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
package org.apache.shindig.gadgets.render;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OpenSocialI18NGadgetRewriterTest {
  private OpenSocialI18NGadgetRewriter i18nRewriter;
  private Locale localeAtRendering;

  @Before
  public void setUp() throws Exception {
    i18nRewriter = new FakeOpenSocialI18NGadgetRewriter();
  }

  @Test
  public void localeNameForEnglish() throws Exception {
    localeAtRendering = new Locale("en");
    assertEquals("en",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForEnglishUS() throws Exception {
    localeAtRendering = new Locale("en", "US");
    assertEquals("en_US",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForChinese() throws Exception {
    localeAtRendering = new Locale("zh");
    assertEquals("zh",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForChineseCN() throws Exception {
    localeAtRendering = new Locale("zh", "CN");
    assertEquals("zh_CN",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForChineseAll() throws Exception {
    localeAtRendering = new Locale("zh", "All");
    assertEquals("zh",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForAllCN() throws Exception {
    localeAtRendering = new Locale("All", "CN");
    assertEquals("en",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForDefault() throws Exception {
    localeAtRendering = new Locale("All", "All");
    assertEquals("en",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForInvalidCountry() throws Exception {
    localeAtRendering = new Locale("zh", "foo");
    assertEquals("zh",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForInvalidLanguage() throws Exception {
    localeAtRendering = new Locale("foo", "CN");
    assertEquals("en",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  @Test
  public void localeNameForInvalidLanguageAndCountry() throws Exception {
    localeAtRendering = new Locale("foo", "foo");
    assertEquals("en",
                 i18nRewriter.getLocaleNameForLoadingI18NConstants(localeAtRendering));
  }

  private static class FakeOpenSocialI18NGadgetRewriter extends OpenSocialI18NGadgetRewriter {
    private Map<String, String> resources = new HashMap<String,String>();
    public FakeOpenSocialI18NGadgetRewriter() {
      resources.put("features/i18n/data/DateTimeConstants__en.js", "content for en");
      resources.put("features/i18n/data/DateTimeConstants__en_US.js", "content for en_US");
      resources.put("features/i18n/data/DateTimeConstants__zh.js", "content for zh");
      resources.put("features/i18n/data/DateTimeConstants__zh_CN.js", "content for zh_CN");
    }

    @Override
    protected String attemptToLoadDateConstants(String localeName) throws IOException {
      String resource = "features/i18n/data/DateTimeConstants__" + localeName + ".js";
      if (resources.containsKey(resource)) {
        return resources.get(resource);
      } else {
        throw new IOException("Resource Unavailable.");
      }
    }
  }
}

