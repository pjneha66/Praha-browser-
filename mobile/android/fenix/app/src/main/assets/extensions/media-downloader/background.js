// Praha Media & Power Suite - Background Service (v2.0.16)
const tabMediaMap = new Map();
const tabPowerStateMap = new Map();

// Default Geolocation and DoH Config
let defaultGeoConfig = { mode: 'off', preset: 'Tokyo', lat: 35.6762, lng: 139.6503 };
let defaultDoHProvider = 'Cloudflare';

browser.storage.local.get(['praha_geo_config', 'praha_doh_provider']).then(res => {
  if (res.praha_geo_config) defaultGeoConfig = res.praha_geo_config;
  if (res.praha_doh_provider) defaultDoHProvider = res.praha_doh_provider;
}).catch(() => {});

// 1. Chrome Extension (.crx) to Firefox (.xpi) Converter & Installer
async function convertCrxToXpi(crxArrayBuffer, extensionId) {
  const uint8 = new Uint8Array(crxArrayBuffer);
  // Find zip header magic bytes PK\x03\x04 (0x50, 0x4B, 0x03, 0x04)
  let zipOffset = -1;
  for (let i = 0; i < Math.min(uint8.length - 4, 150000); i++) {
    if (uint8[i] === 0x50 && uint8[i+1] === 0x4B && uint8[i+2] === 0x03 && uint8[i+3] === 0x04) {
      zipOffset = i;
      break;
    }
  }

  const zipData = zipOffset >= 0 ? uint8.slice(zipOffset) : uint8;
  if (typeof JSZip === 'undefined') {
    throw new Error('JSZip library not available');
  }

  const zip = await JSZip.loadAsync(zipData);
  const manifestFile = zip.file('manifest.json');
  let extName = 'Praha_Extension';

  if (manifestFile) {
    const manifestText = await manifestFile.async('string');
    try {
      const manifest = JSON.parse(manifestText);
      if (manifest.name) {
        extName = manifest.name.replace(/[\\/:*?"<>|]/g, '_').replace(/\s+/g, '_');
      }

      const safeId = (extensionId || 'crx_' + Math.random().toString(36).substring(2, 9))
        .replace(/[^a-zA-Z0-9_-]/g, '') + '@prahabrowser.org';

      manifest.browser_specific_settings = manifest.browser_specific_settings || {};
      manifest.browser_specific_settings.gecko = manifest.browser_specific_settings.gecko || {};
      manifest.browser_specific_settings.gecko.id = safeId;
      manifest.browser_specific_settings.gecko.strict_min_version = '100.0';

      zip.file('manifest.json', JSON.stringify(manifest, null, 2));
    } catch (e) {}
  }

  const xpiBlob = await zip.generateAsync({
    type: 'blob',
    mimeType: 'application/x-xpinstall'
  });

  return { blob: xpiBlob, name: extName + '.xpi' };
}

async function installChromeExtensionById(extensionId, tabId) {
  const crxUrl = `https://clients2.google.com/service/update2/crx?response=redirect&prodversion=120.0&acceptformat=crx2,crx3&x=id%3D${encodeURIComponent(extensionId)}%26uc`;
  const response = await fetch(crxUrl);
  if (!response.ok) {
    throw new Error('Failed to fetch CRX package from Chrome Web Store');
  }

  const arrayBuffer = await response.arrayBuffer();
  const { blob, name } = await convertCrxToXpi(arrayBuffer, extensionId);
  const xpiUrl = URL.createObjectURL(blob);

  if (tabId) {
    browser.tabs.executeScript(tabId, {
      code: `(() => {
        const a = document.createElement('a');
        a.href = ${JSON.stringify(xpiUrl)};
        a.download = ${JSON.stringify(name)};
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        setTimeout(() => a.remove(), 2000);
      })()`
    });
  } else {
    browser.tabs.create({ url: xpiUrl });
  }
}

// 2. IndexedDB Private Media Vault
const DB_NAME = 'PrahaMediaVault';
const DB_STORE = 'vault_items';

function openVaultDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = (e) => {
      const db = e.target.result;
      if (!db.objectStoreNames.contains(DB_STORE)) {
        db.createObjectStore(DB_STORE, { keyPath: 'id' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function saveItemToVault(item) {
  const db = await openVaultDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(DB_STORE, 'readwrite');
    const store = tx.objectStore(DB_STORE);
    store.put(item);
    tx.oncomplete = () => resolve(true);
    tx.onerror = () => reject(tx.error);
  });
}

async function getVaultItems() {
  const db = await openVaultDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(DB_STORE, 'readonly');
    const store = tx.objectStore(DB_STORE);
    const req = store.getAll();
    req.onsuccess = () => {
      const items = (req.result || []).map(i => ({
        id: i.id,
        title: i.title,
        format: i.format,
        size: i.size,
        timestamp: i.timestamp
      }));
      resolve(items);
    };
    req.onerror = () => reject(req.error);
  });
}

async function deleteVaultItem(id) {
  const db = await openVaultDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(DB_STORE, 'readwrite');
    const store = tx.objectStore(DB_STORE);
    store.delete(id);
    tx.oncomplete = () => resolve(true);
    tx.onerror = () => reject(tx.error);
  });
}

