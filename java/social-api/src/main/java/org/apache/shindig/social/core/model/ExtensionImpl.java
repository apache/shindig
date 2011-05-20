package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.EmbeddedExperience;
import org.apache.shindig.social.opensocial.model.Extension;

/**
 * Extension namespace implementation.
 */
public class ExtensionImpl implements Extension {
	
	//Embedded experience extension
	private EmbeddedExperience embed;
	
	/**
	 * Constructor
	 */
	public ExtensionImpl(){}
	
	/** {@inheritDoc} */
	public EmbeddedExperience getEmbed() {
		return embed;
	}

	/** {@inheritDoc} */
	public void setEmbed(EmbeddedExperience embed) {
		this.embed = embed;
	}

}
