# Android Analytics for Snowplow

[![early-release]][tracker-classification]
[![Build Status][gh-actions-image]][gh-actions]
[![Coverage Status][coveralls-image]][coveralls]
[![Release][release-image]][releases]
[![License][license-image]][license]

![snowplow-logo](.github/media/snowplow_logo.png)

Snowplow is a scalable open-source platform for rich, high quality, low-latency data collection. It is designed to collect high quality, complete behavioral data for enterprise business.

**To find out more, please check out the [Snowplow website][website] and our [documentation][docs].**

## Snowplow Kotlin Android Tracker Overview

The Snowplow Kotlin Android Tracker allows you to add analytics to your mobile apps when using a [Snowplow][snowplow] pipeline. With this tracker you can collect event data from your applications, games or frameworks.

This is a new tracker, being actively developed, based on the v4 Snowplow (Java) Android tracker. Watch out for currently undocumented API changes!

**Technical documentation can be found for each tracker in our [Documentation][mobile-docs].**

### Demo apps using the Snowplow Android Tracker

Two demo apps are included in this repository: one in [Java](https://github.com/snowplow-incubator/snowplow-kotlin-android-tracker/tree/main/snowplow-demo-java), one in [Kotlin](https://github.com/snowplow-incubator/snowplow-kotlin-android-tracker/tree/main/snowplow-demo-kotlin).


### Instrument the Kotlin Android Tracker

| Technical Docs                      | Setup Guide                           | API Docs                            |
|-------------------------------------|---------------------------------------|-------------------------------------|
| [![i1][tech-docs-image]][tech-docs] | [![i2][setup-docs-image]][setup-docs] | [![i3][setup-docs-image]][api-docs] |
| [Technical Docs][tech-docs]         | [Setup Guide][setup-docs]             | [API Docs][api-docs]                | 

## Maintainers 

| Contributing                                 |
|----------------------------------------------|
| [![i4][contributing-image]](CONTRIBUTING.md) |
| [Contributing](CONTRIBUTING.md)              |

## Copyright and license

The Snowplow Android Tracker is copyright 2015-2022 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[website]: https://snowplow.io
[snowplow]: https://github.com/snowplow/snowplow
[docs]: https://docs.snowplow.io/
[mobile-docs]: https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/mobile-trackers/

[gh-actions]: https://github.com/snowplow-incubator/snowplow-kotlin-android-tracker/actions/workflows/build.yml
[gh-actions-image]: https://github.com/snowplow-incubator/snowplow-kotlin-android-tracker/actions/workflows/build.yml/badge.svg

[coveralls]: https://coveralls.io/github/snowplow-incubator/snowplow-kotlin-android-tracker?branch=main
[coveralls-image]: https://coveralls.io/repos/github/snowplow-incubator/snowplow-kotlin-android-tracker/badge.svg?branch=main

[license]: https://www.apache.org/licenses/LICENSE-2.0
[license-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat

[release-image]: https://img.shields.io/github/v/release/snowplow/snowplow-kotlin-android-tracker?sort=semver
[releases]: https://github.com/snowplow-incubator/snowplow-kotlin-android-tracker/releases

[setup-docs]: https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/mobile-trackers/installation-and-set-up/
[setup-docs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/setup.png

[tech-docs]: https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/mobile-trackers/
[tech-docs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png

[api-docs]: https://snowplow.github.io/snowplow-kotlin-android-tracker/

[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[tracker-classification]: https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/tracker-maintenance-classification/
[early-release]: https://img.shields.io/static/v1?style=flat&label=Snowplow&message=Early%20Release&color=014477&labelColor=9ba0aa&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAeFBMVEVMaXGXANeYANeXANZbAJmXANeUANSQAM+XANeMAMpaAJhZAJeZANiXANaXANaOAM2WANVnAKWXANZ9ALtmAKVaAJmXANZaAJlXAJZdAJxaAJlZAJdbAJlbAJmQAM+UANKZANhhAJ+EAL+BAL9oAKZnAKVjAKF1ALNBd8J1AAAAKHRSTlMAa1hWXyteBTQJIEwRgUh2JjJon21wcBgNfmc+JlOBQjwezWF2l5dXzkW3/wAAAHpJREFUeNokhQOCA1EAxTL85hi7dXv/E5YPCYBq5DeN4pcqV1XbtW/xTVMIMAZE0cBHEaZhBmIQwCFofeprPUHqjmD/+7peztd62dWQRkvrQayXkn01f/gWp2CrxfjY7rcZ5V7DEMDQgmEozFpZqLUYDsNwOqbnMLwPAJEwCopZxKttAAAAAElFTkSuQmCC
