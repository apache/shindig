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

/**
 * @fileoverview Constants used throughout the container classes for
 * embedded experiences.
 */

/**
 * Embedded experience namespace
 * @type {Object}
 */
osapi.container.ee = {};

/**
 * Rendering params for an embedded experience
 * @enum {string}
 */
osapi.container.ee.RenderParam = {
    GADGET_RENDER_PARAMS: 'gadgetRenderParams',
    GADGET_VIEW_PARAMS: 'gadgetViewParams',
    URL_RENDER_PARAMS: 'urlRenderParams',
    DATA_MODEL: 'eeDataModel',
    EMBEDDED: 'embedded'
};

/**
 * Parameters for EE Data Model.
 * @enum {string}
 */
osapi.container.ee.DataModel = {
    CONTEXT: 'context',
    GADGET: 'gadget',
    URL: 'url',
    PREVIEW_IMAGE: 'previewImage',
    PREFERRED_EXPERIENCE: 'preferredExperience'
};

/**
 * Parameters for EE data model preferredExperience section.
 * @enum {string}
 */
osapi.container.ee.PreferredExperience = {
    TARGET: 'target',
    DISPLAY: 'display',
    TYPE: 'type',
    VIEW: 'view',
    VIEW_TARGET: 'viewTarget'
};

/**
 * Parameters for EE data model context section.
 * @enum {string}
 */
osapi.container.ee.Context = {
    ASSOCIATED_CONTEXT: 'associatedContext',
    OPENSOCIAL: 'openSocial'
};

/**
 * Parameters for EE associated context.
 * @enum {string}
 */
osapi.container.ee.AssociatedContext = {
    ID: 'id',
    TYPE: 'type',
    OBJECT_REFERENCE: 'objectReference'
};

/**
 * Parameters for EE model preferred experience target type.
 * @enum {string}
 */
osapi.container.ee.TargetType = {
    GADGET: 'gadget',
    URL: 'url'
};

/**
 * Parameters for EE model preferred experience display type.
 * @enum {string}
 */
osapi.container.ee.DisplayType = {
    IMAGE: 'image',
    TEXT: 'text'
};

/**
 * Additional config parameter when container support EE.
 */
osapi.container.ee.ContainerConfig = {
    /**
     * Used by container to override logic to determine target type of the EE.
     *
     * The first argument will be the EE data model and the second one will be the optional
     * context from the container.
     * The function should return either gadget or url
     * @type {function}
     *
     * Example:
     *   <code>
     *     var config = {};
     *     config[osapi.container.ee.ContainerConfig.GET_EE_NAVIGATION_TYPE] =
              function(dataModel, opt_containerContext) {
     *          return "gadget";
     *        };
     *   </code>
     */
    GET_EE_NAVIGATION_TYPE: 'GET_EE_NAVIGATION_TYPE'
};
