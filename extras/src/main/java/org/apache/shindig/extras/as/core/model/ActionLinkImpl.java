package org.apache.shindig.extras.as.core.model;

import org.apache.shindig.extras.as.opensocial.model.ActionLink;

public class ActionLinkImpl implements ActionLink {

	private String target;
	private String caption;
	
	public ActionLinkImpl() {
		this.target = null;
		this.caption = null;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
}
