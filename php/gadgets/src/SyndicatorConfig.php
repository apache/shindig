<?

class SyndicatorConfig {
	public $default_syndicator = 'default';
	public $syndicator_key = 'gadgets.syndicator';
	private $config = array();

	public function __construct($defaultSyndicator)
	{
		if (! empty($defaultSyndicator)) {
			$this->loadSyndicators($defaultSyndicator);
		}
	}

	private function loadSyndicators($syndicators)
	{
		if (! file_exists($syndicators) || ! is_dir($syndicators)) {
			throw new Exception("Invalid syndicator path");
		}
		foreach (glob("$syndicators/*") as $file) {
			if (! is_readable($file)) {
				throw new Exception("Could not read syndicator config: $file");
			}
			if (is_dir($file)) {
				// support recursive loading of sub directories
				$this->loadSyndicators($file);
			} else {
				$this->loadFromFile($file);
			}
		}
	}

	private function loadFromFile($file)
	{
		$contents = file_get_contents($file);
		// remove all comments (both /* */ and // style) because this confuses the json parser
		// note: the json parser also crashes on trailing ,'s in records so please don't use them
		$contents = preg_replace('/\/\/.*$/m', '', preg_replace('@/\\*(?:.|[\\n\\r])*?\\*/@', '', $contents));
		$config = json_decode($contents, true);
		if (! isset($config[$this->syndicator_key][0])) {
			throw new Exception("No gadgets.syndicator value set");
		}
		$syndicator = $config[$this->syndicator_key][0];
		$this->config[$syndicator] = array();
		foreach ($config as $key => $val) {
			$this->config[$syndicator][$key] = $val;
		}
	}

	public function getConfig($syndicator, $name)
	{
		$config = array();
		if (isset($this->config[$syndicator]) && isset($this->config[$syndicator][$name])) {
			$config = $this->config[$syndicator][$name];
		}
		if ($syndicator != $this->default_syndicator && isset($this->config[$syndicator][$name])) {
			$config = $this->mergeConfig($this->config[$syndicator][$name], $config);
		}
		return $config;
	}

	// Code sniplet borrowed from: http://nl.php.net/manual/en/function.array-merge-recursive.php#81409
	// default array merge recursive doesn't overwrite values, but creates multiple elementents for that key,
	// which is not what we want here, we want array_merge like behavior
	private function mergeConfig() // $array1, $array2, etc
	{
		$arrays = func_get_args();
		$narrays = count($arrays);
		for($i = 0; $i < $narrays; $i ++) {
			if (! is_array($arrays[$i])) {
				trigger_error('Argument #' . ($i + 1) . ' is not an array - trying to merge array with scalar! Returning null!', E_USER_WARNING);
				return;
			}
		}
		$ret = $arrays[0];
		for($i = 1; $i < $narrays; $i ++) {
			foreach ($arrays[$i] as $key => $value) {
				if (((string) $key) === ((string) intval($key))) { // integer or string as integer key - append
					$ret[] = $value;
				} else {
					if (is_array($value) && isset($ret[$key])) {
						$ret[$key] = array_merge_recursive2($ret[$key], $value);
					} else {
						$ret[$key] = $value;
					}
				}
			}
		}
		return $ret;
	}
}
