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

import org.apache.shindig.common.servlet.UserAgent;

import com.google.inject.Provider;

public class BrowserSpecificRpcJsFeatureLoaderTest extends JsFeatureLoaderTest {
  private Provider<UserAgent> uaProvider = new TestUaProvider();
  
  @Override
  protected JsFeatureLoader makeJsFeatureLoader() {
    return new BrowserSpecificRpcJsFeatureLoader(fetcher, uaProvider);
  }
  
  // All JsFeatureLoaderTests should continue to work. Add some
  // additional tests for rpc.
  
  private static class TestUaProvider implements Provider<UserAgent> {
    private UserAgent entry;

    private void setNextToProvide(UserAgent entry) {
      this.entry = entry;
    }
    
    public UserAgent get() {
      return entry;
    }
  }
}