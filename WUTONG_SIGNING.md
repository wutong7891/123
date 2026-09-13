# wutong signing and KMI identity

This tree is bound to the Android package `com.jinfuwei.wutong` and the
certificate shipped separately as `wutong-cert.der`.

Certificate identity used by `kernel/Kbuild`:

- DER size: `0x0532` (1330 bytes)
- SHA-256: `5eb97c555a9f0060a84a92e0d75f92bc6d18308070d7575d915842f827f5b15e`
- Manager package: `com.jinfuwei.wutong`

To sign the Manager, copy `wutong-release.jks` outside the repository and add
the following properties to `manager/gradle.properties`. Read the actual
passwords from the separately delivered `credentials.txt` file.

```properties
KEYSTORE_FILE=/absolute/path/to/wutong-release.jks
KEYSTORE_PASSWORD=<value from credentials.txt>
KEY_ALIAS=wutong-key
KEY_PASSWORD=<value from credentials.txt>
```

Build the Manager with `manager/gradlew assembleRelease`. Every KMI module
built from this source accepts only an APK whose package name and signing
certificate match the values above. Keep the JKS and both passwords private;
losing them requires rebuilding and reflashing every KMI module with a new
certificate identity.
