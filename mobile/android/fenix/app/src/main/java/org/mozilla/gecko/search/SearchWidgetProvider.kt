/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.search

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.praha.praha.R
import org.praha.praha.widget.SearchWidgetIntents
import org.praha.praha.widget.SearchWidgets
import org.praha.praha.widget.glance.CompactSearchBarWidgetReceiver
import org.praha.praha.widget.glance.SearchBarWidgetReceiver
import org.praha.praha.widget.glance.SearchBarWithLensWidgetReceiver

enum class SearchWidgetProviderSize {
    EXTRA_SMALL_V1,
    EXTRA_SMALL_V2,
    SMALL,
    MEDIUM,
    LARGE,
}

class SearchWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SearchWidgets.syncInstalledState(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        SearchWidgets.syncInstalledState(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        SearchWidgets.syncInstalledState(context)
        val textIntent = createTextSearchIntent(context)
        val voiceIntent = createVoiceSearchIntent(context)
        val lensIntent = createLensSearchIntent(context)

        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val size = getLayoutSize(minWidth)
            val layout = getLayout(size, voiceIntent != null)
            val text = getText(size, context)
            val views = createRemoteViews(context, layout, textIntent, voiceIntent, lensIntent, text)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    fun createTextSearchIntent(context: Context): PendingIntent {
        val intent = SearchWidgetIntents.textSearch(context, SearchWidgets.LEGACY_WIDGET_TYPE)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_TEXT_SEARCH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun createVoiceSearchIntent(context: Context): PendingIntent? {
        val intent = SearchWidgetIntents.maybeVoiceSearch(context, SearchWidgets.LEGACY_WIDGET_TYPE) ?: return null
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_VOICE_SEARCH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun createLensSearchIntent(context: Context): PendingIntent? {
        val intent = SearchWidgetIntents.maybeLensSearch(context, SearchWidgets.LEGACY_WIDGET_TYPE) ?: return null
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_LENS_SEARCH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun createRemoteViews(
        context: Context,
        layout: Int,
        textSearchIntent: PendingIntent?,
        voiceSearchIntent: PendingIntent?,
        lensSearchIntent: PendingIntent?,
        text: String?,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        textSearchIntent?.let {
            views.setOnClickPendingIntent(R.id.button_search_widget_new_tab, it)
            views.setOnClickPendingIntent(R.id.button_search_widget_new_tab_icon, it)
        }
        if (text != null) {
            views.setTextViewText(R.id.button_search_widget_new_tab, text)
        }
        if (voiceSearchIntent != null) {
            views.setViewVisibility(R.id.button_search_widget_voice, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.button_search_widget_voice, voiceSearchIntent)
        } else {
            views.setViewVisibility(R.id.button_search_widget_voice, View.GONE)
        }
        if (lensSearchIntent != null) {
            views.setViewVisibility(R.id.button_search_widget_lens, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.button_search_widget_lens, lensSearchIntent)
        } else {
            views.setViewVisibility(R.id.button_search_widget_lens, View.GONE)
        }
        return views
    }

    companion object {
        const val REQUEST_CODE_TEXT_SEARCH = 1
        const val REQUEST_CODE_VOICE_SEARCH = 2
        const val REQUEST_CODE_LENS_SEARCH = 3

        fun getLayoutSize(dp: Int): SearchWidgetProviderSize = when {
            dp < 64 -> SearchWidgetProviderSize.EXTRA_SMALL_V1
            dp < 100 -> SearchWidgetProviderSize.EXTRA_SMALL_V2
            dp < 192 -> SearchWidgetProviderSize.SMALL
            dp < 256 -> SearchWidgetProviderSize.MEDIUM
            else -> SearchWidgetProviderSize.LARGE
        }

        fun getLayout(size: SearchWidgetProviderSize, showMic: Boolean): Int = when (size) {
            SearchWidgetProviderSize.LARGE -> R.layout.search_widget_large
            SearchWidgetProviderSize.MEDIUM -> R.layout.search_widget_medium
            SearchWidgetProviderSize.SMALL -> if (showMic) R.layout.search_widget_small else R.layout.search_widget_small_no_mic
            SearchWidgetProviderSize.EXTRA_SMALL_V2 -> R.layout.search_widget_extra_small_v2
            SearchWidgetProviderSize.EXTRA_SMALL_V1 -> R.layout.search_widget_extra_small_v1
        }

        fun getText(size: SearchWidgetProviderSize, context: Context): String? = when (size) {
            SearchWidgetProviderSize.LARGE -> context.getString(R.string.search_widget_text_long)
            SearchWidgetProviderSize.MEDIUM -> context.getString(R.string.search_widget_text_short)
            else -> null
        }

        fun updateAllWidgets(
            context: Context,
            widgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        ) {
            val providers = listOf(
                SearchWidgetProvider::class.java,
                SearchBarWidgetReceiver::class.java,
                SearchBarWithLensWidgetReceiver::class.java,
                CompactSearchBarWidgetReceiver::class.java,
            )
            for (provider in providers) {
                val ids = widgetManager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isNotEmpty()) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = ComponentName(context, provider)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            }
        }
    }
}

