This plugin addresses problems with BouncyCastle bcprov. The BouncyCastleProvider
(a Java security provider) needs to be registered early on in the startup of KNIME,
so that no other BouncyCastle duplicates can get in the way and register themselves.

