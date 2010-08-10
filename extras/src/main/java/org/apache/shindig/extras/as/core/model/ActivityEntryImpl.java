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

package org.apache.shindig.extras.as.core.model;

import java.util.List;

import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;

public class ActivityEntryImpl implements ActivityEntry {

	private String icon;
	private String time;
	private ActivityObject actor;
	private List<String> verb;
	private ActivityObject object;
	private ActivityObject target;
	private ActivityObject generator;
	private ActivityObject serviceProvider;
	private String title;
	private String body;
	private List<String> standardLink;
	
	public ActivityEntryImpl() {
		this.icon = null;
		this.time = null;
		this.actor = null;
		this.verb = null;
		this.object = null;
		this.target = null;
		this.generator = null;
		this.serviceProvider = null;
		this.title = null;
		this.body = null;
		this.standardLink = null;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public ActivityObject getActor() {
		return actor;
	}

	public void setActor(ActivityObject actor) {
		this.actor = actor;
	}

	public List<String> getVerb() {
		return verb;
	}

	public void setVerb(List<String> verb) {
		this.verb = verb;
	}

	public ActivityObject getObject() {
		return object;
	}

	public void setObject(ActivityObject object) {
		this.object = object;
	}

	public ActivityObject getTarget() {
		return target;
	}

	public void setTarget(ActivityObject target) {
		this.target = target;
	}

	public ActivityObject getGenerator() {
		return generator;
	}

	public void setGenerator(ActivityObject generator) {
		this.generator = generator;
	}

	public ActivityObject getServiceProvider() {
		return serviceProvider;
	}

	public void setServiceProvider(ActivityObject serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<String> getStandardLink() {
		return standardLink;
	}

	public void setStandardLink(List<String> standardLink) {
		this.standardLink = standardLink;
	}
}
