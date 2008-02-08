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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetView;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Handles processing a single gadget
 */
public class JsonRpcGadgetJob implements Callable<Gadget> {
  private final GadgetServer gadgetServer;
  private final JsonRpcContext context;
  private final JsonRpcGadget gadget;

  /**
   * {@inheritDoc}
   *
   *  @throws RpcException
   */
  public Gadget call() throws RpcException {
    GadgetView.ID gadgetId;
    try {
      gadgetId = new Gadget.GadgetId(new URI(gadget.getUrl()),
                                     gadget.getModuleId());
      return gadgetServer.processGadget(gadgetId,
                                        new UserPrefs(gadget.getUserPrefs()),
                                        new Locale(context.getLanguage(),
                                                   context.getCountry()),
                                        RenderingContext.CONTAINER,
                                        new JsonRpcProcessingOptions(context));
    } catch (URISyntaxException e) {
      throw new RpcException("Bad url");
    } catch (GadgetServer.GadgetProcessException e) {
      throw new RpcException("Failed to process gadget.", e);
    }
  }

  public JsonRpcGadgetJob(GadgetServer gadgetServer,
                          JsonRpcContext context,
                          JsonRpcGadget gadget) {
    this.gadgetServer = gadgetServer;
    this.context = context;
    this.gadget = gadget;
  }
}
