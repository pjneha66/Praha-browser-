/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.middleware

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.service.pocket.PocketStory.ContentRecommendation
import mozilla.telemetry.glean.private.NoExtras
import org.praha.praha.GleanMetrics.HomeContentArticle
import org.praha.praha.GleanMetrics.Pings
import org.praha.praha.GleanMetrics.TopSites
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.appstate.AppAction.ContentRecommendationsAction
import org.praha.praha.components.appstate.AppAction.ShortcutAction
import org.praha.praha.components.appstate.AppState
import org.praha.praha.home.topsites.AddShortcutSource

/**
 * A [Middleware] for recording homepage related telemetry based on [AppAction]s that are dispatch to the [AppStore].
 */
class HomeTelemetryMiddleware : Middleware<AppState, AppAction> {
    override fun invoke(
        store: Store<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        next(action)

        when (action) {
            is ContentRecommendationsAction.ContentRecommendationClicked -> {
                val recommendation = action.recommendation

                HomeContentArticle.click.record(
                    extra =
                        HomeContentArticle.ClickExtra(
                            corpusItemId = recommendation.corpusItemId,
                            isSponsored = false,
                            position = action.position,
                            receivedRank = recommendation.receivedRank,
                            recommendedAt = recommendation.recommendedAt.toInt(),
                            scheduledCorpusItemId = recommendation.scheduledCorpusItemId,
                            tileId = recommendation.tileId.toInt(),
                            topic = recommendation.topic,
                            source = action.source.sourceName,
                        )
                )

                Pings.home.submit()
            }

            is ContentRecommendationsAction.PocketStoriesShown -> {
                for ((story, position) in action.impressions) {
                    when (story) {
                        is ContentRecommendation -> {
                            HomeContentArticle.impression.record(
                                extra =
                                    HomeContentArticle.ImpressionExtra(
                                        corpusItemId = story.corpusItemId,
                                        isSponsored = false,
                                        position = position,
                                        receivedRank = story.receivedRank,
                                        recommendedAt = story.recommendedAt.toInt(),
                                        scheduledCorpusItemId = story.scheduledCorpusItemId,
                                        tileId = story.tileId.toInt(),
                                        topic = story.topic,
                                        source = action.source.sourceName,
                                    )
                            )
                        }
                        else -> {
                            // no-op
                        }
                    }
                }

                Pings.home.submit()
            }

            is ShortcutAction.ShortcutAdded -> {
                TopSites.add.record(
                    TopSites.AddExtra(
                        source = action.source.value,
                        entryPoint = action.entryPoint.value,
                    )
                )
            }

            is ShortcutAction.AddShortcutSheetShown -> {
                TopSites.addSheetShown.record(TopSites.AddSheetShownExtra(entryPoint = action.entryPoint.value))
            }

            is ShortcutAction.AddWebsiteDialogShown -> {
                TopSites.addUrlShown.record(NoExtras())
            }

            is ShortcutAction.FrecencyTopSitePromoted -> {
                TopSites.add.record(TopSites.AddExtra(source = AddShortcutSource.FRECENCY_PROMOTE.value))
            }

            else -> {
                // no-op
            }
        }
    }
}
