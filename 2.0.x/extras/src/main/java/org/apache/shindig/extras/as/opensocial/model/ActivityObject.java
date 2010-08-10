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

package org.apache.shindig.extras.as.opensocial.model;

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.ActivityObjectImpl;

import com.google.inject.ImplementedBy;

@ImplementedBy(ActivityObjectImpl.class)
@Exportablebean
public interface ActivityObject {
	
	/*
	 * Fields that represent JSON elements for an activity entry.
	 */
	public static enum Field {
		ID("id"),
		NAME("name"),
		SUMMARY("summary"),
		MEDIA("media"),
		PERMALINK("permalink"),
		TYPE("type"),
		IN_REPLY_TO("inReplyTo"),
		ATTACHED("attached"),
		REPLY("reply"),
		REACTION("reaction"),
		ACTION("action"),
		UPSTREAM_DUPLICATE_ID("upstreamDuplicateId"),
		DOWNSTREAM_DUPLICATE_ID("downstreamDuplicateId"),
		STANDARD_LINK("standardLink");
		
		/*
		 * The name of the JSON element.
		 */
		private final String jsonString;
		
		/*
		 * Constructs the field base for the JSON element.
		 * 
		 * @param jsonString the name of the element
		 */
		private Field(String jsonString) {
			this.jsonString = jsonString;
		}
		
		/*
		 * Returns the name of the JSON element.
		 * 
		 * @return String the name of the JSON element
		 */
		public String toString() {
			return jsonString;
		}
	}
	
	String getId();

	void setId(String id);

	String getName();

	void setName(String name);

	String getSummary();

	void setSummary(String summary);

	MediaLink getMedia();

	void setMedia(MediaLink media);

	String getPermalink();

	void setPermalink(String permalink);

	List<String> getType();

	void setType(List<String> type);

	ActivityObject getInReplyTo();

	void setInReplyTo(ActivityObject inReplyTo);

	List<ActivityObject> getAttached();

	void setAttached(List<ActivityObject> attached);

	List<ActivityObject> getReply();

	void setReply(List<ActivityObject> reply);

	List<ActivityObject> getReaction();

	void setReaction(List<ActivityObject> reaction);

	ActionLink getAction();

	void setAction(ActionLink action);

	List<String> getUpstreamDuplicateId();

	void setUpstreamDuplicateId(List<String> upstreamDuplicateId);

	List<String> getDownstreamDuplicateId();

	void setDownstreamDuplicateId(List<String> downstreamDuplicateId);

	String getStandardLink();

	void setStandardLink(String standardLink);
}