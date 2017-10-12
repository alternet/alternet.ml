# Authentication Formats

 * Description of popular crypt formats
 * Design considerations

## Few words about the design

For checking a password, most tools will get all the expected parameters, then compute a formatted crypt string, and compare
it with the one stored in the database. Alternet Security is designed with a neat separation of concepts in mind : algorithms and
their parameters, charsets, encodings, formattings, and crypt format families. It gives the advantage that you can combine them
at will, and you'll have the same base code when, for example, you want to encode some bytes in a specific format. Everything work in
the same way. Moreover, for checking a password, the parameters are extracted from the crypt stored in the database, the binary
hash is isolated, and it is the value that will be compared to the password after hashing it, without formatting it.
It sounds more natural to me. Conversely, most passwords checker libraries (not only in Java), will compare the
result hash after encoding them : with an hexadecimal encoding, you certainly have to compare regardless the case,
but with a base64 encoding, the case matters. This is why this framework only compare bytes.

Most of the description of the popular hash formats was largely inspired from the
outstanding [Passlib library for Python](http://passlib.readthedocs.io/en/stable/index.html).

## Crypt formats

```
Digest (md5) 3 colon delimited field, 32 character hash           admin:The Realm:11fbe079ed3476f7712030d24042ca35
SHA-1        {SHA} magic in hash, 33 characters total             admin:{SHA}QvQHx34cyGz2cjXj6cauQoAwtIg=
Crypt        (no magic) - 11 character hash                       admin:$cnhJ7swqUWTc
Apache MD5   $apr1$ magic in hash<br />(Not supported by Foswiki) admin:$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1
crypt-MD5    $1$ magic in hash, 34 characters total               admin:$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.
```

## Format : `$[scheme]$[salt]$[crypt]`

|ID|Method       |
|--|-------------|
|1 |MD5 with salt|
|2a|Blowfish-----|
|5 |SHA-256------|
|6 |SHA-512------|

### Examples

 * Apache MD5 crypt format : `$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1`
 * Crypt MD5 : `$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.`


 * Contains the password encoded to base64 (just like {SSHA}) : `{SSHA.b64}986H5cS9JcDYQeJd6wKaITMho4M9CrXM`
 * Contains the password encoded to hexa : `{SSHA.HEX}3f5ca6203f8cdaa44d9160575c1ee1d77abcf59ca5f852d1`


 * SSHA : Salted SHA

Base64 encoded hash with salt
userPassword: `{SSHA}MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0`

```
Base64 decoded value
     SHA1 Hash      Salt
--------------------++++
123456789012345678901234
```

http://tools.ietf.org/id/draft-stroeder-hashed-userpassword-values-01.html

```
SHA-1        {SHA} magic in hash, 33 characters total             admin:{SHA}QvQHx34cyGz2cjXj6cauQoAwtIg=
Crypt        (no magic) - 11 character hash                       admin:$cnhJ7swqUWTc
Apache MD5   $apr1$ magic in hash<br />(Not supported by Foswiki) admin:$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1
crypt-MD5    $1$ magic in hash, 34 characters total               admin:$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.
```

```
root:$1$hdhxObPx$TYFuTKsB9GGIgo53rF4bi1:0:0:root:/:/bin/sh
```

Crypt based password hashes have several parts separated by $

 * The hash type, `1` in your case, this stands for MD5-crypt (this is not plain MD5)
 * The salt, `hdhxObPx` in your case
 * The actual hash `TYFuTKsB9GGIgo53rF4bi1` in your case
 * Some schemes have additional parameters, such as a work-factor, but this does not apply to the scheme used in your example.
 * The MD5-Crypt scheme should be avoided, in favor of modern schemes, such as bcrypt (usually starting with `$2a$`). Not because MD5 is cryptographically broken, but because it has a constant work-factor, that's too small for the computational power modern attackers can field

#### Crypt

Encrypts a password in a crypt(3) compatible way.
The exact algorithm depends on the format of the salt string:

 * SHA-512 salts start with `$6$` and are up to 16 chars long.
 * SHA-256 salts start with `$5$` and are up to 16 chars long
 * MD5 salts start with `$1$` and are up to 8 chars long
 * DES, the traditional UnixCrypt algorithm is used with only 2 chars. Only the first 8 chars of the passwords are used in the DES algorithm !
 * The magic strings `$apr1$` and `$2a$` are not recognized by this method as its output should be identical with that of the libc implementation.
 * The rest of the salt string is drawn from the set `[a-zA-Z0-9./]` and is cut at the maximum length of if a `$` sign is encountered. It is therefore valid to enter a complete hash value as salt to e.g. verify a password with: `storedPwd.equals(crypt(enteredPwd, storedPwd))`

The resulting string starts with the marker string (`$6$`), continues with the salt value and ends with a `$` sign followed by the actual hash value.

For DES the string only contains the salt and actual hash. It's total length is dependent on the algorithm used:

 * SHA-512: 106 chars
 * SHA-256: 63 chars
 * MD5: 34 chars
 * DES: 13 chars

##### Example:

```
      crypt("secret", "$1$xxxx") => "$1$xxxx$aMkevjfEIpa35Bh3G4bAc."
      crypt("secret", "xx") => "xxWAum7tHdIUw"
```

## Hashers

### Families

3 families of hasher are supported :

 * WorkFactorHasher (e.g. PBKDF2, Bcrypt, Scrypt) : salt + iteration (e.g. PBKDF2WithHmacSHA1)
 * SaltedHasher (e.g. SSHA, MD5Crypt, UNIX CRYPT)
 * SimpleHasher : w/o salt and iteration (e.g. MD5Plain)

### MD5 Variants

 * MD5Crypt (`$1$` and `$apr1$`)
 * MD5Plain (MD5 w/o salt)

### BCrypt

Isolation of the scheme writing/parsing from the production of the crypt.

## CryptFormat

We just need to parse the scheme :

 * `scheme + ssp`
 * `ssp` depends on the Hasher
 * The Hasher is built from the scheme
 * Some `ssp` must know whether there is a salt or an iterator in order to call the suitable hasher family
 * The Hasher can configure itself (e.g. BCrypt always use the base64 encoding and supply its own implementation


 * ModularCryptFormat must not process "_" or "" : we need a unix crypt format for "" (I don't know what for "_") that may be passed as the very last CryptFormat


 * Crypt formats must be able to check the data size (SALT) in order to guess the rights parameters.

http://forum.insidepro.com/viewtopic.php?t=8225

# Binary

Many base64 encodings are used across crypt formats :

|Name|Value space|
|----|-----------|
|BASE64_CHARS|'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'|
|HASH64_CHARS|'./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'|
|BCRYPT_CHARS|'./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'|

Sometimes, hashers are setting specific options :

* DES crypt : use a big-endian encoding ; this is rather taken in charge when producing the bytes
* custom pbkdf2 hashes : BASE64_CHARS , except that it uses '.' instead of '+', and omits trailing padding '=' and whitespace.

