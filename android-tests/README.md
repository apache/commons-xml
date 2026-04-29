<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Android instrumented tests

Runs the attack-test suite from `../src/test/java` against the Android runtime, exercising
the harmony based DOM and SAX factories that ship with Android. The Maven build does not include this
module; it is a standalone Gradle build kept separate so the default `mvn` goal stays JVM only.

## Prerequisites

- JDK 17 on `PATH` (AGP 8.x requires it).
- Android SDK with `platforms/android-34` and `build-tools/34.0.0` installed;
  export `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to point at it.
- Either an attached emulator/device (`adb devices` shows it) or the AGP managed device
  bundled into this build (`api31`, AOSP system image).
- The library JAR built by the parent Maven build:

  ```
  cd .. && mvn -DskipTests package
  ```

## Running

Against an attached emulator/device:

```
./gradlew connectedAndroidTest
```

Against the bundled AGP managed device (downloads the AOSP API 31 system image on first
run, then provisions and tears down a headless emulator for each invocation):

```
./gradlew api31DebugAndroidTest
```

## Excluded test groups

The build excludes JUnit 5 tags for JAXP types Android does not ship:

- `stax`: there is no `XMLInputFactory` on Android.
- `schema`: there is no `SchemaFactory` on Android.
- `xpath3`: relies on Saxon, which is not on the Android classpath.

DOM, SAX, TrAX and XPath 1.0 paths are exercised in full.
