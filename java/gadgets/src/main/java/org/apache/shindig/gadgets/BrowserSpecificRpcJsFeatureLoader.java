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
package org.apache.shindig.gadgets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.common.servlet.UserAgent;
import org.apache.shindig.gadgets.http.HttpFetcher;

import java.util.Map;

public class BrowserSpecificRpcJsFeatureLoader extends JsFeatureLoader {
  private static final String RPC_FEATURE_NAME = "rpc";
  private Provider<UserAgent> uaProvider;
  private RpcJsLibrary rpcLib;
  
  @Inject
  BrowserSpecificRpcJsFeatureLoader(HttpFetcher fetcher, Provider<UserAgent> uaProvider) {
    super(fetcher);
    this.uaProvider = uaProvider;
  }
  
  @Override
  protected JsLibrary createJsLibrary(JsLibrary.Type type, String content, String feature,
      HttpFetcher fetcher) throws GadgetException {
    if (feature.equals(RPC_FEATURE_NAME)) {
      if (rpcLib == null) {
        synchronized(this) {
          if (rpcLib == null) {
            rpcLib = new RpcJsLibrary(uaProvider, type, content);
            return rpcLib;
          }
        }
      }
      return new NullJsLibrary(RPC_FEATURE_NAME);
    }
    return super.createJsLibrary(type, content, feature, fetcher);
  }
  
  /**
   * Do-nothing JsLibrary. Workaround for multi-file features
   * with overrides, ie. rpc. createJsLibrary is called for each
   * resource in the feature, but we want RpcJsLibrary to
   * issue all the JS for the request.
   */
  protected static class NullJsLibrary extends JsLibrary {
    public NullJsLibrary(String name) {
      super(name, Type.FILE, "", "");
    }
  }
  
  /**
   * Custom JsLibrary that emits only rpc.js transport code needed
   * for a given User-Agent, which it gets from an injected Provider.
   */
  protected static class RpcJsLibrary extends JsLibrary {
    private final Provider<UserAgent> uaProvider;
    private final Map<String, String> rpcJsCode;
    
    private static final String RPC_JS_CORE = "rpc.js";
    private static final String WPM_TX = "wpm.transport.js";
    private static final String NIX_TX = "nix.transport.js";
    private static final String FE_TX = "fe.transport.js";
    private static final String RMR_TX = "rmr.transport.js";
    private static final String IFPC_TX = "ifpc.transport.js";
    private static final String ALL_TX = "all-transports";
    
    private static final String OPTIMIZED_SUFFIX = ":opt";
    private static final String DEBUG_SUFFIX = ":dbg";
    
    protected RpcJsLibrary(Provider<UserAgent> uaProvider, Type type, String filePath) {
      super(RPC_FEATURE_NAME, type, "", "");
      this.uaProvider = uaProvider;
      
      // Something of a hack: filePath, without the trailing filename, gives the root
      // path from which to load the JS.
      int lastSlash = filePath.lastIndexOf('/');
      if (lastSlash >= 0) {
        filePath = filePath.substring(0, lastSlash + 1);
      }
      
      // Load the core and all transport JS.
      rpcJsCode = Maps.newHashMap();
      StringBuilder allDbg = new StringBuilder();
      StringBuilder allOpt = new StringBuilder();
      for (String rpcPart :
           ImmutableList.of(RPC_JS_CORE, WPM_TX, NIX_TX, FE_TX, RMR_TX, IFPC_TX)) {
        StringBuilder opt = new StringBuilder();
        StringBuilder dbg = new StringBuilder();
        loadOptimizedAndDebugData(filePath + rpcPart, type, opt, dbg);
        rpcJsCode.put(rpcPart + OPTIMIZED_SUFFIX, opt.toString());
        rpcJsCode.put(rpcPart + DEBUG_SUFFIX, dbg.toString());
        if (!rpcPart.equals(RPC_JS_CORE)) {
          allOpt.append(opt.toString());
          allDbg.append(dbg.toString());
        }
      }
      rpcJsCode.put(ALL_TX + OPTIMIZED_SUFFIX, allOpt.toString());
      rpcJsCode.put(ALL_TX + DEBUG_SUFFIX, allDbg.toString());
    }
    
    @Override
    public String getContent() {
      return getRpcContent(OPTIMIZED_SUFFIX);
    }
    
    @Override
    public String getDebugContent() {
      return getRpcContent(DEBUG_SUFFIX);
    }
    
    @Override
    public boolean isProxyCacheable() {
      return false;
    }
    
    /**
     * Does the dirty work of translating UserAgent into transport + rpc core.
     * @param keySuffix Suffix appended to rpc content key getting debug or opt JS.
     * @return Context-appropriate rpc.js
     */
    String getRpcContent(String keySuffix) {
      // Send all by default.
      String txKey = ALL_TX;
      UserAgent userAgent = uaProvider.get();
      if (userAgent != null) {
        double version = userAgent.getVersionNumber();
        switch(userAgent.getBrowser()) {
          case MSIE:
            if (version >= 8) {
              txKey = WPM_TX;
            } else if (version >= 6) {
              txKey = NIX_TX;
            }
            break;
          case FIREFOX:
            if (version >= 3) {
              txKey = WPM_TX;
            } else if (version >= 2) {
              txKey = FE_TX;
            }
            break;
          case SAFARI:
            if (version >= 4) {
              txKey = WPM_TX;
            } else if (version >= 2) {
              txKey = RMR_TX;
            }
            break;
          case CHROME:
            if (version >= 2) {
              txKey = WPM_TX;
            } else {
              txKey = RMR_TX;
            }
            break;
          case OPERA:
            if (version >= 9) {
              txKey = WPM_TX;
            }
            break;
          case WEBKIT:
            if (version >= 29907) {
              // Webkit nightlies have had window.postMessage since at least 2/1/2008.
              // TODO Figure out exactly when it was added.
              txKey = WPM_TX;
            } else {
              txKey = RMR_TX;
            }
            break;
          case OTHER:
            break;
        }
      }
      
      // Return the appropriate transport(s) + rpc core.
      return rpcJsCode.get(txKey + keySuffix) + rpcJsCode.get(RPC_JS_CORE + keySuffix);
    }
  }
}