// 3. Parallel Chunk Download Accelerator
async function downloadParallel(url, filename, tabId) {
  try {
    const headRes = await fetch(url, { method: 'HEAD' });
    const contentLength = parseInt(headRes.headers.get('content-length'), 10) || 0;
    const acceptRanges = headRes.headers.get('accept-ranges') || '';

    if (contentLength > 2 * 1024 * 1024 && acceptRanges.includes('bytes')) {
      const numChunks = 4;
      const chunkSize = Math.floor(contentLength / numChunks);
      const chunkPromises = [];

      for (let i = 0; i < numChunks; i++) {
        const start = i * chunkSize;
        const end = i === numChunks - 1 ? contentLength - 1 : (start + chunkSize - 1);
        chunkPromises.push(
          fetch(url, { headers: { 'Range': `bytes=${start}-${end}` } })
            .then(r => r.arrayBuffer())
        );
      }

      const buffers = await Promise.all(chunkPromises);
      const combinedBlob = new Blob(buffers, { type: 'application/octet-stream' });
      const blobUrl = URL.createObjectURL(combinedBlob);

      if (tabId) {
        browser.tabs.executeScript(tabId, {
          code: `(() => {
            const a = document.createElement('a');
            a.href = ${JSON.stringify(blobUrl)};
            a.download = ${JSON.stringify(filename)};
            a.target = '_blank';
            document.body.appendChild(a);
            a.click();
            setTimeout(() => { a.remove(); URL.revokeObjectURL(${JSON.stringify(blobUrl)}); }, 10000);
          })()`
        });
      }
      return;
    }
  } catch (e) {}

  // Fallback to direct stream trigger
  if (tabId) {
    browser.tabs.executeScript(tabId, {
      code: `(() => {
        const a = document.createElement('a');
        a.href = ${JSON.stringify(url)};
        a.download = ${JSON.stringify(filename)};
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        setTimeout(() => a.remove(), 1000);
      })()`
    });
  }
}

// 4. Media Format & Extension Filters
const MEDIA_EXTS = /\.(mp4|m3u8|mpd|webm|mkv|flv|m4v|mov|avi|ts|3gp|mp3|m4a|aac|ogg|wav|flac|opus)(\?.*)?$/i;
const MEDIA_MIMES = [
  'video/mp4', 'video/webm', 'video/ogg', 'video/quicktime', 'video/x-matroska',
  'video/x-flv', 'video/3gpp', 'video/mp2t', 'video/x-m4v',
  'application/x-mpegurl', 'application/vnd.apple.mpegurl', 'application/dash+xml',
  'audio/mpeg', 'audio/mp4', 'audio/ogg', 'audio/wav', 'audio/webm', 'audio/aac', 'audio/flac'
];

