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
package org.apache.shindig.gadgets.rewrite.lexer;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.CssRewriter;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterResults;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Sets;
import com.google.common.collect.Maps;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of content rewriting.
 */
@Singleton
public class DefaultContentRewriter implements ContentRewriter {

  private final GadgetSpecFactory specFactory;

  private final String includeUrls;

  private final String excludeUrls;

  private final String expires;

  private final Set<String> includeTags;

  private final String proxyUrl;

  private final String concatUrl;

  @Inject
  public DefaultContentRewriter(
      GadgetSpecFactory specFactory,
      @Named("shindig.content-rewrite.include-urls")String includeUrls,
      @Named("shindig.content-rewrite.exclude-urls")String excludeUrls,
      @Named("shindig.content-rewrite.expires")String expires,
      @Named("shindig.content-rewrite.include-tags")String includeTags,
      @Named("shindig.content-rewrite.proxy-url")String proxyUrl,
      @Named("shindig.content-rewrite.concat-url")String concatUrl) {
    this.specFactory = specFactory;
    this.includeUrls = includeUrls;
    this.excludeUrls = excludeUrls;
    this.expires = expires;
    this.proxyUrl = proxyUrl;
    this.concatUrl = concatUrl;
    this.includeTags = Sets.newHashSet();
    for (String s : includeTags.split(",")) {
      if (s != null && s.trim().length() > 0) {
        this.includeTags.add(s.trim().toLowerCase());
      }
    }
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(
          (content.getContent().length() * 110) / 100);
      OutputStreamWriter output = new OutputStreamWriter(baos, original.getEncoding());
      String mimeType = original.getHeader("Content-Type");
      if (request.getRewriteMimeType() != null) {
        mimeType = request.getRewriteMimeType();
      }
      GadgetSpec spec = null;
      if (request.getGadget() != null) {
        spec = specFactory.getGadgetSpec(request.getGadget().toJavaUri(), false);
      }
      if (rewrite(spec, request.getUri(), content, mimeType, output)) {
        content.setContent(new String(baos.toByteArray()));
        return RewriterResults.cacheableIndefinitely();
      }
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    } catch (GadgetException ge) {
      // Couldn't retrieve gadgetSpec
    }
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    StringWriter sw = new StringWriter();
    GadgetSpec spec = gadget.getSpec();
    Uri base = spec.getUrl();
    View view = gadget.getCurrentView();
    if (view != null && view.getHref() != null) {
      base = view.getHref();
    }
    if (rewrite(spec, base, content, "text/html", sw)) {
      content.setContent(sw.toString());
      return RewriterResults.cacheableIndefinitely();
    }
    return null;
  }

  private boolean rewrite(GadgetSpec spec, Uri source, MutableContent mc, String mimeType, Writer w) {
    // Dont rewrite content if the spec is unavailable
    if (spec == null) {
      return false;
    }

    ContentRewriterFeature rewriterFeature
        = new ContentRewriterFeature(spec, includeUrls, excludeUrls, expires, includeTags);

    if (!rewriterFeature.isRewriteEnabled()) {
      return false;
    }
    if (isHTML(mimeType)) {
      Map<String, HtmlTagTransformer> transformerMap = Maps.newHashMap();

      if (getProxyUrl() != null) {
        LinkRewriter linkRewriter = createLinkRewriter(spec, rewriterFeature);
        LinkingTagRewriter rewriter = new LinkingTagRewriter(linkRewriter, source);
        Set<String> toProcess = Sets.intersection(rewriter.getSupportedTags(), rewriterFeature.getIncludedTags());

        for (String tag : toProcess) {
          transformerMap.put(tag, rewriter);
        }

        if (rewriterFeature.getIncludedTags().contains("style")) {
          transformerMap.put("style", new StyleTagRewriter(source, linkRewriter));
        }
      }
      if (getConcatUrl() != null && rewriterFeature.getIncludedTags().contains("script")) {
        transformerMap
            .put("script", new JavascriptTagMerger(spec, rewriterFeature, getConcatUrl(), source));
      }
      HtmlRewriter.rewrite(new StringReader(mc.getContent()), source, transformerMap, w);
      return true;
    } else if (isCSS(mimeType)) {
      if (getProxyUrl() != null) {
        CssRewriter.rewrite(new StringReader(mc.getContent()), source,
            createLinkRewriter(spec, rewriterFeature), w, false);
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  private boolean isHTML(String mime) {
    return mime != null && (mime.toLowerCase().contains("html"));
  }

  private boolean isCSS(String mime) {
    return mime != null && (mime.toLowerCase().contains("css"));
  }

  // TODO: This needs to be per-container
  protected String getProxyUrl() {
    return proxyUrl;
  }

  // TODO: This needs to be per-container
  protected String getConcatUrl() {
    return concatUrl;
  }

  protected LinkRewriter createLinkRewriter(GadgetSpec spec,
      ContentRewriterFeature rewriterFeature) {
    return new ProxyingLinkRewriter(spec.getUrl(), rewriterFeature, getProxyUrl());
  }
}
