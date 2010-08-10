package org.apache.shindig.extras.as.core.model;

import org.apache.shindig.extras.as.opensocial.model.MediaLink;

public class MediaLinkImpl implements MediaLink {
	
	private String target;
	private String type;
	private String width;
	private String height;
	private String duration;
	
	public MediaLinkImpl() {
		this.target = null;
		this.type = null;
		this.width = null;
		this.height = null;
		this.duration = null;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}
