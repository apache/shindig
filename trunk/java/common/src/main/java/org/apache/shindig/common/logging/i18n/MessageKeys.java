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
package org.apache.shindig.common.logging.i18n;

/**
 * Unique [key, value] pairs for i18n logging messages.
 * The key is used as message key input in the log method and its value matched the key specified in the localized resource.properties file
 */
public interface MessageKeys {
	public static final String MESSAGES = "org.apache.shindig.common.logging.i18n.resource";

	//AuthenticationServletFilter
	public static final String ERROR_PARSING_SECURE_TOKEN = "errorParsingSecureToken";
	//XmlUtil
	public static final String ERROR_PARSING_XML="commonErrorParsingXML";
	public static final String ERROR_PARSING_EXTERNAL_GENERAL_ENTITIES="errorParsingExternalGeneralEntities";
	public static final String ERROR_PARSING_EXTERNAL_PARAMETER_ENTITIES="errorParsingExternalParameterEntities";
	public static final String ERROR_PARSING_EXTERNAL_DTD="errorParsingExternalDTD";
	public static final String ERROR_PARSING_SECURE_XML="errorNotUsingSecureXML";
	public static final String REUSE_DOC_BUILDERS="reuseDocumentBuilders";
	public static final String NOT_REUSE_DOC_BUILDERS="notReuseDocBuilders";
	//LruCacheProvier
	public static final String LRU_CAPACITY="LRUCapacity";
	//DynamicConfigProperty
	public static final String EVAL_EL_FAILED="evalExpressionFailed";
	//JsonContainerConfigLoader
	public static final String READING_CONFIG="readingContainerConfig";
	public static final String LOAD_FR_STRING="loadFromString";
	public static final String LOAD_RESOURCES_FROM="loadResourcesFrom";
	public static final String LOAD_FILES_FROM="loadFilesFrom";
	//ApiServlet
	public static final String API_SERVLET_PROTOCOL_EXCEPTION="apiServletProtocolException";
	public static final String API_SERVLET_EXCEPTION="apiServletException";
	//RegistryFeature
	public static final String OVERRIDING_FEATURE="overridingFeature";
	//XSDValidator
	public static final String RESOLVE_RESOURCE="resolveResource";
	public static final String FAILED_TO_VALIDATE="failedToValidate";


