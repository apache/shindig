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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher.RequestContext;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Rewriter that adds height/width attributes to <img> tags.
 */
public class ImageAttributeRewriter extends DomWalker.Rewriter {
  //class name for logging purpose
    private static final String classname = ImageAttributeRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  @Inject
  public ImageAttributeRewriter(RequestPipeline requestPipeline, ExecutorService executor) {
    super(new ImageAttributeVisitor(requestPipeline, executor));
  }

  /**
   * Visitor that injects height/width attributes for <img> tags, if needed to
   * reduce the page reflows.
   */
  public static class ImageAttributeVisitor implements DomWalker.Visitor {
    private final RequestPipeline requestPipeline;
    private final ExecutorService executor;

    private static final String IMG_ATTR_CLASS_NAME_PREFIX = "__shindig__image";

    public ImageAttributeVisitor(RequestPipeline requestPipeline,
                                 @Named("shindig.concat.executor") ExecutorService executor) {
      this.requestPipeline = requestPipeline;
      this.executor = executor;
    }

    public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          node.getNodeName().equalsIgnoreCase("img")) {
        Element imageElement = (Element) node;

        // we process the <img> tag when it does not have 'class' and 'id'
        // attributes in order to avoid conflicts from css styles.
        if ("".equals(imageElement.getAttribute("class")) &&
            "".equals(imageElement.getAttribute("id")) &&
            !"".equals(imageElement.getAttribute("src")) &&
            "".equals(imageElement.getAttribute("height")) &&
            "".equals(imageElement.getAttribute("width"))) {
          return VisitStatus.RESERVE_NODE;
        }
      }
      return VisitStatus.BYPASS;
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
      if (nodes.isEmpty()) {
        return false;
      }
      Node head = DomUtil.getFirstNamedChildNode(
          nodes.get(0).getOwnerDocument().getDocumentElement(), "head");

      if (head == null) {
        // Should never occur; do for paranoia's sake.
        return false;
      }

      List<HttpRequest> resourceRequests = Lists.newArrayList();
      for (Node node : nodes) {
        String imgSrc = ((Element) node).getAttribute("src");
        Uri uri = UriBuilder.parse(imgSrc).toUri();
        try {
          resourceRequests.add(buildHttpRequest(gadget, uri));
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "revisit", MessageKeys.UNABLE_TO_PROCESS_IMG, new Object[] {imgSrc});
          }
        }
      }

      MultipleResourceHttpFetcher fetcher =
          new MultipleResourceHttpFetcher(requestPipeline, executor);
      Map<Uri, FutureTask<RequestContext>> futureTasks = fetcher.fetchUnique(resourceRequests);
      String cssContent = processAllImgResources(nodes, futureTasks);

      if (cssContent.length() > 0) {
        Element style = nodes.get(0).getOwnerDocument().createElement("style");
        style.setAttribute("type", "text/css");
        style.setTextContent(cssContent);
        head.insertBefore(style, head.getFirstChild());
      }
      return true;
    }

    /**
     * The method process all the images,  determine which of them are safe for
     * injecting css styles for height/width extracted from the image metadata,
     * and returns the string of css styles that needed to injected.
     *
     * @param nodes nodes list of nodes for this we want to height/width
     *    attribute injection in css.
     * @param futureTasks futureTasks map of url -> futureTask for all the requests sent.
     * @return string contianing the css styles that needs to be injected.
     */
    private String processAllImgResources(List<Node> nodes,
                                          Map<Uri, FutureTask<RequestContext>> futureTasks) {
      StringBuilder cssContent = new StringBuilder("");

      for (int i = 0; i < nodes.size(); i++) {
        Element imageElement = (Element) nodes.get(i);
        String src = imageElement.getAttribute("src");
        RequestContext requestCxt;

        // Fetch the content of the requested uri.
        try {
          Uri imgUri = UriBuilder.parse(src).toUri();

          try {
            requestCxt = futureTasks.get(imgUri).get();
          } catch (InterruptedException ie) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, ie);
          } catch (ExecutionException ie) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, ie);
          }

          if (requestCxt.getGadgetException() != null) {
            throw requestCxt.getGadgetException();
          }

          HttpResponse response = requestCxt.getHttpResp();
          // Content header checking is fast so this is fine to do for every
          // response.
          ImageFormat imageFormat = Sanselan.guessFormat(
              new ByteSourceInputStream(response.getResponse(), imgUri.getPath()));

          if (imageFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
             // skip this node
            continue;
          }

          // extract height and width from the actual image and set these
          // attributes of the <img> tag.
          ImageInfo imageInfo = Sanselan.getImageInfo(response.getResponse(),
                                                      imgUri.getPath());

          if (imageInfo == null) {
            continue;
          }

          int imageHeight = imageInfo.getHeight();
          int imageWidth = imageInfo.getWidth();

          if (imageHeight > 0 && imageWidth > 0 && imageHeight * imageWidth > 1) {
            imageElement.setAttribute("class", IMG_ATTR_CLASS_NAME_PREFIX + i);
            cssContent.append('.').append(IMG_ATTR_CLASS_NAME_PREFIX).append(i).append(" {\n")
              .append("  height: ").append(imageHeight).append("px;\n")
              .append("  width: ").append(imageWidth).append("px;\n")
              .append("}\n");
          }
        } catch (ImageReadException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "processAllImgResources", MessageKeys.UNABLE_TO_READ_RESPONSE, new Object[] {src});
          }
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "processAllImgResources", MessageKeys.UNABLE_TO_FETCH_IMG, new Object[] {src});
          }
        } catch (IOException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "processAllImgResources", MessageKeys.UNABLE_TO_PARSE_IMG, new Object[] {src});
          }
        }
      }

      return cssContent.toString();
    }

    // TODO(satya): Need to pass the request parameters as well ?
    public static HttpRequest buildHttpRequest(Gadget gadget, Uri imgUri)
        throws GadgetException {
      HttpRequest req = new HttpRequest(imgUri);
      req.setFollowRedirects(true);
      return req;
    }
  }
}
