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
package org.apache.shindig.social.opensocial.spi;

import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import java.util.Map;

import org.junit.Before;
import org.junit.Assert;
import org.junit.Test;

public class CollectionOptionsTest extends Assert {

	private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

	protected SocialRequestItem request;
	protected BeanJsonConverter converter;

	@Before
	public void setUp() throws Exception {
		FAKE_TOKEN.setAppId("12345");
		FAKE_TOKEN.setOwnerId("someowner");
		FAKE_TOKEN.setViewerId("someowner");
		converter = new BeanJsonConverter(Guice.createInjector());
		Map<String, String[]> optionalParameters = Maps.newHashMap();
		String[] cvalue1 = {"cvalue1"};
		String[] cvalue2 = {"cvalue2"};
		optionalParameters.put("condition1",cvalue1);
		optionalParameters.put("condition2",cvalue2);
		request = new SocialRequestItem(
				optionalParameters,
				FAKE_TOKEN, converter, converter);
	}

	@Test
	public void testOptionalParams() throws Exception {
		CollectionOptions op = new CollectionOptions(request);
		Map<String, String> optionalParams = op.getOptionalParameter();
		Map<String, String> optionalParams1 = Maps.newHashMap();
		optionalParams1.put("condition1","cvalue1");
		optionalParams1.put("condition2","cvalue2");
		assertEquals(optionalParams, optionalParams1);
	}
}