	//FeatureResourceLoader
	public static final String MISSING_FILE="missingFile";
	public static final String UNABLE_RETRIEVE_LIB="unableRetrieveLib";
	//BasicHttpFetcher
	public static final String TIMEOUT_EXCEPTION="timeoutException";
	public static final String EXCEPTION_OCCURRED="exceptionOccurred";
	public static final String SLOW_RESPONSE="slowResponse";
	//HttpResponseMetadataHelper
	public static final String ERROR_GETTING_MD5="errorGettingMD5";
	public static final String ERROR_PARSING_MD5="errorParsingMD5";
	//OAuthModule
	public static final String USING_RANDOM_KEY="usingRandomKey";
	public static final String USING_FILE="usingFile";
	public static final String LOAD_KEY_FILE_FROM="loadKeyFileFrom";
	public static final String COULD_NOT_LOAD_KEY_FILE="couldNotLoadKeyFile";
	public static final String COULD_NOT_LOAD_SIGN_KEY="couldNotLoadSignedKey";
	public static final String FAILED_TO_INIT="failedToInit";
	//OauthRequest
	public static final String OAUTH_FETCH_FATAL_ERROR="oauthFetchFatalError";
	public static final String BOGUS_EXPIRED="bogusExpired";
	public static final String OAUTH_FETCH_ERROR_REPROMPT="oauthFetchErrorReprompt";
	public static final String OAUTH_FETCH_UNEXPECTED_ERROR="oauthFetchUnexpectedError";
	public static final String UNAUTHENTICATED_OAUTH="unauthenticatedOauth";
	public static final String INVALID_OAUTH="invalidOauth";
	//CajaCssSanitizer
	public static final String FAILED_TO_PARSE="failedToParse";
	public static final String UNABLE_TO_CONVERT_SCRIPT="unableToConvertScript";
	//PipelineExecutor
	public static final String ERROR_PRELOADING="errorPreloading";
	//Processor
	public static final String RENDER_NON_WHITELISTED_GADGET="renderNonWhitelistedGadget";
	//CajaResponseRewriter
	public static final String FAILED_TO_RETRIEVE="failedToRetrieve";
	public static final String FAILED_TO_READ="failedToRead";
	//DefaultServiceFetcher
	public static final String HTTP_ERROR_FETCHING="httpErrorFetching";
	public static final String FAILED_TO_FETCH_SERVICE="failedToFetchService";
	public static final String FAILED_TO_PARSE_SERVICE="failedToParseService";
	//Renderer
	public static final String FAILED_TO_RENDER="FailedToRender";
	//RenderingGadgetRewriter
	public static final String UNKNOWN_FEATURES="unknownFeatures";
	public static final String UNEXPECTED_ERROR_PRELOADING="unexpectedErrorPreloading";
	//SanitizingResponseRewriter
	public static final String REQUEST_TO_SANITIZE_WITHOUT_CONTENT="requestToSanitizeWithoutContent";
	public static final String REQUEST_TO_SANITIZE_UNKNOW_CONTENT="requestToSanitizeUnknownContent";
	public static final String UNABLE_SANITIZE_UNKNOWN_IMG="unableToSanitizeUnknownImg";
	public static final String UNABLE_DETECT_IMG_TYPE="unableToDetectImgType";
	//BasicImageRewriter
	public static final String IO_ERROR_REWRITING_IMG="ioErrorRewritingImg";
	public static final String UNKNOWN_ERROR_REWRITING_IMG="unknownErrorRewritingImg";
	public static final String FAILED_TO_READ_IMG="failedToReadImg";
	//CssResponseRewriter
	public static final String CAJA_CSS_PARSE_FAILURE="cajaCssParseFailure";
	//ImageAttributeRewriter
	public static final String UNABLE_TO_PROCESS_IMG="unableToProcessImg";
	public static final String UNABLE_TO_READ_RESPONSE="unableToReadResponse";
	public static final String UNABLE_TO_FETCH_IMG="unableToFetchImg";
	public static final String UNABLE_TO_PARSE_IMG="unableToParseImg";
	//MutableContent
	public static final String EXCEPTION_PARSING_CONTENT="exceptionParsingContent";
	//PipelineDataGadgetRewriter
	public static final String FAILED_TO_PARSE_PRELOAD="failedToParsePreload";
	//ProxyingVisitor
	public static final String URI_EXCEPTION_PARSING="uriExceptionParsing";
	//TemplateRewriter
	public static final String 	MALFORMED_TEMPLATE_LIB="malformedTemplateLib";
	//CajaContentRewriter
	public static final String CAJOLED_CACHE_CREATED="cajoledCacheCreated";
	public static final String RETRIEVE_REFERENCE="retrieveReference";
	public static final String UNABLE_TO_CAJOLE="unableToCajole";
	//ConcatProxyServlet
	public static final String CONCAT_PROXY_REQUEST_FAILED="concatProxyRequestFailed";
	//GadgetRenderingServlet
	public static final String MALFORMED_TTL_VALUE="malformedTtlValue";
	//HttpRequestHandler
	public static final String GADGET_CREATION_ERROR="gadgetCreationError";
	//ProxyServlet
	public static final String EMBEDED_IMG_WRONG_DOMAIN="embededImgWrongDomain";
	//RpcServlet
	public static final String BAD_REQUEST_400="badRequest400";
	//DefaultTemplateProcessor
	public static final String EL_FAILURE="elFailure";
	//UriUtils
	public static final String SKIP_ILLEGAL_HEADER="skipIllegalHeader";
	//AbstractSpecFactory
	public static final String UPDATE_SPEC_FAILURE_USE_CACHE_VERSION="updateSpecFailureUseCacheVersion";
	public static final String UPDATE_SPEC_FAILURE_APPLY_NEG_CACHE="updateSpecFailureApplyNegCache";
	//HashLockedDomainService
	public static final String NO_LOCKED_DOMAIN_CONFIG="noLockedDomainConfig";
	//Bootstrap
	public static final String STARTING_CONN_MANAGER_WITH="startingConnManagerWith";
	//DefaultRequestPipeline
	public static final String CACHED_RESPONSE="cachedResponse";
	public static final String STALE_RESPONSE="staleResponse";

}
