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
package org.apache.shindig.gadgets.templates.tags;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;

/**
 * Implement the os:Flash tag
 */
public class FlashTagHandler extends AbstractTagHandler {

  static final String SWFOBJECT = "swfobject";
  static final String TAG_NAME = "Flash";

  private final BeanJsonConverter beanConverter;
  private final FeatureRegistry featureRegistry;
  private final String flashMinVersion;

  /**
   * Used to generate id's for generated tags and functions
   */
  final AtomicLong idGenerator = new AtomicLong();
  private static final String ALT_CONTENT_PREFIX = "os_xFlash_alt_";

  @Inject
  public FlashTagHandler(BeanJsonConverter beanConverter, FeatureRegistry featureRegistry,
      @Named("shindig.template-rewrite.extension-tag-namespace") String namespace,
      @Named("shindig.flash.min-version") String flashMinVersion) {
    super(namespace, TAG_NAME);
    this.beanConverter = beanConverter;
    this.featureRegistry = featureRegistry;
    this.flashMinVersion = flashMinVersion;
  }

  public void process(Node result, Element tag, TemplateProcessor processor) {
    SwfObjectConfig config;
    try {
      config = getSwfConfig(tag, processor);
    } catch (RuntimeException re) {
      // Record the processing error into the output
      Element err = result.getOwnerDocument().createElement("span");
      err.setTextContent("Failed to process os:Flash tag: " +
          StringEscapeUtils.escapeHtml4(re.getMessage()));
      result.appendChild(err);
      return;
    }

    // Bind the security token to the flashvars if its available
    String st = processor.getTemplateContext().getGadget()
        .getContext().getParameter("st");
    if (!Strings.isNullOrEmpty(st)) {
      String stVar = "st=" + Utf8UrlCoder.encode(st);
      if (Strings.isNullOrEmpty(config.flashvars)) {
        config.flashvars = stVar;
      } else {
        config.flashvars += '&' + stVar;
      }
    }

    // Restrict the content if sanitization is enabled
    if (processor.getTemplateContext().getGadget().sanitizeOutput()) {
      config.allowscriptaccess = SwfObjectConfig.ScriptAccess.never;
      config.swliveconnect = false;
      config.allownetworking = SwfObjectConfig.NetworkAccess.internal;
      // TODO - Implement container control over autoplay on views
    }

    // Create a div wrapper around the provided alternate content
    Element altHolder = result.getOwnerDocument().createElement("div");
    String altContentId = ALT_CONTENT_PREFIX + idGenerator.incrementAndGet();
    altHolder.setAttribute("id", altContentId);
    result.appendChild(altHolder);

    // Add the alternate content to the holder
    NodeList alternateContent = tag.getChildNodes();
    if (alternateContent.getLength() > 0) {
      processor.processChildNodes(altHolder, tag);
    }

    // Create the call to swfobject
    String swfObjectCall = buildSwfObjectCall(config, altContentId);
    Element script = result.getOwnerDocument().createElement("script");
    script.setAttribute("type", "text/javascript");
    result.appendChild(script);

    if (config.play == SwfObjectConfig.Play.immediate) {
      // Call swfobject immediately
      script.setTextContent(swfObjectCall);
    } else {
      // Add onclick handler to trigger call to swfobject
      script.setTextContent("function " + altContentId + "(){ " + swfObjectCall + " }");
      altHolder.setAttribute("onclick", altContentId + "()");
    }

    // Bypass sanitization for the holder tag and the call to swfobject
    SanitizingGadgetRewriter.bypassSanitization(altHolder, false);
    SanitizingGadgetRewriter.bypassSanitization(script, false);
    ensureSwfobject(result.getOwnerDocument(), processor);
  }

  /**
   * Generate the correctly parameterized Javascript call to swfobject
   */
  String buildSwfObjectCall(SwfObjectConfig config, String altContentId) {
    try {
      StringBuilder builder = new StringBuilder();
      builder.append("swfobject.embedSWF(");
      JsonSerializer.appendString(builder, config.swf.toString());
      builder.append(",\"");
      builder.append(altContentId);
      builder.append("\",");
      JsonSerializer.appendString(builder, config.width);
      builder.append(',');
      JsonSerializer.appendString(builder, config.height);
      builder.append(",\"").append(flashMinVersion).append("\",");
      builder.append("null,null,");
      JsonSerializer.appendMap(builder, config.getParams());
      builder.append(',');
      JsonSerializer.appendMap(builder, config.getAttributes());
      builder.append(");");
      return builder.toString();
    } catch (IOException ioe) {
      // Should not happen
      throw new RuntimeException(ioe);
    }

  }

  /**
   * Read the swfconfig from the tag
   */
  SwfObjectConfig getSwfConfig(Element tag, TemplateProcessor processor) {
    Map<String, String> params = getAllAttributesLowerCase(tag, processor);
    return (SwfObjectConfig) beanConverter.convertToObject(new JSONObject(params),
        SwfObjectConfig.class);
  }

  Map<String, String> getAllAttributesLowerCase(Element tag, TemplateProcessor processor) {
    Map<String, String> result = Maps.newHashMap();
    for (int i = 0; i < tag.getAttributes().getLength(); i++) {
      Node attr = tag.getAttributes().item(i);
      String attrName = attr.getNodeName().toLowerCase();
      result.put(attrName, processor.evaluate(attr.getNodeValue(), String.class, null));
    }
    return result;
  }

