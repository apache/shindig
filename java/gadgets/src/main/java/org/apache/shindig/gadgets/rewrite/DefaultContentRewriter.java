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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
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
  private final Set<String> includeTags;

  @Inject
  public DefaultContentRewriter(
      GadgetSpecFactory specFactory,
      @Named("content-rewrite.include-urls")String includeUrls,
      @Named("content-rewrite.exclude-urls")String excludeUrls,
      @Named("content-rewrite.include-tags")String includeTags) {
    this.specFactory = specFactory;
    this.includeUrls = includeUrls;
    this.excludeUrls = excludeUrls;
    this.includeTags = new HashSet<String>();
    for (String s : includeTags.split(",")) {
      if (s != null && s.trim().length() > 0) {
        this.includeTags.add(s.trim().toLowerCase());
      }
    }
  }

  public HttpResponse rewrite(HttpRequest request, HttpResponse original) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(
          (original.getContentLength() * 110) / 100);
      OutputStreamWriter output = new OutputStreamWriter(baos,
          original.getEncoding());
      String mimeType = original.getHeader("Content-Type");
      if (request.getOptions() != null && request.getOptions().rewriteMimeType != null) {
        mimeType = request.getOptions().rewriteMimeType;
      }
      GadgetSpec spec = null;
      if (request.getOptions() != null && request.getOptions().gadgetUri != null) {
        spec = specFactory.getGadgetSpec(request.getOptions().gadgetUri, false);
      }
      if (rewrite(spec, request.getUri(),
          new InputStreamReader(original.getResponse(), original.getEncoding()),
          mimeType,
          output)) {
        return new HttpResponse(original.getHttpStatusCode(),
            baos.toByteArray(),
            original.getAllHeaders());
      }
      return null;
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    } catch (GadgetException ge) {
      return null;
    }
  }

  public String rewriteGadgetView(GadgetSpec spec, String view, String mimeType) {
    StringWriter sw = new StringWriter();
    if (rewrite(spec, spec.getUrl(), new StringReader(view), mimeType, sw)) {
      return sw.toString();
    } else {
      return null;
    }
  }

  private boolean rewrite(GadgetSpec spec, URI source, Reader r, String mimeType, Writer w) {
    // Dont rewrite content if the spec is unavailable
    if (spec == null) {
      return false;
    }

    // Store the feature in the spec so we dont keep parsing it
    ContentRewriterFeature rewriterFeature = (ContentRewriterFeature)spec.getAttribute("content-rewrite");
    if (rewriterFeature == null) {
      rewriterFeature = new ContentRewriterFeature(spec, includeUrls, excludeUrls, includeTags);
      spec.setAttribute("content-rewrite", rewriterFeature);
    }

    if (!rewriterFeature.isRewriteEnabled()) {
      return false;
    }
    if (isHTML(mimeType)) {
      Map<String, HtmlTagTransformer> transformerMap
          = new HashMap<String, HtmlTagTransformer>();

      if (getProxyUrl() != null) {
        LinkRewriter linkRewriter = createLinkRewriter(spec, rewriterFeature);
        LinkingTagRewriter rewriter = new LinkingTagRewriter(
            linkRewriter,
            source);
        Set<String> toProcess = new HashSet<String>(rewriter.getSupportedTags());
        toProcess.retainAll(rewriterFeature.getIncludedTags());
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
      HtmlRewriter.rewrite(r, source, transformerMap, w);
      return true;
    } else if (isCSS(mimeType)) {
      if (getProxyUrl() != null) {
        CssRewriter.rewrite(r, source, createLinkRewriter(spec, rewriterFeature), w);
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  private boolean isHTML(String mime) {
    if (mime == null) return false;
    return (mime.toLowerCase().contains("html"));
  }

  private boolean isCSS(String mime) {
    if (mime == null) return false;
    return (mime.toLowerCase().contains("css"));
  }

  protected String getProxyUrl() {
    return "/gadgets/proxy?url=";
  }

  protected String getConcatUrl() {
    return "/gadgets/concat?";
  }

  protected LinkRewriter createLinkRewriter(GadgetSpec spec, ContentRewriterFeature rewriterFeature) {
    return new ProxyingLinkRewriter(spec.getUrl(), rewriterFeature, getProxyUrl());
  }
}