function getFormat(url, mime) {
  const clean = (url || '').split('?')[0].toLowerCase();
  if (clean.endsWith('.m3u8') || (mime && mime.includes('mpegurl'))) return 'M3U8';
  if (clean.endsWith('.mpd') || (mime && mime.includes('dash+xml'))) return 'DASH';
  if (clean.endsWith('.mp4') || (mime && mime.includes('mp4'))) return 'MP4';
  if (clean.endsWith('.webm') || (mime && mime.includes('webm'))) return 'WEBM';
  if (clean.endsWith('.mkv')) return 'MKV';
  if (clean.endsWith('.ts')) return 'TS';
  if (clean.endsWith('.mp3') || (mime && mime.includes('audio/mpeg'))) return 'MP3';
  if (clean.endsWith('.m4a') || (mime && mime.includes('audio/mp4'))) return 'M4A';
  if (clean.endsWith('.aac')) return 'AAC';
  if (clean.endsWith('.ogg')) return 'OGG';
  if (clean.endsWith('.wav')) return 'WAV';
  if (clean.endsWith('.flac')) return 'FLAC';
  if (clean.endsWith('.opus')) return 'OPUS';
  if (mime && mime.startsWith('video/')) return 'VIDEO';
  if (mime && mime.startsWith('audio/')) return 'AUDIO';
  return 'MEDIA';
}

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return 'Live / Stream';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function cleanFilename(title, format) {
  let name = (title || 'Praha_Media')
    .replace(/[\\/:*?"<>|]/g, '_')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 100);
  const ext = (format || 'mp4').toLowerCase();
  if (!name.toLowerCase().endsWith('.' + ext)) {
    name += '.' + (ext === 'm3u8' ? 'm3u8' : ext === 'stream' ? 'mp4' : ext);
  }
  return name;
}

function addMedia(tabId, item) {
  if (!tabId || tabId < 0) return;
  if (!tabMediaMap.has(tabId)) {
    tabMediaMap.set(tabId, []);
  }
  const list = tabMediaMap.get(tabId);
  if (list.some(m => m.url === item.url)) return;

  if (item.sizeBytes > 0 && item.sizeBytes < 40960 && item.format !== 'M3U8') {
    return;
  }

  list.push(item);
  updateBadge(tabId);
  notifyTab(tabId);
}

function updateBadge(tabId) {
  const list = tabMediaMap.get(tabId) || [];
  const count = list.length;
  if (count > 0) {
    browser.browserAction.setBadgeText({ tabId, text: String(count) });
    browser.browserAction.setBadgeBackgroundColor({ tabId, color: '#7C4DFF' });
  } else {
    browser.browserAction.setBadgeText({ tabId, text: '' });
  }
}

function notifyTab(tabId) {
  const list = tabMediaMap.get(tabId) || [];
  browser.tabs.sendMessage(tabId, {
    type: 'PRAHA_MEDIA_DETECTED',
    count: list.length,
    items: list
  }).catch(() => {});
}

// 5. Bang Search & Magnet Interceptor
const BANG_MAP = [
  { prefix: '!c ', url: 'https://batcave.biz/?s=' },
  { prefix: '!comics ', url: 'https://batcave.biz/?s=' },
  { prefix: '!gc ', url: 'https://getcomics.org/?s=' },
  { prefix: '!m ', url: 'https://www.movy.sx/search?q=' },
  { prefix: '!movie ', url: 'https://www.movy.sx/search?q=' },
  { prefix: '!a ', url: 'https://gotaku.to/search?keyword=' },
  { prefix: '!anime ', url: 'https://gotaku.to/search?keyword=' },
  { prefix: '!b ', url: 'https://oceanofpdf.com/?s=' },
  { prefix: '!book ', url: 'https://oceanofpdf.com/?s=' },
  { prefix: '!aa ', url: 'https://annas-archive.gl/search?q=' },
  { prefix: '!mn ', url: 'https://mangadot.net/search?q=' },
  { prefix: '!manga ', url: 'https://mangadot.net/search?q=' },
  { prefix: '!yt ', url: 'https://www.youtube.com/results?search_query=' },
  { prefix: '!w ', url: 'https://en.wikipedia.org/wiki/Special:Search?search=' },
  { prefix: '!r ', url: 'https://www.reddit.com/search/?q=' }
];

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    try {
      const url = details.url;
      // Intercept magnet links
      if (url.startsWith('magnet:?')) {
        const streamUrl = browser.runtime.getURL('webtorrent.html') + '?magnet=' + encodeURIComponent(url);
        return { redirectUrl: streamUrl };
      }

      // Intercept direct .crx downloads
      if (url.endsWith('.crx') && !url.includes('clients2.google.com/service/update2/crx')) {
        fetch(url)
          .then(r => r.arrayBuffer())
          .then(buf => convertCrxToXpi(buf, 'sideloaded_ext'))
          .then(({ blob, name }) => {
            const xpiUrl = URL.createObjectURL(blob);
            if (details.tabId && details.tabId >= 0) {
              browser.tabs.executeScript(details.tabId, {
                code: `(() => {
                  const a = document.createElement('a');
                  a.href = ${JSON.stringify(xpiUrl)};
                  a.download = ${JSON.stringify(name)};
                  a.target = '_blank';
                  document.body.appendChild(a);
                  a.click();
                  setTimeout(() => a.remove(), 2000);
                })()`
              });
            }
          }).catch(() => {});
      }

      const u = new URL(url);
      const q = u.searchParams.get('q') || u.searchParams.get('query');
      if (q) {
        const trimmed = q.trim();
        for (const b of BANG_MAP) {
          if (trimmed.startsWith(b.prefix)) {
            const query = trimmed.substring(b.prefix.length).trim();
            if (query) {
              return { redirectUrl: b.url + encodeURIComponent(query) };
            }
          }
        }
      }
    } catch (e) {}
    return {};
  },
  {
    urls: [
      '*://*.google.com/search*',
      '*://duckduckgo.com/*',
      '*://*.bing.com/search*',
      '*://search.brave.com/search*',
      '*://*.ecosia.org/search*',
      'magnet:*',
      '*://*/*.crx*'
    ],
    types: ['main_frame', 'other']
  },
  ['blocking']
);

