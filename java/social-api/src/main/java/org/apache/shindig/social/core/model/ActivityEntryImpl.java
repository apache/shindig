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

package org.apache.shindig.social.core.model;

import java.util.List;

import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.ActivityObject;

public class ActivityEntryImpl implements ActivityEntry {

	private String id;
	private String streamFavicon;
	private String postedTime;
	private ActivityObject actor;
	private List<String> verb;
	private ActivityObject object;
	private ActivityObject target;
	private String permalink;
	private String title;
	private String body;
	
	public ActivityEntryImpl() {
		this.id = null;
		this.streamFavicon = null;
		this.postedTime = null;
		this.actor = null;
		this.verb = null;
		this.object = null;
		this.target = null;
		this.permalink = null;
		this.title = null;
		this.body = null;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setStreamFavicon(String streamFavicon) {
		this.streamFavicon = streamFavicon;
	}
	
	public String getStreamFavicon() {
		return streamFavicon;
	}
	
	public void setPostedTime(String postedTime) {
		this.postedTime = postedTime;
	}
	
	public String getPostedTime() {
		return postedTime;
	}
	
	public void setActor(ActivityObject actor) {
		this.actor = actor;
	}
	
	public ActivityObject getActor() {
		return actor;
	}
	
	public void setVerb(List<String> verb) {
		this.verb = verb;
	}
	
	public List<String> getVerb() {
		return verb;
	}
	
	public void setObject(ActivityObject object) {
		this.object = object;
	}
	
	public ActivityObject getObject() {
		return object;
	}
	
	public void setTarget(ActivityObject target) {
		this.target = target;
	}
	
	public ActivityObject getTarget() {
		return target;
	}
	
	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}
	
	public String getPermalink() {
		return permalink;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
		
	public void setBody(String body) {
		this.body = body;
	}
	
	public String getBody() {
		return body;
	}
}
