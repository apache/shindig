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

import com.google.common.collect.ImmutableList;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import org.apache.shindig.gadgets.rewrite.ImageAttributeRewriter.ImageAttributeVisitor;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.MultipleResourceHttpFetcher.RequestContext;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Node;
import org.easymock.IMocksControl;
import org.easymock.EasyMock;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tests for {@code ImageAttributeRewriter}
 */
public class ImageAttributeRewriterTest extends DomWalkerTestBase {
  private RequestPipeline requestPipeline;
  private IMocksControl control;
  private transient ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final String IMG_JPG_SMALL_URL =
      "org/apache/shindig/gadgets/rewrite/image/small.jpg";
  private static final String IMG_JPG_LARGE_URL =
      "org/apache/shindig/gadgets/rewrite/image/large.jpg";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    control = EasyMock.createControl();
    requestPipeline = control.createMock(RequestPipeline.class);
  }

  @Test
  public void dontVisitImgTagWithClass() throws Exception {
    Node img = elem("img", "class", "classname", "src", IMG_JPG_SMALL_URL);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(img));
  }

  @Test
  public void dontVisitImgTagWithId() throws Exception {
    Node img = elem("img", "id", "idname", "src", IMG_JPG_SMALL_URL);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(img));
  }

  @Test
  public void dontVisitImgTagWithHeight() throws Exception {
    Node img = elem("img", "height", "30", "src", IMG_JPG_SMALL_URL);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(img));
  }

  @Test
  public void dontVisitImgTagWithWidth() throws Exception {
    Node img = elem("img", "width", "70", "src", IMG_JPG_SMALL_URL);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(img));
  }

  @Test
  public void dontVisitImgTagWithoutSrc() throws Exception {
    Node img = elem("img");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(img));
  }

  @Test
  public void visitImgTagWithSrc() throws Exception {
    Node img = elem("img", "src", IMG_JPG_SMALL_URL, "title", "test image");
    assertEquals(VisitStatus.RESERVE_NODE, getVisitStatus(img));
  }

  @Test
  public void revisitZeroNodes() throws Exception {
    assertEquals(false, getRevisitState(new ArrayList<Node>()));
  }

  @Test
  public void revisit() throws Exception {
    Node img1 = elem("img", "src", IMG_JPG_SMALL_URL);
    Node img2 = elem("img", "src", IMG_JPG_LARGE_URL);
    List<Node> nodes = ImmutableList.of(img1, img2);

    RequestContext reqCxtImg1 = createRequestContext(IMG_JPG_SMALL_URL, "image/jpeg");
    RequestContext reqCxtImg2 = createRequestContext(IMG_JPG_LARGE_URL, "image/jpeg");

    expect(requestPipeline.execute(eq(reqCxtImg1.getHttpReq())))
        .andReturn(reqCxtImg1.getHttpResp());
    expect(requestPipeline.execute(eq(reqCxtImg2.getHttpReq())))
        .andReturn(reqCxtImg2.getHttpResp());

    Node html = htmlDoc(new Node[] {}, img1, img2);

    String expectedContent = new StringBuilder()
        .append(".__shindig__image0 {\n")
        .append("  height: 16px;\n").append("  width: 16px;\n")
        .append("}\n")
        .append(".__shindig__image1 {\n")
        .append("  height: 125px;\n").append("  width: 108px;\n")
        .append("}\n").toString();

    control.replay();
    assertEquals(true, getRevisitState(nodes));
    Node head = doc.getElementsByTagName("head").item(0);
    assertEquals(1, head.getChildNodes().getLength());
    assertEquals("style", head.getFirstChild().getNodeName());
    assertEquals(expectedContent, head.getFirstChild().getTextContent());
    control.verify();
  }

  private VisitStatus getVisitStatus(Node node) throws Exception {
    return new ImageAttributeVisitor(requestPipeline, executor).visit(gadget(), node);
  }

  private boolean getRevisitState(List<Node> nodes) throws Exception{
    return new ImageAttributeVisitor(requestPipeline, executor).revisit(gadget(), nodes);
  }

  private RequestContext createRequestContext(String resource, String mimeType) throws Exception {
    HttpRequest request = null;
    Uri uri = UriBuilder.parse(resource).toUri();
    request = ImageAttributeVisitor.buildHttpRequest(gadget(), uri);


    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(resource));
    HttpResponse response =  new HttpResponseBuilder().addHeader("Content-Type", mimeType)
            .setResponse(bytes).create();

    return new RequestContext(request, response, null);
  }
}
