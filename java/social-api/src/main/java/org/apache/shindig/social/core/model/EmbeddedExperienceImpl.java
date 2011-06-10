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
package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.EmbeddedExperience;

/**
 * Embedded experience implementation.
 *
 */
public class EmbeddedExperienceImpl implements EmbeddedExperience {
	
	private Object context;
	private String gadget;
	private String previewImage;
	private String url;
	
	/**
	 * Constructor
	 */
	public EmbeddedExperienceImpl(){}

	/** {@inheritDoc} */
	public Object getContext() {
		return context;
	}

	/** {@inheritDoc} */
	public String getGadget() {
		return gadget;
	}

	/** {@inheritDoc} */
	public String getPreviewImage() {
		return previewImage;
	}
	
	/** {@inheritDoc} */
	public String getUrl(){
		return url;
	}

	/** {@inheritDoc} */
	public void setContext(Object context) {
		this.context = context;
	}

	/** {@inheritDoc} */
	public void setGadget(String gadget) {
		this.gadget = gadget;
	}

	/** {@inheritDoc} */
	public void setPreviewImage(String previewImage) {
		this.previewImage = previewImage;
	}
	
	/** {@inheritDoc} */
	public void setUrl(String url){
		this.url = url;
	}

}
