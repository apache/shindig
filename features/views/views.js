/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview This library provides functions for navigating to and dealing
 *     with views of the current gadget.
 */

var gadgets = gadgets || {};

/**
 * @static
 * @class Provides operations for dealing with Views.
 * @name gadgets.views
 */
gadgets.views = gadgets.views || {};

/**
 * Attempts to navigate to this gadget in a different view. If the container
 * supports parameters will pass the optional parameters along to the gadget in
 * the new view.
 *
 * @param {gadgets.views.View} view The view to navigate to
 * @param {Map.&lt;String, String&gt;} opt_params Parameters to pass to the
 *     gadget after it has been navigated to on the surface
 *
 * @member gadgets.views
 */
gadgets.views.requestNavigateTo = function(surface, opt_params) {
  return opensocial.Container.get().requestNavigateTo(surface, opt_params);
};

/**
 * Returns the current view.
 *
 * @return {gadgets.views.View} The current view
 * @member gadgets.views
 */
gadgets.views.getCurrentView = function() {
  return this.surface;
};

/**
 * Returns a map of all the supported views. Keys each gadgets.view.View by
 * its name.
 *
 * @return {Map&lt;gadgets.views.ViewType | String, gadgets.views.View&gt;} All
 *   supported views, keyed by their name attribute.
 * @member gadgets.views
 */
gadgets.views.getSupportedViews = function() {
  return this.supportedSurfaces;
};

/**
 * Returns the parameters passed into this gadget for this view. Does not
 * include all url parameters, only the ones passed into
 * gadgets.views.requestNavigateTo
 *
 * @return {Map.&lt;String, String&gt;} The parameter map
 * @member gadgets.views
 */
gadgets.views.getParams = function() {
  return this.params;
};


/**
 * @class Base interface for all view objects.
 * @name gadgets.views.View
 */

/**
 * @private
 * @constructor
 */
gadgets.views.View = function(name, opt_isOnlyVisibleGadgetValue) {
  this.name = name;
  this.isOnlyVisibleGadgetValue = !!opt_isOnlyVisibleGadgetValue;
};

/**
 * Returns the name of this view.
 *
 * @return {gadgets.views.ViewType | String} The view name, usually specified as
 * a gadgets.views.ViewType
 */
gadgets.views.View.prototype.getName = function() {
  return this.name;
};

/**
 * Returns true if the gadget is the only visible gadget in this view.
 * On a canvas page or in maximize mode this is most likely true; on a profile
 * page or in dashboard mode, it is most likely false.
 *
 * @return {boolean} True if the gadget is the only visible gadget; otherwise, false
 */
gadgets.views.View.prototype.isOnlyVisibleGadget = function() {
  return this.isOnlyVisibleGadgetValue;
};


/**
 * @static
 * @class
 * Used by <a href="gadgets.views.View.html"> View</a>s.
 * @name gadgets.views.ViewType
 */
gadgets.views.ViewType = {
 /**
  * A view where the gadget is displayed in a very large mode. It should be the
  * only thing on the page. In a social context, this is usually called the
  * canvas page.
  *
  * @member gadgets.views.ViewType
  */
  FULL_PAGE : 'FULL_PAGE',

 /**
  * A view where the gadget is displayed in a small area usually on a page with
  * other gadgets. In a social context, this is usually called the profile page.
  *
  * @member gadgets.views.ViewType
  */
  DASHBOARD : 'DASHBOARD',

 /**
  * A view where the gadget is displayed in a small separate window by itself.
  *
  * @member gadgets.views.ViewType
  */
  POPUP : 'POPUP'
};
