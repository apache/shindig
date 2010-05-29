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
import org.apache.shindig.extras.as.opensocial.model.ActivityStream;

public class ActivityStreamImpl implements ActivityStream {

	private String displayName;
	private String language;
	private List<ActivityEntry> entries;
	private String id;
	private String subject;
	
	public ActivityStreamImpl() {
		this.displayName = null;
		this.language = null;
		this.entries = null;
		this.id = null;
		this.subject = null;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<ActivityEntry> getEntries() {
		return entries;
	}

	public String getId() {
		return id;
	}

	public String getLanguage() {
		return language;
	}

	public String getSubject() {
		return subject;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setEntries(List<ActivityEntry> entries) {
		this.entries = entries;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
}
