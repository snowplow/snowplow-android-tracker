package com.snowplowanalytics.snowplow

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
        editor.putString(Parameters.SESSION_USER_ID, uUIDString)
        editor.putString(Parameters.SESSION_ID, uUIDString)
        editor.putInt(Parameters.SESSION_INDEX, 0)
        editor.commit()
    }
}
