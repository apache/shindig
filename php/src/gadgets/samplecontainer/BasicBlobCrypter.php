<?php
/**
 * This class provides basic binary blob encryption and decryption, for use with the security token
 *
 */

//FIXME make this compatible with the java's blobcrypter
class BasicBlobCrypter extends BlobCrypter {
	
	/**
	 * {@inheritDoc}
	 */
	public function wrap($in)
	{
		if(is_array($in)) {
			$in = implode(":", $in);
		}
		return $in;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public function unwrap($in, $maxAgeSec)
	{
		$data = explode(":", $in);
		$rta = array();
		$rta['o'] = $data[0];
		$rta['a'] = $data[1];
		$rta['v'] = $data[2];
		$rta['d'] = $data[3];
		$rta['u'] = $data[4];
		$rta['m'] = $data[5];
		return $rta;
	}
}
