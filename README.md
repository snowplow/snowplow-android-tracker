# Android Analytics for Snowplow

[![actively-maintained]][tracker-classificiation] [![Build Status][gh-actions-image]][gh-actions] [![Coverage Status][coveralls-image]][coveralls] [![Release][release-image]][releases] [![License][license-image]][license]

## Overview

Add analytics to your Java software with the **[Snowplow][snowplow]** event tracker for **[Android][android]**. See also: **[Snowplow Java Tracker][snowplow-java-tracker]**.

With this tracker you can collect event data from your Android applications, games or frameworks.

## Quickstart

### Building

Assuming git is installed, clone the project.

```bash
$ git clone https://github.com/snowplow/snowplow-android-tracker.git
```

Then open the project in Android Studio and finish the setup.

### Testing

1. Tests require a device, whether emulator-based or a real one.

2. We need to run [Snowplow Micro][micro] before running trackers' tests. Micro will provide an endpoint that our tests will interact with.

3. Assuming Micro is running on `localhost:9090`, we need to make this endpoint publicly available. An option could be using an ssh-based service, `serveo`. Feel free to use any other tool serving the same purpose, like `ngrok`. The following is an example for `serveo`.

```bash
ssh -R 80:localhost:9090 serveo.net
```

which should print something similar to

```bash
Forwarding HTTP traffic from https://micro.serveo.net
```

4. Copy the url without the scheme, `micro.serveo.net` in the example above, and insert a line to `local.properties` file as following:

```bash
microSubdomain=micro.serveo.net
```

5. Use Android Studio's capabilities to run tests or use `gradlew` CLI tool. e.g. At the root of the repository, run

```bash
./gradlew connectedCheck
```

## Find out more

| Technical Docs                 | Quick Start              | Roadmap                | Contributing                     |
|:-------------------------------|:-------------------------|:-----------------------|:---------------------------------|
| ![i1][techdocs-image]          | ![i2][quick-start-image]       | ![i3][roadmap-image]   | ![i4][contributing-image]        |
| **[Technical Docs][techdocs]** | **[Quick Start][setup]** | **[Roadmap][roadmap]** | **[Contributing](Contributing.md)** |

Older documentation can be found [here][techdocs-old].

## Copyright and license

The Snowplow Android Tracker is copyright 2015-2020 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[android]: https://www.android.com/

[snowplow]: https://snowplowanalytics.com
[snowplow-java-tracker]: https://github.com/snowplow/snowplow-java-tracker

[micro]: https://github.com/snowplow-incubator/snowplow-micro

[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[quick-start-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[techdocs]: https://docs.snowplowanalytics.com/docs/collecting-data/collecting-from-own-applications/android-tracker/android-1-4-0/
[techdocs-old]: https://github.com/snowplow/snowplow/wiki/Android-Tracker
[setup]: https://docs.snowplowanalytics.com/docs/collecting-data/collecting-from-own-applications/android-tracker/android-1-4-0/#quick-start
[roadmap]: https://github.com/snowplow/snowplow/wiki/Product-roadmap

[gh-actions]: https://github.com/snowplow/snowplow-android-tracker/actions
[gh-actions-image]: https://github.com/snowplow/snowplow-android-tracker/workflows/build/badge.svg?branch=master

[release-image]: https://img.shields.io/badge/release-1.4.2-blue.svg?style=flat
[releases]: https://github.com/snowplow/snowplow-android-tracker/releases

[license-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: https://www.apache.org/licenses/LICENSE-2.0

[coveralls-image]: https://coveralls.io/repos/github/snowplow/snowplow-android-tracker/badge.svg?branch=master
[coveralls]: https://coveralls.io/github/snowplow/snowplow-android-tracker?branch=master

[tracker-classificiation]: https://github.com/snowplow/snowplow/wiki/Tracker-Maintenance-Classification
[actively-maintained]: https://img.shields.io/static/v1?style=flat&label=Snowplow&message=Actively%20Maintained&color=6638b8&labelColor=9ba0aa&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAeFBMVEVMaXGXANeYANeXANZbAJmXANeUANSQAM+XANeMAMpaAJhZAJeZANiXANaXANaOAM2WANVnAKWXANZ9ALtmAKVaAJmXANZaAJlXAJZdAJxaAJlZAJdbAJlbAJmQAM+UANKZANhhAJ+EAL+BAL9oAKZnAKVjAKF1ALNBd8J1AAAAKHRSTlMAa1hWXyteBTQJIEwRgUh2JjJon21wcBgNfmc+JlOBQjwezWF2l5dXzkW3/wAAAHpJREFUeNokhQOCA1EAxTL85hi7dXv/E5YPCYBq5DeN4pcqV1XbtW/xTVMIMAZE0cBHEaZhBmIQwCFofeprPUHqjmD/+7peztd62dWQRkvrQayXkn01f/gWp2CrxfjY7rcZ5V7DEMDQgmEozFpZqLUYDsNwOqbnMLwPAJEwCopZxKttAAAAAElFTkSuQmCC
