/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.interactor

import org.praha.praha.home.bookmarks.interactor.BookmarksInteractor
import org.praha.praha.home.pocket.interactor.PocketStoriesInteractor
import org.praha.praha.home.privatebrowsing.interactor.PrivateBrowsingInteractor
import org.praha.praha.home.recentsyncedtabs.interactor.RecentSyncedTabInteractor
import org.praha.praha.home.recenttabs.interactor.RecentTabInteractor
import org.praha.praha.home.recentvisits.interactor.RecentVisitsInteractor
import org.praha.praha.home.search.HomeSearchInteractor
import org.praha.praha.home.sessioncontrol.CollectionInteractor
import org.praha.praha.home.sessioncontrol.MessageCardInteractor
import org.praha.praha.home.sessioncontrol.SetupChecklistInteractor
import org.praha.praha.home.sessioncontrol.TabSessionInteractor
import org.praha.praha.home.sessioncontrol.TrackingProtectionInteractor
import org.praha.praha.home.sessioncontrol.WallpaperInteractor
import org.praha.praha.home.termsofuse.PrivacyNoticeBannerInteractor
import org.praha.praha.home.toolbar.ToolbarInteractor
import org.praha.praha.home.topsites.interactor.TopSiteInteractor

/** Homepage interactor for interactions with the homepage UI. */
interface HomepageInteractor :
    CollectionInteractor,
    TopSiteInteractor,
    TabSessionInteractor,
    ToolbarInteractor,
    HomeSearchInteractor,
    MessageCardInteractor,
    PrivacyNoticeBannerInteractor,
    RecentTabInteractor,
    RecentSyncedTabInteractor,
    BookmarksInteractor,
    RecentVisitsInteractor,
    PocketStoriesInteractor,
    PrivateBrowsingInteractor,
    WallpaperInteractor,
    SetupChecklistInteractor,
    TrackingProtectionInteractor
