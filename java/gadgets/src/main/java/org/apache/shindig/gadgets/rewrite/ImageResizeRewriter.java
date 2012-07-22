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
package org.apache.shindig.gadgets.rewrite;

import com.google.inject.Inject;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.apache.shindig.common.uri.Uri;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;

/**
 * This rewriter helps in appending the image size parameters (extracted from inline styles, height
 * and width) to the proxied resource urls so that server side resizing can be done when ever
 * possible. Non-proxied resource URLs are ignored by this rewriter.
 */
public class ImageResizeRewriter extends DomWalker.Rewriter {
  private final ContentRewriterFeature.Factory featureConfigFactory;
  private final ProxyUriManager proxyUriManager;

  @Inject
  public ImageResizeRewriter(ProxyUriManager proxyUriManager,
                             ContentRewriterFeature.Factory featureConfigFactory) {
    this.featureConfigFactory = featureConfigFactory;
    this.proxyUriManager = proxyUriManager;
  }

  @Override
  protected List<DomWalker.Visitor> makeVisitors(Gadget context, Uri gadgetUri) {
    ContentRewriterFeature.Config config = featureConfigFactory.get(context.getSpec());
    return Arrays.<DomWalker.Visitor>asList(new ImageResizeVisitor(proxyUriManager, config));
  }

  public static class ImageResizeVisitor implements DomWalker.Visitor {
    protected final ProxyUriManager proxyUriManager;
    protected final ContentRewriterFeature.Config featureConfig;

    public ImageResizeVisitor(ProxyUriManager proxyUriManager,
                              ContentRewriterFeature.Config featureConfig) {
      this.proxyUriManager = proxyUriManager;
      this.featureConfig = featureConfig;
    }

    public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          node.getNodeName().equalsIgnoreCase("img")) {
        Element imageElement = (Element) node;

        // We process the <img> tag in following cases
        // a) it has 'height' and 'width' but no 'id' and 'class' attributes.
        // b) it has inline style attribute.
        // TODO(satya): please beware of max-height, etc fields.
        if ((!isEmpty(imageElement, "height") && !isEmpty(imageElement, "width") &&
             isEmpty(imageElement, "id") && isEmpty(imageElement, "class")) ||
            (!isEmpty(imageElement, "style"))) {
          return addHeightWidthParams(imageElement);
        }
      }
      return VisitStatus.BYPASS;
    }

    private boolean isEmpty(Element element, String attribute) {
      return "".equals(element.getAttribute(attribute));
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
      return true;
    }

    private VisitStatus addHeightWidthParams(Element imgElement) {
      // We want to append image resize params only to urls that are proxied through us.
      String uriStr = imgElement.getAttribute("src").trim();
      Uri uri = Uri.parse(uriStr);
      ProxyUriManager.ProxyUri proxied;

      // Try parsing this uri as a ProxyUri.
      try {
        proxied = proxyUriManager.process(uri);
      } catch (GadgetException e) {
        return VisitStatus.BYPASS;
      }

      if (null == proxied || proxied.getStatus() == UriStatus.BAD_URI) {
        return VisitStatus.BYPASS;
      }

      VisitStatus status = VisitStatus.BYPASS;

      // We consider only cases where both image dimensions are in 'px' format. As '%', 'em'
      // units are relative to the parent, it is more difficult to infer those values.
      // Specifically, we consider only the following cases:
      //   i) style specifies both height and width
      //   ii) height and width are both specified and style does not specify these attributes.
      //   iii) height and width are both specified and style overrides one of these.
      // All other cases are ignored.
      Integer height = getIntegerPrefix(imgElement.getAttribute("height").trim());
      Integer width = getIntegerPrefix(imgElement.getAttribute("width").trim());
      if (null == height || null == width) {
        height = null;
        width = null;
      }

      // Inline style tags trump everything, including inline height/width attribute,
      // height/width inherited from css.
      if (!"".equals(imgElement.getAttribute("style"))) {
        String styleStr = imgElement.getAttribute("style");

        for (String attr : Splitter.on(';').split(styleStr)) {
          String[] splits = StringUtils.split(attr, ':');
          if (splits.length != 2) {
            continue;
          }

          if ("height".equalsIgnoreCase(splits[0].trim())) {
            Integer styleHeight = getIntegerPrefix(splits[1].trim());
            if (null != styleHeight) {
              height = styleHeight;
            }
          }

          if ("width".equalsIgnoreCase(splits[0].trim())) {
            Integer styleWidth = getIntegerPrefix(splits[1].trim());
            if (null != styleWidth) {
              width = styleWidth;
            }
          }
        }
      }

      if (null != height && null != width) {
        proxied.setResize(width, height, null, true);
        List<Uri> updatedUri = proxyUriManager.make(Lists.newArrayList(proxied),
                                                    featureConfig.getExpires());
        if (updatedUri.size() == 1) {
          imgElement.setAttribute("src", updatedUri.get(0).toString());
          status = VisitStatus.MODIFY;
        }
      }

      return status;
    }

    private Integer getIntegerPrefix(String input) {
      String integerPrefix = "";
      if (NumberUtils.isDigits(input)) {
        integerPrefix = input;
      } else if (input.endsWith("px") &&
                 NumberUtils.isDigits(input.substring(0, input.length() - 2))) {
        integerPrefix = input.substring(0, input.length() - 2);
      }

      Integer value = null;
      if (!"".equals(integerPrefix)) {
        try {
          value = NumberUtils.createInteger(integerPrefix);
        } catch (NumberFormatException e) {
          // ignore
        }
      }
      return value;
    }
  }
}
