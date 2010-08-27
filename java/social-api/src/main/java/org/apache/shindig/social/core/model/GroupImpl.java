package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.Group;
import org.apache.shindig.social.opensocial.spi.GroupId;

public class GroupImpl implements Group{

	private GroupId groupId;
	private String title;
	private String description;

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(GroupId groupId) {
		this.groupId = groupId;
	}
	public GroupId getId() {
		return groupId;
	}


}
