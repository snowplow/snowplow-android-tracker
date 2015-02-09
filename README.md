# Android Analytics for Snowplow

[ ![Build Status] [travis-image] ] [travis]

## Overview

Add analytics to your Java software with the **[Snowplow] [snowplow]** event tracker for **[Android] [snowplow]**. See also: **[Snowplow Java Tracker] [snowplow-java-tracker]**.

With this tracker you can collect event data from your Android-based applications, games or frameworks.

## Quickstart

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow/snowplow-android-tracker.git
 host$ cd snowplow-android-tracker
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ ./gradlew clean build
```

## Find out more

| Technical Docs                  | Setup Guide               | Roadmap                 | Contributing                      |
|---------------------------------|---------------------------|-------------------------|-----------------------------------|
| ![i1] [techdocs-image]          | ![i2] [setup-image]       | ![i3] [roadmap-image]   | ![i4] [contributing-image]        |
| **[Technical Docs] [techdocs]** | **[Setup Guide] [setup]** | **[Roadmap] [roadmap]** | **[Contributing] [contributing]** |

## Copyright and license

The Snowplow Android Tracker is copyright 2014 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0] [license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[android]: http://www.android.com/

[snowplow]: http://snowplowanalytics.com
[snowplow-java-tracker]: https://github.com/snowplow/snowplow-java-tracker

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[setup-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[techdocs]: https://github.com/snowplow/snowplow/wiki/Android-Tracker
[setup]: https://github.com/snowplow/snowplow/wiki/Android-Tracker-Setup
[roadmap]: https://github.com/snowplow/snowplow/wiki/Product-roadmap
[contributing]: https://github.com/snowplow/snowplow/wiki/Contributing

[travis]: https://travis-ci.org/snowplow/snowplow-android-tracker
[travis-image]: https://travis-ci.org/snowplow/snowplow-android-tracker.svg?branch=master

[license]: http://www.apache.org/licenses/LICENSE-2.0
