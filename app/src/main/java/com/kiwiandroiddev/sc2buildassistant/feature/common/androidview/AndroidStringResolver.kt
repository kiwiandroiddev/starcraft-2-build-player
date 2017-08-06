package com.kiwiandroiddev.sc2buildassistant.feature.common.androidview

import android.content.Context
import com.kiwiandroiddev.sc2buildassistant.feature.common.presentation.StringResolver

class AndroidStringResolver(val context: Context) : StringResolver {

    override fun getString(stringResourceId: Int): String =
            context.getString(stringResourceId)

    override fun getString(stringResourceId: Int, vararg formatArgs: Any): String =
            context.getString(stringResourceId, *formatArgs)

}