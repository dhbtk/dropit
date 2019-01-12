DropIt
======

DropIt is a tool for easily copying files and text between your computer and your phone. It does this using your local 
network, without having to create any accounts on cloud providers or sending your data anywhere. DropIt is 
multi-platform, working on Windows, Linux and macOS, and the phone app is currently available for Android.

From your phone, you can:

* Send files and text to your computer in one click using the Android "Share" button
* Easily send text to the computer clipboard through a home screen widget (TODO!)
* One-click camera: take a picture and instantly send it to your computer (TODO!)

From your computer, you can:

* Send files and text to your phone, either with a global keyboard shortcut or via drag-and-drop
* View the file transfer history, both sent and received (TODO!)
* View the text transfer history, both sent and received (TODO!)

## Installation

There are no binary builds available at this time.

To build the project from source, use Gradle:

```
$ ./gradlew assemble -Ddesktop.platform=<win32|osx|linux>
```

An APK will be generated at `mobile/build/outputs/apk/debug/mobile-debug.apk`, and a JAR for the desktop application
will be generated at `desktop/build/libs/dropit-desktop-0.1-<win32|osx|linux>.jar`.

## Caveats

Since the data is transferred over LAN, your computer and phone must be on the same network. This should not be a
problem if you use it in your home, but there might be some issues with corporate networks. This also means that the app
will not work over 3G/4G at all.

## Screenshots

<p align="center">
![image](https://user-images.githubusercontent.com/92917/51067549-71659200-15fa-11e9-8780-b4e19b4b5625.png)

![image](https://user-images.githubusercontent.com/92917/51067576-a70a7b00-15fa-11e9-942d-d09cce07e369.png)
</p>