# MulticastCompat
An support library for Multicast Network Discovery.

Currently this library is only intended for network service discovery and quering such.
Registering services is not supported.

This library is intended for 2 cases:

1. It supports mDNS TXT data on API < 21, which NsdManager does not.
1. It supports mDNS queries even when the systems multicast daemon is not running.

**This library is still under development and should not be used in production.**

### Installation
Add `maven { url "https://jitpack.io" }` in your root build.gradle

```sh
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Then add the library by including it in one of your dependencies

```sh
dependencies {
    implementation('com.github.programminghoch10:MulticastCompat:main-SNAPSHOT') {
       changing=true
    }
}
```