// 6. Bypass Paywalls Header Cleaner
const PAYWALL_DOMAINS = [
  'medium.com', 'bloomberg.com', 'nytimes.com', 'washingtonpost.com',
  'economist.com', 'wsj.com', 'theatlantic.com', 'wired.com',
  'newyorker.com', 'businessinsider.com', 'quora.com', 'scribd.com'
];

browser.webRequest.onBeforeSendHeaders.addListener(
  (details) => {
    let host = '';
    try { host = new URL(details.url).hostname; } catch (e) {}
    const isPaywallDomain = PAYWALL_DOMAINS.some(d => host.endsWith(d));

    if (isPaywallDomain) {
      let headers = details.requestHeaders || [];
      let hasReferer = false;
      headers.forEach(h => {
        if (h.name.toLowerCase() === 'referer') {
          h.value = 'https://www.google.com/';
          hasReferer = true;
        }
      });
      if (!hasReferer) {
        headers.push({ name: 'Referer', value: 'https://www.google.com/' });
      }
      return { requestHeaders: headers };
    }
    return {};
  },
  { urls: ['<all_urls>'], types: ['main_frame', 'sub_frame'] },
  ['blocking', 'requestHeaders']
);

// 7. Media Network Interception
browser.webRequest.onResponseStarted.addListener(
  (details) => {
    if (!details.tabId || details.tabId < 0) return;
    const url = details.url;
    if (!url || url.startsWith('about:') || url.startsWith('resource:') || url.startsWith('chrome:')) return;

    let mime = '';
    let contentLength = 0;
    if (details.responseHeaders) {
      for (const h of details.responseHeaders) {
        const name = (h.name || '').toLowerCase();
        if (name === 'content-type') mime = (h.value || '').toLowerCase();
        if (name === 'content-length') contentLength = parseInt(h.value, 10) || 0;
      }
    }

    const isMediaExt = MEDIA_EXTS.test(url);
    const isMediaMime = MEDIA_MIMES.some(m => mime.startsWith(m));
    const isStreamQuery = /videoplayback|master\.m3u8|index\.m3u8|playlist\.m3u8|\/stream\/|manifest\.mpd/i.test(url);

    if (isMediaExt || isMediaMime || isStreamQuery) {
      const format = getFormat(url, mime);
      const isVideo = !['MP3', 'M4A', 'AAC', 'WAV', 'OGG', 'FLAC', 'OPUS'].includes(format);

      let title = '';
      try {
        const u = new URL(url);
        const parts = u.pathname.split('/');
        title = decodeURIComponent(parts[parts.length - 1] || 'media');
      } catch (e) {
        title = 'media_' + Date.now();
      }

      addMedia(details.tabId, {
        id: 'req_' + Math.random().toString(36).substring(2, 9),
        url: url,
        title: title,
        format: format,
        type: isVideo ? 'video' : 'audio',
        size: formatBytes(contentLength),
        sizeBytes: contentLength,
        timestamp: Date.now()
      });
    }
  },
  { urls: ['<all_urls>'] },
  ['responseHeaders']
);

