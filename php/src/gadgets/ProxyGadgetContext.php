<?php

class ProxyGadgetContext extends GadgetContext {

	public function __construct($url)
	{
		parent::__construct('GADGET');
		$this->url = $url;
	}
}
