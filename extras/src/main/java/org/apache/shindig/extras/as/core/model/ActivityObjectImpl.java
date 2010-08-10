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

import org.apache.shindig.extras.as.opensocial.model.ActionLink;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;
import org.apache.shindig.extras.as.opensocial.model.MediaLink;

public class ActivityObjectImpl implements ActivityObject {
	
	private String id;
	private String name;
	private String summary;
	private MediaLink media;
	private String permalink;
	private List<String> type;
	private ActivityObject inReplyTo;
	private List<ActivityObject> attached;
	private List<ActivityObject> reply;
	private List<ActivityObject> reaction;
	private ActionLink action;
	private List<String> upstreamDuplicateId;
	private List<String> downstreamDuplicateId;
	private String standardLink;
	
	public ActivityObjectImpl() {
		this.id = null;
		this.name = null;
		this.summary = null;
		this.media = null;
		this.permalink = null;
		this.type = null;
		this.inReplyTo = null;
		this.attached = null;
		this.reply = null;
		this.reaction = null;
		this.action = null;
		this.upstreamDuplicateId = null;
		this.downstreamDuplicateId = null;
		this.standardLink = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public MediaLink getMedia() {
		return media;
	}

	public void setMedia(MediaLink media) {
		this.media = media;
	}

	public String getPermalink() {
		return permalink;
	}

	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}

	public List<String> getType() {
		return type;
	}

	public void setType(List<String> type) {
		this.type = type;
	}

	public ActivityObject getInReplyTo() {
		return inReplyTo;
	}

	public void setInReplyTo(ActivityObject inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	public List<ActivityObject> getAttached() {
		return attached;
	}

	public void setAttached(List<ActivityObject> attached) {
		this.attached = attached;
	}

	public List<ActivityObject> getReply() {
		return reply;
	}

	public void setReply(List<ActivityObject> reply) {
		this.reply = reply;
	}

	public List<ActivityObject> getReaction() {
		return reaction;
	}

	public void setReaction(List<ActivityObject> reaction) {
		this.reaction = reaction;
	}

	public ActionLink getAction() {
		return action;
	}

	public void setAction(ActionLink action) {
		this.action = action;
	}

	public List<String> getUpstreamDuplicateId() {
		return upstreamDuplicateId;
	}

	public void setUpstreamDuplicateId(List<String> upstreamDuplicateId) {
		this.upstreamDuplicateId = upstreamDuplicateId;
	}

	public List<String> getDownstreamDuplicateId() {
		return downstreamDuplicateId;
	}

	public void setDownstreamDuplicateId(List<String> downstreamDuplicateId) {
		this.downstreamDuplicateId = downstreamDuplicateId;
	}

	public String getStandardLink() {
		return standardLink;
	}

	public void setStandardLink(String standardLink) {
		this.standardLink = standardLink;
	}
}
