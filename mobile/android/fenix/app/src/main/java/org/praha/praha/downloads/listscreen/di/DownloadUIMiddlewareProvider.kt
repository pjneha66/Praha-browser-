/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.downloads.listscreen.di

import android.content.Context
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import mozilla.components.lib.state.Middleware
import mozilla.components.support.utils.DefaultDownloadFileUtils
import org.praha.praha.components.Components
import org.praha.praha.downloads.listscreen.DownloadNavigationMiddleware
import org.praha.praha.downloads.listscreen.middleware.BroadcastSender
import org.praha.praha.downloads.listscreen.middleware.DefaultBroadcastSender
import org.praha.praha.downloads.listscreen.middleware.DefaultFileItemDescriptionProvider
import org.praha.praha.downloads.listscreen.middleware.DownloadDeleteMiddleware
import org.praha.praha.downloads.listscreen.middleware.DownloadTelemetryMiddleware
import org.praha.praha.downloads.listscreen.middleware.DownloadUIMapperMiddleware
import org.praha.praha.downloads.listscreen.middleware.DownloadUIRenameMiddleware
import org.praha.praha.downloads.listscreen.middleware.DownloadUIShareMiddleware
import org.praha.praha.downloads.listscreen.middleware.DownloadsServiceCommunicationMiddleware
import org.praha.praha.downloads.listscreen.store.DownloadUIAction
import org.praha.praha.downloads.listscreen.store.DownloadUIState
import org.praha.praha.ext.components
import org.praha.praha.utils.Settings.DeleteDownloadBehavior
import org.praha.praha.utils.getUndoDelay

internal object DownloadUIMiddlewareProvider {

    @Volatile private var broadcastSender: BroadcastSender? = null

    internal fun provideMiddleware(
        coroutineScope: CoroutineScope,
        applicationContext: Context,
        navController: NavController,
    ): List<Middleware<DownloadUIState, DownloadUIAction>> =
        listOf(
            provideUIMapperMiddleware(applicationContext, coroutineScope),
            provideShareMiddleware(applicationContext),
            provideTelemetryMiddleware(),
            provideDeleteMiddleware(
                applicationContext.components.settings.getUndoDelay(),
                applicationContext.components,
            ) {
                applicationContext.components.settings.deleteDownloadBehavior
            },
            provideDownloadsServiceCommunicationMiddleware(applicationContext),
            provideDownloadNavigationMiddleware(navController),
            provideRenameMiddleware(applicationContext, coroutineScope),
        )

    private fun provideDeleteMiddleware(
        undoDelay: Long,
        components: Components,
        deleteBehaviorProvider: () -> DeleteDownloadBehavior,
    ) =
        DownloadDeleteMiddleware(
            undoDelay = undoDelay,
            removeDownloadUseCase = components.useCases.downloadUseCases.removeDownload,
            deleteBehaviorProvider = deleteBehaviorProvider,
        )

    private fun provideShareMiddleware(applicationContext: Context) =
        DownloadUIShareMiddleware(applicationContext = applicationContext)

    private fun provideUIMapperMiddleware(
        applicationContext: Context,
        coroutineScope: CoroutineScope,
    ) =
        DownloadUIMapperMiddleware(
            browserStore = applicationContext.components.core.store,
            publicSuffixList = applicationContext.components.publicSuffixList,
            scope = coroutineScope,
            fileItemDescriptionProvider =
                DefaultFileItemDescriptionProvider(
                    context = applicationContext,
                    fileSizeFormatter = applicationContext.components.core.fileSizeFormatter,
                    downloadEstimator = applicationContext.components.core.downloadEstimator,
                    publicSuffixList = applicationContext.components.publicSuffixList,
                ),
        )

    private fun provideTelemetryMiddleware() = DownloadTelemetryMiddleware()

    private fun provideRenameMiddleware(
        applicationContext: Context,
        coroutineScope: CoroutineScope,
    ) =
        DownloadUIRenameMiddleware(
            browserStore = applicationContext.components.core.store,
            downloadFileUtils = DefaultDownloadFileUtils(applicationContext),
            scope = coroutineScope,
        )

    private fun provideDownloadsServiceCommunicationMiddleware(applicationContext: Context) =
        DownloadsServiceCommunicationMiddleware(provideBroadcastSender(applicationContext))

    private fun provideDownloadNavigationMiddleware(navController: NavController) =
        DownloadNavigationMiddleware(navController)

    private fun provideBroadcastSender(applicationContext: Context): BroadcastSender {
        initializeBroadcastSender(applicationContext)
        return requireNotNull(broadcastSender) {
            "BroadcastSender not initialized. Call initialize(applicationContext) first."
        }
    }

    private fun initializeBroadcastSender(applicationContext: Context) {
        if (broadcastSender == null) {
            synchronized(this) {
                if (broadcastSender == null) {
                    broadcastSender = DefaultBroadcastSender(applicationContext)
                }
            }
        }
    }
}
