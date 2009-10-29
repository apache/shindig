/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @class
 * Tame and expose gadgets.Tabs and gadgets.TabSet API to cajoled gadgets
 */

var tamings___ = tamings___ || [];
tamings___.push(function(imports) {
  caja___.whitelistMeths([
    [gadgets.Tab, 'getCallback'],
    [gadgets.Tab, 'getContentContainer'],
    [gadgets.Tab, 'getIndex'],
    [gadgets.Tab, 'getName'],
    [gadgets.Tab, 'getNameContainer'],
  
    [gadgets.TabSet, 'addTab'],
    [gadgets.TabSet, 'alignTabs'],
    [gadgets.TabSet, 'displayTabs'],
    [gadgets.TabSet, 'getHeaderContainer'],
    [gadgets.TabSet, 'getSelectedTab'],
    [gadgets.TabSet, 'getTabs'],
    [gadgets.TabSet, 'removeTab'],
    [gadgets.TabSet, 'setSelectedTab'],
    [gadgets.TabSet, 'swapTabs']
  ]);
  caja___.whitelistCtors([
    [gadgets, 'TabSet'],
  ]);
});
