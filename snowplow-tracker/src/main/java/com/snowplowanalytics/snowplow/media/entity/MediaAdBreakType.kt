/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.media.entity

/**
 * Type of ads within the break.
 */
enum class MediaAdBreakType {
    /// Take full control of the video for a period of time
    Linear,
    /// Run concurrently to the video
    NonLinear,
    /// Accompany the video but placed outside the player
    Companion;

    override fun toString(): String {
        return when (this) {
            Linear -> "linear"
            NonLinear -> "nonlinear"
            Companion -> "companion"
        }
    }
}
