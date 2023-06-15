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
package com.snowplowanalytics.snowplow.util

import android.content.Context
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.utils.Util.uUIDString
import com.snowplowanalytics.core.constants.TrackerConstants

object TestUtils {
    @JvmStatic
    fun createSessionSharedPreferences(context: Context, namespaceId: String) {
        val sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS + "_" + namespaceId
        val sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(Parameters.SESSION_USER_ID, uUIDString())
        editor.putString(Parameters.SESSION_ID, uUIDString())
        editor.putInt(Parameters.SESSION_INDEX, 0)
        editor.commit()
    }
}
