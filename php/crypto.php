<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
?><?php

require_once 'RSA.class.php';

//TODO docs throughout
class CryptoHelper {

    public static function encrypt($key, $modulo, $data) {
        $rsa = new RSA();
        return $rsa->encrypt(serialize($data), $key, $modulo);
    }

    public static function decrypt($key, $modulo, $data) {
        $rsa = new RSA();
        return unserialize($rsa->decrypt($data, $key, $modulo));
    }

    // get modulo for key pair, generate key pair if it doesn't exist
    public static function appModulo() { //TODO this is a pretty bad name
        return self::readKey('modulo');
    }

    // get private key, generate key pair if it doesn't exist
    public static function appPrivateKey() {
        return self::readKey('private');
    }

    // get public key, generate key pair if it doesn't exist
    public static function appPublicKey() {
        return self::readKey('public');
    }

    // ------------------------------------------------------------------

    // a "poor man's" atomic operation
    private static function generateKeys() {
        $n = mt_rand();
        $rsa = new RSA();
        list($modulo, $publicKey, $privateKey) = $rsa->generate_keys('1002074641' /* XXX self::randomPrime() */, '1002073529' /* XXX self::randomPrime() */);
        // app creates keys in temp file
        if ((! file_put_contents(self::keyPath('modulo-' . $n), $modulo))
                || (! file_put_contents(self::keyPath('public-' . $n), $publicKey))
                || (! file_put_contents(self::keyPath('private-' . $n), $privateKey))) {
            return false;
        }
        // app moves temp file to xn_private
        rename(self::keyPath('modulo-' . $n), self::keyPath('modulo'));
        rename(self::keyPath('public-' . $n), self::keyPath('public'));
        rename(self::keyPath('private-' . $n), self::keyPath('private'));
        sleep(1);
    }

    private static function readKey($type) {
        // app checks for key
        if (file_exists(self::keyPath($type))) {
            $k = file_get_contents(self::keyPath($type));
            if (! $k) {
                error_log('Could not read key from ' . self::keyPath($type));
            }
            return $k;
        }
        return (CryptoHelper::generateKeys() ? self::readKey($type) : null);
    }

    private static function randomPrime() {
        return '7'; //XXX :)
    }

    private static function keyPath($type) {
        return "./key-$type";
    }

}
