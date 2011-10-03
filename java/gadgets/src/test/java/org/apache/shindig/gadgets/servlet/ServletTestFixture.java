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
package org.apache.shindig.gadgets.servlet;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.servlet.HttpServletResponseRecorder;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.FeedProcessorImpl;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.DefaultResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;

import com.google.inject.Provider;

/**
 * Contains everything needed for making servlet requests, plus a bunch of stuff that shouldn't be
 * here.
 *
 * TODO: Get rid of 'stuff that shouldn't be here'.
 */
public abstract class ServletTestFixture extends EasyMockTestCase {
  public final RequestPipeline pipeline = mock(RequestPipeline.class);
  public CaptureRewriter rewriter = new CaptureRewriter();
  public ResponseRewriterRegistry rewriterRegistry
      = new DefaultResponseRewriterRegistry(Arrays.<ResponseRewriter>asList(rewriter), null);
  public final HttpServletRequest request = mock(HttpServletRequest.class);
  public final HttpServletResponse response = mock(HttpServletResponse.class);
  public final HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(response);
  public final LockedDomainService lockedDomainService = mock(LockedDomainService.class);
  public final Processor processor = mock(Processor.class);
  public final Provider<FeedProcessor> feedProcessorProvider = new Provider<FeedProcessor>() {
    public FeedProcessor get() {
      return new FeedProcessorImpl();
    }
  };
}
