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

import org.apache.shindig.extras.as.opensocial.model.ActivityObject;

public class ActivityObjectImpl implements ActivityObject {
	
	private String id;
	private String displayName;
	private String summary;
	private String permalink;
	private String image;
	private List<String> objectType;
	private String content;
	private String audioStream;
	private String videoStream;
	private String playerApplet;
	private String bookmarkTarget;
	private String thumbnail;
	private String subject;
	private String description;
	private String rating;
	private String icon;
	private ActivityObject inReplyTo;
	
	public ActivityObjectImpl() {
		this.id = null;
		this.displayName = null;
		this.summary = null;
		this.permalink = null;
		this.image = null;
		this.content = null;
		this.audioStream = null;
		this.videoStream = null;
		this.playerApplet = null;
		this.bookmarkTarget = null;
		this.thumbnail = null;
		this.subject = null;
		this.description = null;
		this.rating = null;
		this.icon = null;
		this.objectType = null;
		this.inReplyTo = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getPermalink() {
		return permalink;
	}

	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAudioStream() {
		return audioStream;
	}

	public void setAudioStream(String audioStream) {
		this.audioStream = audioStream;
	}

	public String getVideoStream() {
		return videoStream;
	}

	public void setVideoStream(String videoStream) {
		this.videoStream = videoStream;
	}

	public String getPlayerApplet() {
		return playerApplet;
	}

	public void setPlayerApplet(String playerApplet) {
		this.playerApplet = playerApplet;
	}

	public String getBookmarkTarget() {
		return bookmarkTarget;
	}

	public void setBookmarkTarget(String bookmarkTarget) {
		this.bookmarkTarget = bookmarkTarget;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public List<String> getObjectType() {
		return objectType;
	}

	public void setObjectType(List<String> objectType) {
		this.objectType = objectType;
	}

	public ActivityObject getInReplyTo() {
		return inReplyTo;
	}

	public void setInReplyTo(ActivityObject inReplyTo) {
		this.inReplyTo = inReplyTo;
	}
}
