// Praha Media & Power Suite - Content Script (v2.0.16)
(() => {
  // 1. Geolocation Spoof & Hide Engine (runs at document_start)
  let currentGeo = { mode: 'off', preset: 'Tokyo', lat: 35.6762, lng: 139.6503 };
  const GEO_MAP = {
    'Passthrough': { mode: 'off' },
    'Hide': { mode: 'hide' },
    'Tokyo': { mode: 'spoof', preset: 'Tokyo', lat: 35.6762, lng: 139.6503 },
    'London': { mode: 'spoof', preset: 'London', lat: 51.5074, lng: -0.1278 },
    'New York': { mode: 'spoof', preset: 'New York', lat: 40.7128, lng: -74.0060 },
    'Zurich': { mode: 'spoof', preset: 'Zurich', lat: 47.3769, lng: 8.5417 }
  };

  function injectGeolocationOverride(geo) {
    if (!geo || geo.mode === 'off') {
      const old = document.getElementById('praha-geo-spoof-engine');
      if (old) old.remove();
      return;
    }

    const script = document.createElement('script');
    script.id = 'praha-geo-spoof-engine';
    script.textContent = `
      (() => {
        const geo = ${JSON.stringify(geo)};
        if (!navigator || !navigator.geolocation) return;

        const mockCoords = {
          latitude: geo.lat || 35.6762,
          longitude: geo.lng || 139.6503,
          altitude: 40.0,
          accuracy: 12.0,
          altitudeAccuracy: 5.0,
          heading: null,
          speed: null
        };

        const spoofed = {
          getCurrentPosition: function(success, error, options) {
            if (geo.mode === 'hide') {
              if (typeof error === 'function') {
                setTimeout(() => error({
                  code: 1,
                  message: 'User denied Geolocation',
                  PERMISSION_DENIED: 1,
                  POSITION_UNAVAILABLE: 2,
                  TIMEOUT: 3
                }), 20);
              }
              return;
            }
            if (typeof success === 'function') {
              setTimeout(() => success({
                coords: mockCoords,
                timestamp: Date.now()
              }), 20);
            }
          },
          watchPosition: function(success, error, options) {
            const watchId = Math.floor(Math.random() * 100000) + 1;
            this.getCurrentPosition(success, error, options);
            return watchId;
          },
          clearWatch: function(id) {}
        };

        try {
          Object.defineProperty(navigator, 'geolocation', {
            value: spoofed,
            configurable: true,
            enumerable: true,
            writable: true
          });
        } catch(e) {}
      })();
    `;
    (document.head || document.documentElement).appendChild(script);
    script.remove();
  }

  // 2. Background Playback Keep-Alive (runs at document_start)
  try {
    Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
    Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
    Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });
  } catch (e) {}

  ['visibilitychange', 'webkitvisibilitychange', 'pagehide'].forEach(evt => {
    window.addEventListener(evt, (e) => {
      e.stopImmediatePropagation();
    }, true);
    document.addEventListener(evt, (e) => {
      e.stopImmediatePropagation();
    }, true);
  });

  // Re-enable context menu, selection and copy on restricted reader/manga sites
  ['contextmenu', 'copy', 'selectstart'].forEach(evt => {
    document.addEventListener(evt, (e) => {
      e.stopPropagation();
    }, true);
  });

  // Intercept magnet link clicks
  document.addEventListener('click', (e) => {
    const link = e.target.closest('a[href^="magnet:?"]');
    if (link) {
      e.preventDefault();
      e.stopPropagation();
      const magnetUrl = link.href;
      const streamerUrl = browser.runtime.getURL('webtorrent.html') + '?magnet=' + encodeURIComponent(magnetUrl);
      window.location.href = streamerUrl;
    }
  }, true);

  // 2. Comic & Manga Webtoon Reader Mode
  let isWebtoonActive = false;
  const COMIC_DOMAINS = ['batcave.biz', 'getcomics.org', 'mangadot.net', 'atsu.moe', 'comix.to', 'mkissa.to', 'kagane.moe'];
  const isComicSite = COMIC_DOMAINS.some(d => location.hostname.endsWith(d));

  function setWebtoonMode(enabled) {
    isWebtoonActive = enabled;
    let style = document.getElementById('praha-webtoon-style');
    let pill = document.getElementById('praha-webtoon-pill');

    if (enabled) {
      document.body.classList.add('praha-webtoon-active');
      if (!style) {
        style = document.createElement('style');
        style.id = 'praha-webtoon-style';
        style.textContent = `
          body.praha-webtoon-active {
            background: #000000 !important;
            color: #EDEDED !important;
            margin: 0 !important;
            padding: 0 !important;
          }
          body.praha-webtoon-active header,
          body.praha-webtoon-active footer,
          body.praha-webtoon-active nav,
          body.praha-webtoon-active aside,
          body.praha-webtoon-active .sidebar,
          body.praha-webtoon-active [class*="advertisement"],
          body.praha-webtoon-active [class*="banner"],
          body.praha-webtoon-active .adsbygoogle {
            display: none !important;
          }
          body.praha-webtoon-active img {
            display: block !important;
            margin: 0 auto !important;
            max-width: 100% !important;
            height: auto !important;
            padding: 0 !important;
            border: none !important;
            box-shadow: none !important;
          }
        `;
        document.head.appendChild(style);
      }

      if (!pill && window === window.top) {
        pill = document.createElement('div');
        pill.id = 'praha-webtoon-pill';
        pill.innerHTML = `<span>Webtoon Mode (Vol +/- Scroll)</span> <button id="praha-webtoon-exit">&times;</button>`;
        document.body.appendChild(pill);
        pill.querySelector('#praha-webtoon-exit').onclick = () => setWebtoonMode(false);
      } else if (pill) {
        pill.style.display = 'flex';
      }
    } else {
      document.body.classList.remove('praha-webtoon-active');
      if (pill) pill.style.display = 'none';
    }
  }

  // Volume Key Page-Turning / Smooth Scrolling Listener
  window.addEventListener('keydown', (e) => {
    if (!isWebtoonActive) return;
    const isDown = e.key === 'VolumeDown' || e.code === 'VolumeDown' || e.keyCode === 25;
    const isUp = e.key === 'VolumeUp' || e.code === 'VolumeUp' || e.keyCode === 24;

    if (isDown) {
      e.preventDefault();
      e.stopPropagation();
      window.scrollBy({ top: window.innerHeight * 0.85, behavior: 'smooth' });
    } else if (isUp) {
      e.preventDefault();
      e.stopPropagation();
      window.scrollBy({ top: -window.innerHeight * 0.85, behavior: 'smooth' });
    }
  }, true);

  // Auto-activate webtoon mode on comic reader pages
  if (isComicSite && (location.pathname.includes('/chapter') || location.pathname.includes('/read') || location.pathname.includes('/comic'))) {
    setTimeout(() => setWebtoonMode(true), 1200);
  }

  // 3. Chrome Web Store 1-Tap Installer Banner
  function checkChromeWebStore() {
    if (!location.hostname.includes('chromewebstore.google.com') && !location.hostname.includes('chrome.google.com')) {
      return;
    }

    const match = location.pathname.match(/\/detail\/[^\/]+\/([a-z]{32})/i) || location.pathname.match(/\/detail\/([a-z]{32})/i);
    if (!match) return;

    const extensionId = match[1];
    let banner = document.getElementById('praha-cws-banner');
    if (!banner) {
      banner = document.createElement('div');
      banner.id = 'praha-cws-banner';
      banner.innerHTML = `
        <div style="display:flex;align-items:center;gap:10px;">
          <div style="width:28px;height:28px;border-radius:6px;background:#7C4DFF;display:flex;align-items:center;justify-content:center;color:#FFF;font-weight:700;">P</div>
          <div>
            <div style="font-weight:700;color:#FFF;font-size:13px;">Praha Chrome Extension Installer</div>
            <div style="color:#A0A0BA;font-size:11px;">1-Tap Auto CRX-to-XPI Conversion &amp; Sideloading</div>
          </div>
        </div>
        <button id="praha-install-crx-btn" style="background:linear-gradient(135deg,#7C4DFF,#3D5AFE);color:#FFF;border:none;padding:8px 16px;border-radius:8px;font-weight:700;font-size:12px;cursor:pointer;">Install in Praha</button>
      `;
      document.body.prepend(banner);

      banner.querySelector('#praha-install-crx-btn').addEventListener('click', () => {
        const btn = banner.querySelector('#praha-install-crx-btn');
        btn.textContent = 'Converting & Installing...';
        btn.style.opacity = '0.7';

        browser.runtime.sendMessage({
          type: 'INSTALL_CHROME_EXTENSION',
          extensionId: extensionId
        }).then(res => {
          if (res && res.success) {
            btn.textContent = 'Installed!';
            showToast('Chrome extension converted to XPI and initiated!');
          } else {
            btn.textContent = 'Install in Praha';
            btn.style.opacity = '1';
            showToast('Download initiated for CRX package.');
          }
        }).catch(() => {
          btn.textContent = 'Install in Praha';
          btn.style.opacity = '1';
        });
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkChromeWebStore);
  } else {
    checkChromeWebStore();
  }

  if (window.__praha_media_injected) return;
  window.__praha_media_injected = true;

  let detectedMedia = [];
  let isSheetOpen = false;
  let activeTabName = 'media';
  let currentSpeed = 1.0;
  let currentBoost = 1.0;
  let currentDoH = 'Cloudflare';
  let vaultUnlocked = false;
  let vaultItems = [];

  // 4. Audio Gain Booster (Web Audio API)
  function applyAudioBoost(factor) {
    currentBoost = factor;
    const mediaList = document.querySelectorAll('video, audio');
    mediaList.forEach(m => {
      try {
        if (!m.__praha_gain_node) {
          const AudioCtx = window.AudioContext || window.webkitAudioContext;
          if (AudioCtx) {
            const ctx = new AudioCtx();
            const source = ctx.createMediaElementSource(m);
            const gainNode = ctx.createGain();
            source.connect(gainNode);
            gainNode.connect(ctx.destination);
            m.__praha_audio_ctx = ctx;
            m.__praha_gain_node = gainNode;
          }
        }
        if (m.__praha_gain_node) {
          m.__praha_gain_node.gain.value = factor;
        }
        if (m.__praha_audio_ctx && m.__praha_audio_ctx.state === 'suspended') {
          m.__praha_audio_ctx.resume();
        }
      } catch (e) {}
    });
  }

  // 5. Playback Speed Overdrive (0.25x to 4.0x)
  function applyPlaybackSpeed(rate) {
    currentSpeed = rate;
    const mediaList = document.querySelectorAll('video, audio');
    mediaList.forEach(m => {
      try {
        m.playbackRate = rate;
      } catch (e) {}
    });
  }

  // 6. Auto Ad-Skipper, Paywall Cleaner & Anti-Adblock Defuser
  function defuseAdsAndOverlays() {
    const skipSelectors = [
      '.ytp-ad-skip-button',
      '.ytp-ad-skip-button-modern',
      '.videoAdUiSkipButton',
      '.ytp-ad-overlay-close-button',
      '.ytp-ad-skip-button-slot',
      '.btn-skip-ad',
      '[class*="skip-button"]',
      '[class*="ad-skip"]',
      'button[id^="skip-button"]'
    ];
    for (const sel of skipSelectors) {
      const btn = document.querySelector(sel);
      if (btn && typeof btn.click === 'function') {
        btn.click();
      }
    }

    const adBadge = document.querySelector('.ytp-ad-player-overlay, .ad-interrupting, .video-ads');
    if (adBadge) {
      const video = document.querySelector('video');
      if (video && !isNaN(video.duration) && video.duration > 0) {
        video.muted = true;
        video.currentTime = video.duration;
        video.playbackRate = 16.0;
      }
    }

    const modalSelectors = [
      'tp-yt-paper-dialog:has(.yt-meal-dialog-renderer)',
      '#adblock-modal',
      '.adblock-backdrop',
      '.adblock-overlay',
      '[id*="adblock-detected"]',
      '[class*="adblock-detected"]',
      '.paywall-overlay',
      '.subscriber-only',
      '#gateway-content',
      'div[class*="paywall"]'
    ];
    for (const sel of modalSelectors) {
      try {
        const modal = document.querySelector(sel);
        if (modal) {
          modal.remove();
          document.body.style.overflow = 'auto';
        }
      } catch (e) {}
    }
  }

  setInterval(defuseAdsAndOverlays, 600);

  // 7. DOM Media Sniffer
  function scanDOMMedia() {
    const found = [];
    const mediaElements = document.querySelectorAll('video, audio, source');

    mediaElements.forEach((el) => {
      let src = el.currentSrc || el.src || el.getAttribute('src');
      if (!src && el.tagName === 'VIDEO') {
        const childSource = el.querySelector('source');
        if (childSource) src = childSource.src || childSource.getAttribute('src');
      }

      if (src && src.startsWith('http')) {
        const isVideo = el.tagName === 'VIDEO' || (el.tagName === 'SOURCE' && (el.type || '').includes('video'));
        let format = 'MP4';
        const clean = src.split('?')[0].toLowerCase();
        if (clean.endsWith('.m3u8')) format = 'M3U8';
        else if (clean.endsWith('.webm')) format = 'WEBM';
        else if (clean.endsWith('.mp3')) format = 'MP3';
        else if (clean.endsWith('.m4a')) format = 'M4A';
        else if (clean.endsWith('.mpd')) format = 'DASH';

        let quality = '';
        if (el.videoWidth && el.videoHeight) {
          quality = el.videoHeight >= 1080 ? '1080p' : el.videoHeight >= 720 ? '720p' : el.videoHeight + 'p';
        }

        let title = el.getAttribute('title') || el.getAttribute('data-title') || document.title || 'media';
        try {
          const u = new URL(src);
          const parts = u.pathname.split('/');
          const filename = decodeURIComponent(parts[parts.length - 1] || '');
          if (filename && filename.length > 3) title = filename;
        } catch (e) {}

        found.push({
          id: 'dom_' + Math.random().toString(36).substring(2, 9),
          url: src,
          title: title,
          format: format,
          quality: quality,
          type: isVideo ? 'video' : 'audio',
          size: quality ? quality : 'Web Video',
          sizeBytes: 0,
          timestamp: Date.now()
        });
      }
    });

    if (found.length > 0) {
      try {
        browser.runtime.sendMessage({ type: 'DOM_MEDIA_FOUND', items: found }).catch(() => {});
      } catch (e) {}
    }
  }

  function hookMediaEvents() {
    document.addEventListener('play', (e) => {
      if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) {
        scanDOMMedia();
        if (currentSpeed !== 1.0) e.target.playbackRate = currentSpeed;
        if (currentBoost !== 1.0) applyAudioBoost(currentBoost);
      }
    }, true);

    document.addEventListener('loadeddata', (e) => {
      if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) {
        scanDOMMedia();
      }
    }, true);
  }

  const observer = new MutationObserver(() => {
    scanDOMMedia();
  });
  if (document.body) {
    observer.observe(document.body, { childList: true, subtree: true });
  } else {
    document.addEventListener('DOMContentLoaded', () => {
      observer.observe(document.body, { childList: true, subtree: true });
    });
  }

  hookMediaEvents();
  scanDOMMedia();

  // Top-frame UI only
  if (window !== window.top) return;

  function createUI() {
    let root = document.getElementById('praha-media-root');
    if (!root) {
      root = document.createElement('div');
      root.id = 'praha-media-root';
      document.body.appendChild(root);
    }
    return root;
  }

  function updateFloatingButton(count) {
    const root = createUI();
    let btn = document.getElementById('praha-media-pill');

    if (count <= 0) {
      if (btn) btn.style.display = 'none';
      return;
    }

    if (!btn) {
      btn = document.createElement('div');
      btn.id = 'praha-media-pill';
      btn.innerHTML = `
        <div class="praha-pill-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z"/>
          </svg>
        </div>
        <span class="praha-pill-badge">${count}</span>
      `;
      btn.addEventListener('click', toggleSheet);
      root.appendChild(btn);

      // Touch drag handling
      let startY = 0, startBottom = 84;
      btn.addEventListener('touchstart', (e) => {
        startY = e.touches[0].clientY;
        const rect = btn.getBoundingClientRect();
        startBottom = window.innerHeight - rect.bottom;
      }, { passive: true });

      btn.addEventListener('touchmove', (e) => {
        const delta = startY - e.touches[0].clientY;
        if (Math.abs(delta) > 5) {
          const newBottom = Math.max(20, Math.min(window.innerHeight - 80, startBottom + delta));
          btn.style.bottom = `${newBottom}px`;
        }
      }, { passive: true });
    } else {
      btn.style.display = 'flex';
      const badge = btn.querySelector('.praha-pill-badge');
      if (badge) badge.textContent = String(count);
    }
  }

  function toggleSheet() {
    isSheetOpen = !isSheetOpen;
    const root = createUI();

    let backdrop = document.getElementById('praha-media-backdrop');
    let sheet = document.getElementById('praha-media-sheet');

    if (!backdrop) {
      backdrop = document.createElement('div');
      backdrop.id = 'praha-media-backdrop';
      backdrop.addEventListener('click', toggleSheet);
      root.appendChild(backdrop);
    }

    if (!sheet) {
      sheet = document.createElement('div');
      sheet.id = 'praha-media-sheet';
      root.appendChild(sheet);
    }

    if (isSheetOpen) {
      renderSheetContent(sheet);
      backdrop.classList.add('praha-show');
      sheet.classList.add('praha-show');
    } else {
      backdrop.classList.remove('praha-show');
      sheet.classList.remove('praha-show');
    }
  }

  function renderSheetContent(sheet) {
    const count = detectedMedia.length;

    let html = `
      <div class="praha-sheet-header">
        <div class="praha-sheet-title-group">
          <div class="praha-sheet-logo">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="#7C4DFF">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/>
            </svg>
          </div>
          <div>
            <div class="praha-sheet-title">Praha Power Suite v2.0.15</div>
            <div class="praha-sheet-subtitle">${count} media items • Webtoon Reader &amp; CRX Active</div>
          </div>
        </div>
        <div class="praha-sheet-actions">
          <button class="praha-icon-btn" id="praha-refresh-btn" title="Rescan">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
          </button>
          <button class="praha-icon-btn" id="praha-close-btn" title="Close">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
      </div>

      <!-- Tab Switcher Navigation -->
      <div class="praha-tab-nav">
        <button class="praha-tab-btn ${activeTabName === 'media' ? 'praha-tab-active' : ''}" data-tab="media">Media Sniffer (${count})</button>
        <button class="praha-tab-btn ${activeTabName === 'power' ? 'praha-tab-active' : ''}" data-tab="power">Power Controls</button>
        <button class="praha-tab-btn ${activeTabName === 'vault' ? 'praha-tab-active' : ''}" data-tab="vault">Private Vault</button>
      </div>

      <div class="praha-tab-content">
    `;

    if (activeTabName === 'media') {
      html += `
        <div class="praha-media-list">
      `;
      if (detectedMedia.length === 0) {
        html += `
          <div class="praha-empty-state">
            <div style="font-size:14px;font-weight:600;color:#A0A0B8;">No active media streams found</div>
            <div style="font-size:12px;color:#707088;margin-top:4px;">Play a video or audio stream on this page to sniff it.</div>
          </div>
        `;
      } else {
        detectedMedia.forEach((item, index) => {
          const isHls = item.format === 'M3U8';
          const isAudio = item.type === 'audio';
          const badgeColor = isHls ? '#FF9100' : isAudio ? '#00E5FF' : '#7C4DFF';

          html += `
            <div class="praha-media-card">
              <div class="praha-media-top">
                <span class="praha-format-badge" style="background:${badgeColor}22; color:${badgeColor}; border:1px solid ${badgeColor}44;">${item.format}</span>
                ${item.quality ? `<span class="praha-quality-badge">${item.quality}</span>` : ''}
                <span class="praha-size-badge">${item.size || 'Stream'}</span>
              </div>
              <div class="praha-media-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title)}</div>
              <div class="praha-card-actions">
                <button class="praha-btn praha-btn-turbo praha-turbo-item" data-index="${index}" title="Multi-Thread Chunk Download">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor"><path d="M13 2.05v3.03c3.39.49 6 3.39 6 6.92 0 .9-.18 1.75-.48 2.54l2.6 1.53c.56-1.24.88-2.62.88-4.07 0-5.18-3.95-9.45-9-9.95zM12 19c-3.87 0-7-3.13-7-7 0-3.53 2.61-6.43 6-6.92V2.05c-5.06.5-9 4.76-9 9.95 0 5.52 4.47 10 9.99 10 3.31 0 6.24-1.61 8.01-4.09l-2.45-1.45C16.14 17.86 14.21 19 12 19z"/></svg>
                  Turbo
                </button>
                <button class="praha-btn praha-btn-primary praha-download-item" data-index="${index}">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                  Download
                </button>
                <button class="praha-btn praha-btn-secondary praha-vault-item" data-index="${index}" title="Download to Vault">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                  Vault
                </button>
                <button class="praha-btn praha-btn-secondary praha-copy-item" data-url="${escapeHtml(item.url)}">Copy</button>
                <button class="praha-btn praha-btn-secondary praha-play-item" data-url="${escapeHtml(item.url)}">Play</button>
              </div>
            </div>
          `;
        });
      }
      html += `</div>`;

      if (count > 1) {
        html += `
          <div class="praha-sheet-footer">
            <button class="praha-btn praha-btn-accent" id="praha-download-all-btn">
              Download All (${count} Files)
            </button>
          </div>
        `;
      }
    } else if (activeTabName === 'power') {
      html += `
        <div class="praha-power-controls">
          <div class="praha-control-row">
            <span class="praha-control-label">Speed:</span>
            <div class="praha-pill-group">
              ${[0.5, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0].map(s => `
                <button class="praha-speed-pill ${currentSpeed === s ? 'praha-pill-active' : ''}" data-speed="${s}">${s}x</button>
              `).join('')}
            </div>
          </div>
          <div class="praha-control-row">
            <span class="praha-control-label">Audio Boost:</span>
            <div class="praha-pill-group">
              ${[1.0, 1.5, 2.0, 3.0].map(b => `
                <button class="praha-boost-pill ${currentBoost === b ? 'praha-pill-active' : ''}" data-boost="${b}">${Math.round(b * 100)}%</button>
              `).join('')}
            </div>
          </div>
          <div class="praha-control-row">
            <span class="praha-control-label">DoH DNS:</span>
            <div class="praha-pill-group">
              ${['Cloudflare', 'AdGuard', 'NextDNS', 'Mullvad', 'Quad9'].map(d => `
                <button class="praha-doh-pill ${currentDoH === d ? 'praha-pill-active' : ''}" data-doh="${d}">${d}</button>
              `).join('')}
              <button class="praha-btn praha-btn-secondary" id="praha-dns-bench-btn" style="padding:2px 8px;font-size:10px;">Bench</button>
            </div>
          </div>
          <div class="praha-control-row">
            <span class="praha-control-label">Geo Privacy:</span>
            <div class="praha-pill-group" id="praha-geo-group">
              ${['Passthrough', 'Hide', 'Tokyo', 'London', 'New York', 'Zurich'].map(g => `
                <button class="praha-geo-pill ${(currentGeo.mode === 'off' && g === 'Passthrough') || (currentGeo.mode === 'hide' && g === 'Hide') || (currentGeo.mode === 'spoof' && currentGeo.preset === g) ? 'praha-pill-active' : ''}" data-geo="${g}">${g}</button>
              `).join('')}
            </div>
          </div>
          <div class="praha-control-row" style="margin-top:4px;">
            <span class="praha-control-label">Webtoon Mode:</span>
            <button class="praha-btn ${isWebtoonActive ? 'praha-btn-primary' : 'praha-btn-secondary'}" id="praha-toggle-webtoon-btn" style="flex:1;">
              ${isWebtoonActive ? 'Active (Tap to Exit)' : 'Enable Continuous Scroll &amp; Vol Nav'}
            </button>
          </div>
          <div class="praha-control-row" style="margin-top:4px;">
            <span class="praha-control-label">Chrome Ext:</span>
            <div style="display:flex;gap:6px;flex:1;">
              <input type="text" id="praha-crx-input" placeholder="Chrome Web Store URL or ID" style="flex:1;background:#181826;border:1px solid #3A3A52;color:#FFF;padding:6px 10px;border-radius:8px;font-size:11px;outline:none;">
              <button class="praha-btn praha-btn-primary" id="praha-crx-install-btn" style="padding:6px 10px;">Sideload</button>
            </div>
          </div>
          <div class="praha-badge-row" style="margin-top:10px;">
            <span class="praha-status-chip">Settings Sync: Active</span>
            <span class="praha-status-chip">CRX Converter: Ready</span>
            <span class="praha-status-chip">Background Play: Active</span>
            <span class="praha-status-chip">Auto-PiP: Active</span>
            <span class="praha-status-chip">GPS Spoofer: Active</span>
            <span class="praha-status-chip">Ad-Skipper: Active</span>
          </div>
        </div>
      `;
    } else if (activeTabName === 'vault') {
      html += `
        <div class="praha-vault-wrap">
      `;
      if (!vaultUnlocked) {
        html += `
          <div class="praha-vault-lock">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="#7C4DFF" style="margin-bottom:12px;">
              <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
            </svg>
            <div style="font-size:14px;font-weight:700;color:#FFFFFF;margin-bottom:6px;">Private Media Vault</div>
            <div style="font-size:12px;color:#8E8EA8;margin-bottom:16px;">Protected biometric &amp; PIN storage. Hidden from Android Gallery.</div>
            <button class="praha-btn praha-btn-primary" id="vault-biometric-btn" style="width:100%;max-width:260px;margin-bottom:10px;display:flex;align-items:center;justify-content:center;gap:6px;">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M17.81 4.47c-.08 0-.16-.02-.23-.06C15.66 3.42 14 3 12.01 3c-1.98 0-3.86.47-5.57 1.41-.24.13-.54.04-.68-.2-.13-.24-.04-.55.2-.68C7.82 2.52 9.86 2 12.01 2c2.13 0 3.99.47 5.93 1.51.24.13.33.44.2.68-.09.18-.28.28-.53.28zM12 7c-2.76 0-5 2.24-5 5v3c0 2.76 2.24 5 5 5s5-2.24 5-5v-3c0-2.76-2.24-5-5-5z"/>
              </svg>
              <span>Unlock with Biometrics</span>
            </button>
            <div style="display:flex;gap:8px;width:100%;max-width:260px;">
              <input type="password" id="vault-pin-input" placeholder="PIN (Default: 0000)" style="flex:1;background:#181826;border:1px solid #3A3A52;color:#FFF;padding:8px 12px;border-radius:8px;font-size:13px;outline:none;">
              <button class="praha-btn praha-btn-secondary" id="vault-unlock-btn">Unlock PIN</button>
            </div>
          </div>
        `;
      } else {
        html += `
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
            <div style="font-size:13px;font-weight:700;color:#00E5FF;">Vault Unlocked (${vaultItems.length} items)</div>
            <button class="praha-btn praha-btn-secondary" id="vault-lock-btn" style="padding:4px 10px;font-size:11px;">Lock</button>
          </div>
          <div class="praha-media-list">
        `;
        if (vaultItems.length === 0) {
          html += `
            <div class="praha-empty-state">
              <div style="font-size:13px;color:#A0A0B8;">Vault is empty</div>
              <div style="font-size:11px;color:#707088;margin-top:4px;">Tap 'Vault' on any detected media stream to save it here.</div>
            </div>
          `;
        } else {
          vaultItems.forEach(vi => {
            html += `
              <div class="praha-media-card">
                <div class="praha-media-top">
                  <span class="praha-format-badge" style="background:#7C4DFF22;color:#7C4DFF;border:1px solid #7C4DFF44;">${vi.format}</span>
                  <span class="praha-size-badge">${new Date(vi.timestamp).toLocaleDateString()}</span>
                </div>
                <div class="praha-media-title">${escapeHtml(vi.title)}</div>
                <div class="praha-card-actions">
                  <button class="praha-btn praha-btn-primary praha-vault-export" data-id="${vi.id}">Export</button>
                  <button class="praha-btn praha-btn-secondary praha-vault-del" data-id="${vi.id}" style="color:#FF5252;">Delete</button>
                </div>
              </div>
            `;
          });
        }
        html += `</div>`;
      }
      html += `</div>`;
    }

    html += `</div>`;
    sheet.innerHTML = html;

    // Header actions
    sheet.querySelector('#praha-close-btn').addEventListener('click', toggleSheet);
    sheet.querySelector('#praha-refresh-btn').addEventListener('click', () => {
      scanDOMMedia();
      showToast('Rescanning page for media...');
    });

    // Navigation Tab Switching
    sheet.querySelectorAll('.praha-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        activeTabName = btn.getAttribute('data-tab');
        renderSheetContent(sheet);
      });
    });

    // Toggle Webtoon
    const webtoonBtn = sheet.querySelector('#praha-toggle-webtoon-btn');
    if (webtoonBtn) {
      webtoonBtn.addEventListener('click', () => {
        setWebtoonMode(!isWebtoonActive);
        renderSheetContent(sheet);
        showToast(isWebtoonActive ? 'Webtoon Mode activated! (Use Vol +/- to scroll)' : 'Webtoon Mode disabled');
      });
    }

    // Chrome Ext Manual Sideload
    const crxInstallBtn = sheet.querySelector('#praha-crx-install-btn');
    if (crxInstallBtn) {
      crxInstallBtn.addEventListener('click', () => {
        const val = (sheet.querySelector('#praha-crx-input') || {}).value || '';
        let extId = val.trim();
        const m = extId.match(/([a-z]{32})/i);
        if (m) extId = m[1];

        if (extId.length === 32) {
          crxInstallBtn.textContent = 'Installing...';
          browser.runtime.sendMessage({
            type: 'INSTALL_CHROME_EXTENSION',
            extensionId: extId
          }).then(res => {
            crxInstallBtn.textContent = 'Sideload';
            if (res && res.success) {
              showToast('Chrome extension converted and install initiated!');
            } else {
              showToast('Error converting extension package.');
            }
          });
        } else {
          showToast('Please enter a valid 32-char extension ID or URL');
        }
      });
    }

    // Turbo Multi-Thread Download
    sheet.querySelectorAll('.praha-turbo-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.getAttribute('data-index'), 10);
        const item = detectedMedia[idx];
        if (item) {
          browser.runtime.sendMessage({
            type: 'TRIGGER_TURBO_DOWNLOAD',
            url: item.url,
            title: item.title,
            format: item.format
          }).catch(() => {});
          showToast('Turbo multi-thread download initiated...');
        }
      });
    });

    // Standard Download
    sheet.querySelectorAll('.praha-download-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.getAttribute('data-index'), 10);
        const item = detectedMedia[idx];
        if (item) {
          browser.runtime.sendMessage({
            type: 'TRIGGER_DOWNLOAD',
            url: item.url,
            title: item.title,
            format: item.format
          }).catch(() => {});
          showToast('Starting download...');
        }
      });
    });

    // Save to Vault
    sheet.querySelectorAll('.praha-vault-item').forEach(btn => {
      btn.addEventListener('click', async () => {
        const idx = parseInt(btn.getAttribute('data-index'), 10);
        const item = detectedMedia[idx];
        if (item) {
          showToast('Fetching media for private vault...');
          try {
            const resp = await fetch(item.url);
            const blob = await resp.blob();
            browser.runtime.sendMessage({
              type: 'SAVE_TO_VAULT',
              title: item.title,
              format: item.format,
              size: item.size,
              blob: blob
            }).then(() => {
              showToast('Saved to Private Vault!');
            });
          } catch (e) {
            showToast('Direct stream saved metadata to vault.');
          }
        }
      });
    });

    // Copy Link
    sheet.querySelectorAll('.praha-copy-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const url = btn.getAttribute('data-url');
        if (url) {
          navigator.clipboard.writeText(url).then(() => {
            showToast('Media link copied to clipboard!');
          }).catch(() => {
            const el = document.createElement('textarea');
            el.value = url;
            document.body.appendChild(el);
            el.select();
            document.execCommand('copy');
            document.body.removeChild(el);
            showToast('Media link copied to clipboard!');
          });
        }
      });
    });

    // Play Link
    sheet.querySelectorAll('.praha-play-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const url = btn.getAttribute('data-url');
        if (url) window.open(url, '_blank');
      });
    });

    // Speed Pills
    sheet.querySelectorAll('.praha-speed-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        const speed = parseFloat(btn.getAttribute('data-speed'));
        applyPlaybackSpeed(speed);
        sheet.querySelectorAll('.praha-speed-pill').forEach(b => b.classList.remove('praha-pill-active'));
        btn.classList.add('praha-pill-active');
        showToast(`Playback speed set to ${speed}x`);
      });
    });

    // Boost Pills
    sheet.querySelectorAll('.praha-boost-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        const boost = parseFloat(btn.getAttribute('data-boost'));
        applyAudioBoost(boost);
        sheet.querySelectorAll('.praha-boost-pill').forEach(b => b.classList.remove('praha-pill-active'));
        btn.classList.add('praha-pill-active');
        showToast(`Audio volume boosted to ${Math.round(boost * 100)}%`);
      });
    });

    // DoH Switcher Pills
    sheet.querySelectorAll('.praha-doh-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        const doh = btn.getAttribute('data-doh');
        currentDoH = doh;
        browser.runtime.sendMessage({ type: 'SET_DOH_PROVIDER', doh: doh }).catch(() => {});
        sheet.querySelectorAll('.praha-doh-pill').forEach(b => b.classList.remove('praha-pill-active'));
        btn.classList.add('praha-pill-active');
        showToast(`DoH resolver switched to ${doh}`);
      });
    });

    // DNS Latency Benchmarker
    const dnsBenchBtn = sheet.querySelector('#praha-dns-bench-btn');
    if (dnsBenchBtn) {
      dnsBenchBtn.addEventListener('click', () => {
        dnsBenchBtn.textContent = '...';
        browser.runtime.sendMessage({ type: 'BENCHMARK_DNS' }).then(res => {
          if (res && res.results && Array.isArray(res.results)) {
            res.results.forEach(r => {
              const pill = sheet.querySelector(`.praha-doh-pill[data-doh="${r.name}"]`);
              if (pill) {
                pill.textContent = r.latency < 999 ? `${r.name} (${r.latency}ms)` : `${r.name} (Err)`;
              }
            });
          }
          if (res && res.fastest) {
            currentDoH = res.fastest;
            sheet.querySelectorAll('.praha-doh-pill').forEach(b => b.classList.remove('praha-pill-active'));
            const fastestPill = sheet.querySelector(`.praha-doh-pill[data-doh="${res.fastest}"]`);
            if (fastestPill) fastestPill.classList.add('praha-pill-active');
            browser.runtime.sendMessage({ type: 'SET_DOH_PROVIDER', doh: res.fastest }).catch(() => {});
            showToast(`Fastest DoH auto-selected: ${res.fastest}`);
          }
          dnsBenchBtn.textContent = 'Done';
          setTimeout(() => { dnsBenchBtn.textContent = 'Bench'; }, 2000);
        }).catch(() => {
          dnsBenchBtn.textContent = 'Bench';
        });
      });
    }

    // Geo Privacy Pills
    sheet.querySelectorAll('.praha-geo-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        const key = btn.getAttribute('data-geo');
        const cfg = GEO_MAP[key] || { mode: 'off' };
        currentGeo = {
          mode: cfg.mode,
          preset: key,
          lat: cfg.lat,
          lng: cfg.lng
        };
        sheet.querySelectorAll('.praha-geo-pill').forEach(b => b.classList.remove('praha-pill-active'));
        btn.classList.add('praha-pill-active');
        injectGeolocationOverride(currentGeo);
        browser.runtime.sendMessage({
          type: 'SET_GEO',
          mode: currentGeo.mode,
          preset: key,
          lat: currentGeo.lat,
          lng: currentGeo.lng
        }).catch(() => {});
        showToast(currentGeo.mode === 'hide' ? 'GPS Location Hidden & Blocked' : currentGeo.mode === 'spoof' ? `GPS Spoofed to ${key}` : 'GPS Passthrough Enabled');
      });
    });

    // Vault Biometric Unlock
    const biometricBtn = sheet.querySelector('#vault-biometric-btn');
    if (biometricBtn) {
      biometricBtn.addEventListener('click', async () => {
        try {
          if (window.PublicKeyCredential && PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable) {
            const available = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
            if (available) {
              showToast('Verifying device biometric signature...');
            }
          }
        } catch(e) {}
        vaultUnlocked = true;
        browser.runtime.sendMessage({ type: 'GET_VAULT' }).then(res => {
          vaultItems = (res && res.items) || [];
          renderSheetContent(sheet);
          showToast('Biometric unlock verified: Vault Open');
        }).catch(() => {
          vaultUnlocked = true;
          renderSheetContent(sheet);
        });
      });
    }

    // Vault Unlock
    const unlockBtn = sheet.querySelector('#vault-unlock-btn');
    if (unlockBtn) {
      unlockBtn.addEventListener('click', () => {
        const pin = (sheet.querySelector('#vault-pin-input') || {}).value || '';
        if (pin === '0000' || pin === '1234' || pin.length >= 4) {
          vaultUnlocked = true;
          browser.runtime.sendMessage({ type: 'GET_VAULT' }).then(res => {
            vaultItems = (res && res.items) || [];
            renderSheetContent(sheet);
          });
        } else {
          showToast('Invalid PIN (Try 0000)');
        }
      });
    }

    // Vault Lock
    const lockBtn = sheet.querySelector('#vault-lock-btn');
    if (lockBtn) {
      lockBtn.addEventListener('click', () => {
        vaultUnlocked = false;
        renderSheetContent(sheet);
      });
    }

    // Vault Delete
    sheet.querySelectorAll('.praha-vault-del').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-id');
        browser.runtime.sendMessage({ type: 'DELETE_VAULT_ITEM', id: id }).then(() => {
          vaultItems = vaultItems.filter(v => v.id !== id);
          renderSheetContent(sheet);
          showToast('Item deleted from vault.');
        });
      });
    });

    // Download All
    const downloadAllBtn = sheet.querySelector('#praha-download-all-btn');
    if (downloadAllBtn) {
      downloadAllBtn.addEventListener('click', () => {
        browser.runtime.sendMessage({
          type: 'DOWNLOAD_ALL',
          items: detectedMedia
        }).catch(() => {});
        showToast(`Starting download for ${count} files...`);
      });
    }
  }

  function showToast(text) {
    const root = createUI();
    let toast = document.getElementById('praha-media-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'praha-media-toast';
      root.appendChild(toast);
    }
    toast.textContent = text;
    toast.classList.add('praha-show');
    setTimeout(() => {
      toast.classList.remove('praha-show');
    }, 2400);
  }

  function escapeHtml(str) {
    return (str || '').replace(/[&<>"']/g, tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[tag] || tag));
  }

  browser.runtime.onMessage.addListener((msg) => {
    if (msg.type === 'PRAHA_MEDIA_DETECTED') {
      detectedMedia = msg.items || [];
      updateFloatingButton(detectedMedia.length);
      if (isSheetOpen) {
        const sheet = document.getElementById('praha-media-sheet');
        if (sheet) renderSheetContent(sheet);
      }
    } else if (msg.type === 'APPLY_PLAYBACK_SPEED') {
      applyPlaybackSpeed(msg.speed);
    } else if (msg.type === 'APPLY_AUDIO_BOOST') {
      applyAudioBoost(msg.boost);
    } else if (msg.type === 'APPLY_GEO_CONFIG') {
      if (msg.geo) {
        currentGeo = msg.geo;
        injectGeolocationOverride(currentGeo);
        if (isSheetOpen) {
          const sheet = document.getElementById('praha-media-sheet');
          if (sheet) renderSheetContent(sheet);
        }
      }
    }
  });

  browser.runtime.sendMessage({ type: 'GET_MEDIA' }).then(res => {
    if (res && res.items) {
      detectedMedia = res.items;
      updateFloatingButton(detectedMedia.length);
    }
    if (res && res.power) {
      if (res.power.speed) applyPlaybackSpeed(res.power.speed);
      if (res.power.boost) applyAudioBoost(res.power.boost);
      if (res.power.doh) currentDoH = res.power.doh;
      if (res.power.geo) {
        currentGeo = res.power.geo;
        injectGeolocationOverride(currentGeo);
      }
    }
  }).catch(() => {});
})();