  /**
   * Ensure that the swfobject JS is inlined
   */
  void ensureSwfobject(Document doc, TemplateProcessor processor) {
    // TODO: This should probably be a function of the rewriter.
    Element head = (Element) DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
    Node childNode = head.getFirstChild();
    while(childNode != null) {
      if (childNode.getUserData(SWFOBJECT) != null) {
        return;
      }
      childNode = childNode.getNextSibling();
    }
    Element swfobject = doc.createElement("script");
    swfobject.setAttribute("type", "text/javascript");
    List<FeatureResource> resources =
        featureRegistry.getFeatureResources(processor.getTemplateContext().getGadget().getContext(),
          ImmutableSet.of(SWFOBJECT), null).getResources();
    for (FeatureResource resource : resources) {
      // Emits all content for feature SWFOBJECT, which has no downstream dependencies.
      swfobject.setTextContent(resource.getContent());
    }
    swfobject.setUserData(SWFOBJECT, SWFOBJECT, null);
    head.appendChild(swfobject);
    SanitizingGadgetRewriter.bypassSanitization(swfobject, false);
  }

  /**
   * Definition of the flash tag and mapping to swfobject structures
   */
  public static class SwfObjectConfig {
    String id;
    Uri swf;
    String width = "100px";
    String height = "100px";
    String name;
    String clazz;
    Boolean menu;

    public static enum Play { immediate, onclick }
    Play play = Play.immediate;

    public static enum Scale { showall, noborder, exactfit, noscale }
    Scale scale;

    public static enum WMode { window, opaque, transparent, direct, gpu}
    WMode wmode;

    Boolean devicefont;
    Boolean swliveconnect;

    public static enum ScriptAccess { always, samedomain, never }
    ScriptAccess allowscriptaccess;

    Boolean loop;

    public static enum Quality { best, high, medium, autohigh, autolow, low }
    Quality quality;

    public static enum Align { middle, left, right, top, bottom }
    Align align;

    public static enum SAlign { tl, tr, bl, br, l, t, r, b}
    SAlign salign;

    String bgcolor;

    Boolean seamlesstabbing;

    Boolean allowfullscreen;

    public static enum NetworkAccess { all, internal, none }
    NetworkAccess allownetworking;

    String flashvars;

    public void setId(String id) {
      this.id = id;
    }

    public void setSwf(Uri swf) {
      this.swf = swf;
    }

    public void setWidth(String width) {
      this.width = width;
    }

    public void setHeight(String height) {
      this.height = height;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setClass(String clazz) {
      this.clazz = clazz;
    }

    public void setPlay(Play play) {
      this.play = play;
    }

    public void setMenu(Boolean menu) {
      this.menu = menu;
    }

    public void setScale(Scale scale) {
      this.scale = scale;
    }

    public void setWmode(WMode wmode) {
      this.wmode = wmode;
    }

    public void setDevicefont(Boolean devicefont) {
      this.devicefont = devicefont;
    }

    public void setSwliveconnect(Boolean swliveconnect) {
      this.swliveconnect = swliveconnect;
    }

    public void setAllowscriptaccess(ScriptAccess allowscriptaccess) {
      this.allowscriptaccess = allowscriptaccess;
    }

    public void setLoop(Boolean loop) {
      this.loop = loop;
    }

    public void setQuality(Quality quality) {
      this.quality = quality;
    }

    public void setAlign(Align align) {
      this.align = align;
    }

    public void setSalign(SAlign salign) {
      this.salign = salign;
    }

    public void setBgcolor(String bgcolor) {
      this.bgcolor = bgcolor;
    }

    public void setSeamlesstabbing(Boolean seamlesstabbing) {
      this.seamlesstabbing = seamlesstabbing;
    }

    public void setAllowfullscreen(Boolean allowfullscreen) {
      this.allowfullscreen = allowfullscreen;
    }

    public void setAllownetworking(NetworkAccess allownetworking) {
      this.allownetworking = allownetworking;
    }

    public void setFlashvars(String flashvars) {
      this.flashvars = flashvars;
    }

    public Map<String, Object> getParams() {

      Map<String, Object> swfobjectParams = Maps.newLinkedHashMap();
      if (loop != null) {
        swfobjectParams.put("loop", loop);
      }
      if (menu != null) {
        swfobjectParams.put("menu", menu);
      }
      if (quality != null) {
        swfobjectParams.put("quality", quality);
      }
      if (scale != null) {
        swfobjectParams.put("scale", scale);
      }
      if (salign != null) {
        swfobjectParams.put("salign", salign);
      }
      if (wmode != null) {
        swfobjectParams.put("wmode", wmode);
      }
      if (bgcolor != null) {
        swfobjectParams.put("bgcolor", bgcolor);
      }
      if (swliveconnect != null) {
        swfobjectParams.put("swliveconnect", swliveconnect);
      }
      if (flashvars != null) {
        swfobjectParams.put("flashvars", flashvars);
      }
      if (devicefont != null) {
        swfobjectParams.put("devicefont", devicefont);
      }
      if (allowscriptaccess != null) {
        swfobjectParams.put("allowscriptaccess", allowscriptaccess);
      }
      if (seamlesstabbing != null) {
        swfobjectParams.put("seamlesstabbing", seamlesstabbing);
      }
      if (allowfullscreen != null) {
        swfobjectParams.put("allowfullscreen", allowfullscreen);
      }
      if (allownetworking != null) {
        swfobjectParams.put("allownetworking", allownetworking);
      }
      return swfobjectParams;
    }

    public Map<String, Object> getAttributes() {
      Map<String, Object> swfObjectAttrs = Maps.newLinkedHashMap();
      if (id != null) {
        swfObjectAttrs.put("id", id);
      }
      if (name != null) {
        swfObjectAttrs.put("name", name);
      }
      if (clazz != null) {
        swfObjectAttrs.put("styleclass", clazz);
      }
      if (align != null) {
        swfObjectAttrs.put("align", align.toString());
      }
      return swfObjectAttrs;
    }
  }
}
