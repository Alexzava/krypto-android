# Krypto for Android

Krypto for Android is a simple and secure app to protect your private documents with strong encryption.

Windows and Linux versions are coming soon.

![Description here](images/screenshot.png)

## Key Features
- **XChaCha20-Poly1305** algorithm for symmetric encryption ([Read more](https://doc.libsodium.org/secret-key_cryptography/secretstream))
- **Argon2ID** as password key derivation ([Read more](https://doc.libsodium.org/password_hashing/default_phf))
- **X25519** as key exchange ([Read more](https://doc.libsodium.org/key_exchange))
- Designed to be fully compatible with [Hat.sh](https://hat.sh/)

All cryptographic algorithms are from the [libsodium](https://doc.libsodium.org/) library.


## Download/Installation

[Download here](https://github.com/Alexzava/krypto-android/releases)

## Open source resources

* [FilePicker](https://github.com/ChochaNaresh/FilePicker)
* [Lazysodium Android](https://github.com/terl/lazysodium-android)
* [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded)
* [QRCode-Kotlin](https://github.com/g0dkar/qrcode-kotlin)

## License

Krypto for Android is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```text
MIT License

Copyright (c) 2024 Alex Zava

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```