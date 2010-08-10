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

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.MediaLinkImpl;
import com.google.inject.ImplementedBy;

/*
 * TODO: comment this class.
 */
@ImplementedBy(MediaLinkImpl.class)
@Exportablebean
public interface MediaLink {

	/*
	 * Fields that represent the JSON elements.
	 */
	public static enum Field {
		TARGET("target"),
		TYPE("type"),
		WIDTH("width"),
		HEIGHT("height"),
		DURATION("duration");
		
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
	
	String getTarget();

	void setTarget(String target);

	String getType();

	void setType(String type);

	String getWidth();

	void setWidth(String width);

	String getHeight();

	void setHeight(String height);

	String getDuration();

	void setDuration(String duration);
}