browser.tabs.onRemoved.addListener((tabId) => {
  tabMediaMap.delete(tabId);
  tabPowerStateMap.delete(tabId);
});

browser.tabs.onUpdated.addListener((tabId, changeInfo) => {
  if (changeInfo.status === 'loading') {
    tabMediaMap.delete(tabId);
    updateBadge(tabId);
    notifyTab(tabId);
  }
});

browser.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  const tabId = (sender.tab && sender.tab.id) || msg.tabId;

  if (msg.type === 'GET_MEDIA') {
    const list = tabMediaMap.get(tabId) || [];
    const power = tabPowerStateMap.get(tabId) || {
      speed: 1.0,
      boost: 1.0,
      doh: defaultDoHProvider,
      geo: defaultGeoConfig
    };
    sendResponse({ count: list.length, items: list, power: power });
    return true;
  }

  if (msg.type === 'DOM_MEDIA_FOUND') {
    if (msg.items && Array.isArray(msg.items)) {
      for (const item of msg.items) {
        addMedia(tabId, item);
      }
    }
    sendResponse({ success: true });
    return true;
  }

  if (msg.type === 'INSTALL_CHROME_EXTENSION') {
    installChromeExtensionById(msg.extensionId, tabId)
      .then(() => sendResponse({ success: true }))
      .catch(err => sendResponse({ success: false, error: err.message }));
    return true;
  }

  if (msg.type === 'SET_PLAYBACK_SPEED') {
    const state = tabPowerStateMap.get(tabId) || { speed: 1.0, boost: 1.0, doh: defaultDoHProvider, geo: defaultGeoConfig };
    state.speed = msg.speed;
    tabPowerStateMap.set(tabId, state);

    if (tabId) {
      browser.tabs.sendMessage(tabId, {
        type: 'APPLY_PLAYBACK_SPEED',
        speed: msg.speed
      }).catch(() => {});
    }
    sendResponse({ success: true, speed: msg.speed });
    return true;
  }

  if (msg.type === 'SET_AUDIO_BOOST') {
    const state = tabPowerStateMap.get(tabId) || { speed: 1.0, boost: 1.0, doh: defaultDoHProvider, geo: defaultGeoConfig };
    state.boost = msg.boost;
    tabPowerStateMap.set(tabId, state);

    if (tabId) {
      browser.tabs.sendMessage(tabId, {
        type: 'APPLY_AUDIO_BOOST',
        boost: msg.boost
      }).catch(() => {});
    }
    sendResponse({ success: true, boost: msg.boost });
    return true;
  }

  if (msg.type === 'SET_DOH_PROVIDER') {
    const state = tabPowerStateMap.get(tabId) || { speed: 1.0, boost: 1.0, doh: defaultDoHProvider, geo: defaultGeoConfig };
    state.doh = msg.doh;
    defaultDoHProvider = msg.doh;
    tabPowerStateMap.set(tabId, state);
    browser.storage.local.set({ praha_doh_provider: msg.doh });
    sendResponse({ success: true, doh: msg.doh });
    return true;
  }

  if (msg.type === 'SET_GEO') {
    const state = tabPowerStateMap.get(tabId) || { speed: 1.0, boost: 1.0, doh: defaultDoHProvider, geo: defaultGeoConfig };
    state.geo = {
      mode: msg.mode || 'off',
      preset: msg.preset || 'Tokyo',
      lat: typeof msg.lat === 'number' ? msg.lat : 35.6762,
      lng: typeof msg.lng === 'number' ? msg.lng : 139.6503
    };
    defaultGeoConfig = state.geo;
    tabPowerStateMap.set(tabId, state);
    browser.storage.local.set({ praha_geo_config: state.geo });

    if (tabId) {
      browser.tabs.sendMessage(tabId, {
        type: 'APPLY_GEO_CONFIG',
        geo: state.geo
      }).catch(() => {});
    }
    sendResponse({ success: true, geo: state.geo });
    return true;
  }

  if (msg.type === 'BENCHMARK_DNS') {
    const dnsEndpoints = [
      { name: 'Cloudflare', url: 'https://cloudflare-dns.com/dns-query?name=cloudflare.com&type=A' },
      { name: 'AdGuard', url: 'https://dns.adguard-dns.com/resolve?name=adguard-dns.io&type=A' },
      { name: 'Google', url: 'https://dns.google/resolve?name=google.com&type=A' },
      { name: 'Mullvad', url: 'https://doh.mullvad.net/dns-query?name=mullvad.net&type=A' },
      { name: 'Quad9', url: 'https://dns.quad9.net:5053/dns-query?name=quad9.net&type=A' }
    ];

    const measureEndpoint = async (ep) => {
      const t0 = performance.now();
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 4000);
        const res = await fetch(ep.url, {
          method: 'GET',
          headers: { 'Accept': 'application/dns-json' },
          signal: controller.signal
        });
        clearTimeout(timeoutId);
        const duration = Math.max(1, Math.round(performance.now() - t0));
        return { name: ep.name, latency: duration, ok: res.ok };
      } catch (err) {
        return { name: ep.name, latency: 999, ok: false };
      }
    };

    Promise.all(dnsEndpoints.map(measureEndpoint)).then(results => {
      let fastest = results[0];
      for (const r of results) {
        if (r.ok && (fastest.latency === 999 || r.latency < fastest.latency)) {
          fastest = r;
        }
      }
      sendResponse({ results: results, fastest: fastest ? fastest.name : 'Cloudflare' });
    }).catch(() => {
      sendResponse({ results: [], fastest: 'Cloudflare' });
    });
    return true;
  }

  if (msg.type === 'TRIGGER_TURBO_DOWNLOAD') {
    const targetUrl = msg.url;
    const targetTitle = cleanFilename(msg.title, msg.format || 'mp4');
    downloadParallel(targetUrl, targetTitle, tabId);
    sendResponse({ success: true });
    return true;
  }

  if (msg.type === 'TRIGGER_DOWNLOAD') {
    const targetUrl = msg.url;
    const targetTitle = cleanFilename(msg.title, msg.format || 'mp4');

    if (tabId) {
      browser.tabs.executeScript(tabId, {
        code: `(() => {
          const a = document.createElement('a');
          a.href = ${JSON.stringify(targetUrl)};
          a.download = ${JSON.stringify(targetTitle)};
          a.target = '_blank';
          document.body.appendChild(a);
          a.click();
          setTimeout(() => a.remove(), 1000);
        })()`
      }).catch(() => {
        browser.tabs.create({ url: targetUrl });
      });
    } else {
      browser.tabs.create({ url: targetUrl });
    }
    sendResponse({ success: true });
    return true;
  }

  if (msg.type === 'SAVE_TO_VAULT') {
    saveItemToVault({
      id: 'vault_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
      title: msg.title || 'Private Media',
      format: msg.format || 'mp4',
      size: msg.size || 'Saved',
      blob: msg.blob,
      timestamp: Date.now()
    }).then(() => {
      sendResponse({ success: true });
    }).catch(err => {
      sendResponse({ success: false, error: err.message });
    });
    return true;
  }

  if (msg.type === 'GET_VAULT') {
    getVaultItems().then(items => {
      sendResponse({ items: items });
    }).catch(() => {
      sendResponse({ items: [] });
    });
    return true;
  }

  if (msg.type === 'DELETE_VAULT_ITEM') {
    deleteVaultItem(msg.id).then(() => {
      sendResponse({ success: true });
    });
    return true;
  }

  if (msg.type === 'DOWNLOAD_ALL') {
    const items = msg.items || [];
    items.forEach((item, index) => {
      setTimeout(() => {
        const title = cleanFilename(item.title, item.format);
        if (tabId) {
          browser.tabs.executeScript(tabId, {
            code: `(() => {
              const a = document.createElement('a');
              a.href = ${JSON.stringify(item.url)};
              a.download = ${JSON.stringify(title)};
              a.target = '_blank';
              document.body.appendChild(a);
              a.click();
              setTimeout(() => a.remove(), 1000);
            })()`
          }).catch(() => {
            browser.tabs.create({ url: item.url });
          });
        } else {
          browser.tabs.create({ url: item.url });
        }
      }, index * 800);
    });
    sendResponse({ success: true });
    return true;
  }
});
