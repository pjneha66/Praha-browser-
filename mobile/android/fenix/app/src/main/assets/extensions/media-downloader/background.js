// Praha Media & Power Suite - Background Service (v2.0.17)
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
  if (!crxArrayBuffer || crxArrayBuffer.byteLength < 16) {
    throw new Error('Truncated CRX archive (under 16 bytes)');
  }
  if (crxArrayBuffer.byteLength > 50 * 1024 * 1024) {
    throw new Error('CRX file exceeds 50MB size limit');
  }

  const uint8 = new Uint8Array(crxArrayBuffer);
  let zipOffset = -1;

  // Header bounds checking: check for 'Cr24' magic (0x43, 0x72, 0x32, 0x34)
  if (uint8[0] === 0x43 && uint8[1] === 0x72 && uint8[2] === 0x32 && uint8[3] === 0x34) {
    const view = new DataView(crxArrayBuffer);
    const version = view.getUint32(4, true);
    if (version === 2) {
      const pubKeyLen = view.getUint32(8, true);
      const sigLen = view.getUint32(12, true);
      const headerLen = 16 + pubKeyLen + sigLen;
      if (headerLen > crxArrayBuffer.byteLength) {
        throw new Error('Truncated or malformed CRX2 archive header');
      }
      zipOffset = headerLen;
    } else if (version === 3) {
      const headerLen = view.getUint32(8, true);
      const totalHeader = 12 + headerLen;
      if (totalHeader > crxArrayBuffer.byteLength) {
        throw new Error('Truncated or malformed CRX3 archive header');
      }
      zipOffset = totalHeader;
    }
  }

  // Fallback to scanning for ZIP magic PK\x03\x04 if zipOffset not determined or not aligned
  if (zipOffset < 0 || zipOffset > uint8.length - 4 ||
      !(uint8[zipOffset] === 0x50 && uint8[zipOffset+1] === 0x4B && uint8[zipOffset+2] === 0x03 && uint8[zipOffset+3] === 0x04)) {
    const scanStart = zipOffset >= 0 ? zipOffset : 0;
    zipOffset = -1;
    for (let i = scanStart; i < Math.min(uint8.length - 4, 150000); i++) {
      if (uint8[i] === 0x50 && uint8[i+1] === 0x4B && uint8[i+2] === 0x03 && uint8[i+3] === 0x04) {
        zipOffset = i;
        break;
      }
    }
  }

  if (zipOffset < 0) {
    throw new Error('Invalid CRX: ZIP header not found');
  }

  const zipData = uint8.slice(zipOffset);
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
  if (!extensionId || !/^[a-z]{32}$/i.test(extensionId)) {
    throw new Error('Invalid extension ID format (must be 32 alphabetic characters)');
  }
  return fetchCrxAndPrepareInstall(extensionId, tabId);
}

async function fetchCrxAndPrepareInstall(extensionId, tabId) {
  if (!tabId || tabId < 0) {
    throw new Error('Valid tabId required for CRX installation prompt');
  }
  const crxUrl = `https://clients2.google.com/service/update2/crx?response=redirect&prodversion=120.0&acceptformat=crx2,crx3&x=id%3D${encodeURIComponent(extensionId)}%26uc`;
  const response = await fetch(crxUrl);
  if (!response.ok) {
    throw new Error('Failed to fetch CRX package from Chrome Web Store');
  }

  const arrayBuffer = await response.arrayBuffer();
  if (arrayBuffer.byteLength > 50 * 1024 * 1024) {
    throw new Error('CRX file exceeds 50MB limit');
  }

  const { blob, name } = await convertCrxToXpi(arrayBuffer, extensionId);
  const pendingId = 'crx_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
  const timer = setTimeout(() => {
    pendingCrxInstalls.delete(pendingId);
  }, 300000);

  pendingCrxInstalls.set(pendingId, { blob, name, timer, tabId });

  await browser.tabs.sendMessage(tabId, {
    type: 'PROMPT_CRX_CONFIRM',
    pendingId,
    name
  }).catch((err) => {
    clearTimeout(timer);
    pendingCrxInstalls.delete(pendingId);
    throw err;
  });
  return { pendingId, name };
}

// 2. IndexedDB Private Media Vault
const DB_NAME = 'PrahaMediaVault';
const DB_STORE = 'vault_items';

// Vault Security: WebCrypto PBKDF2 Authentication, Session Auto-Lock & Persistent Lockout
let vaultUnlocked = false;
let vaultAutoLockTimer = null;
const AUTO_LOCK_TIMEOUT_MS = 300000; // 5-minute inactivity auto-lock

function setVaultAutoLock() {
  clearTimeout(vaultAutoLockTimer);
  vaultAutoLockTimer = setTimeout(() => {
    vaultUnlocked = false;
  }, AUTO_LOCK_TIMEOUT_MS);
}

async function getVaultLockoutState() {
  const res = await browser.storage.local.get(['praha_vault_lockout']).catch(() => ({}));
  return res.praha_vault_lockout || { failedAttempts: 0, lockoutUntil: 0, lockoutCount: 0 };
}

async function setVaultLockoutState(state) {
  await browser.storage.local.set({ praha_vault_lockout: state }).catch(() => {});
}

function timingSafeEqualHex(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false;
  let mismatch = 0;
  for (let i = 0; i < a.length; i++) {
    mismatch |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return mismatch === 0;
}

async function hashVaultPin(pin, saltHex) {
  const enc = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    enc.encode(pin),
    { name: 'PBKDF2' },
    false,
    ['deriveBits']
  );
  let salt;
  if (saltHex) {
    const matches = saltHex.match(/.{1,2}/g) || [];
    salt = new Uint8Array(matches.map(byte => parseInt(byte, 16)));
  } else {
    salt = crypto.getRandomValues(new Uint8Array(16));
    saltHex = Array.from(salt).map(b => b.toString(16).padStart(2, '0')).join('');
  }
  // Domain-separated verifier derivation using a distinct info string
  const verifierSalt = new Uint8Array([...salt, ...enc.encode('praha-vault-verifier-v1')]);
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: verifierSalt,
      iterations: 100000,
      hash: 'SHA-256'
    },
    keyMaterial,
    256
  );
  const hashHex = Array.from(new Uint8Array(derivedBits)).map(b => b.toString(16).padStart(2, '0')).join('');
  return { hash: hashHex, salt: saltHex };
}

async function getVaultAuth() {
  const res = await browser.storage.local.get(['praha_vault_auth', 'praha_vault_pin', 'praha_vault_salt']);
  if (res.praha_vault_auth && res.praha_vault_auth.hash && res.praha_vault_auth.salt) {
    return res.praha_vault_auth;
  }
  // Migration path: legacy builds stored the raw PIN; re-derive a proper verifier from it
  if (typeof res.praha_vault_pin === 'string' && /^\d{4,8}$/.test(res.praha_vault_pin.trim())) {
    const migrated = await hashVaultPin(res.praha_vault_pin.trim());
    await browser.storage.local.set({ praha_vault_auth: migrated });
    await browser.storage.local.remove(['praha_vault_pin', 'praha_vault_salt']).catch(() => {});
    return migrated;
  }
  // A present-but-malformed auth record must fail closed: never treat it as "no PIN set"
  if (res.praha_vault_auth) {
    throw new Error('Vault auth data is corrupted');
  }
  return null;
}

// Serializes vault auth state transitions so concurrent messages cannot
// interleave the read-modify-write of the lockout counter
let vaultAuthQueue = Promise.resolve();
function withVaultAuthLock(task) {
  const result = vaultAuthQueue.then(task, task);
  vaultAuthQueue = result.then(() => {}, () => {});
  return result;
}

// Active Blob Downloads Management (Revocation on Download Completion)
const activeBlobDownloads = new Map(); // downloadId -> { url, timer }
const pendingCrxInstalls = new Map();  // pendingId -> { blob, name, timer }

if (browser.downloads && browser.downloads.onChanged) {
  browser.downloads.onChanged.addListener((delta) => {
    if (activeBlobDownloads.has(delta.id)) {
      if (delta.state && (delta.state.current === 'complete' || delta.state.current === 'interrupted')) {
        const entry = activeBlobDownloads.get(delta.id);
        activeBlobDownloads.delete(delta.id);
        if (entry) {
          clearTimeout(entry.timer);
          try { URL.revokeObjectURL(entry.url); } catch (e) {}
        }
      }
    }
  });
}

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

async function getVaultItemById(id) {
  const db = await openVaultDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(DB_STORE, 'readonly');
    const store = tx.objectStore(DB_STORE);
    const req = store.get(id);
    req.onsuccess = () => resolve(req.result || null);
    req.onerror = () => reject(req.error);
  });
}

// 3. Parallel Chunk Download Accelerator
async function downloadParallel(url, filename, tabId) {
  try {
    const headRes = await fetch(url, { method: 'HEAD' });
    const contentLength = parseInt(headRes.headers.get('content-length'), 10) || 0;
    const acceptRanges = headRes.headers.get('accept-ranges') || '';

    // Enforce 2MB - 250MB boundaries for parallel chunk acceleration
    if (contentLength > 2 * 1024 * 1024 && contentLength <= 250 * 1024 * 1024 && acceptRanges.includes('bytes')) {
      const numChunks = 4;
      const chunkSize = Math.floor(contentLength / numChunks);
      const chunkPromises = [];

      for (let i = 0; i < numChunks; i++) {
        const start = i * chunkSize;
        const end = i === numChunks - 1 ? contentLength - 1 : (start + chunkSize - 1);
        chunkPromises.push(
          fetch(url, { headers: { 'Range': `bytes=${start}-${end}` } })
            .then(r => {
              if (r.status !== 206) {
                throw new Error(`Non-206 status received for range chunk (${r.status})`);
              }
              return r.arrayBuffer();
            })
        );
      }

      const buffers = await Promise.all(chunkPromises);
      const combinedBlob = new Blob(buffers, { type: 'application/octet-stream' });
      const blobUrl = URL.createObjectURL(combinedBlob);
      const fallbackTimer = setTimeout(() => {
        try { URL.revokeObjectURL(blobUrl); } catch (e) {}
      }, 300000);

      if (browser.downloads && browser.downloads.download) {
        try {
          const downloadId = await browser.downloads.download({
            url: blobUrl,
            filename: filename,
            saveAs: false
          });
          if (downloadId) {
            activeBlobDownloads.set(downloadId, { url: blobUrl, timer: fallbackTimer });
          }
          return;
        } catch (downloadErr) {
          clearTimeout(fallbackTimer);
          try { URL.revokeObjectURL(blobUrl); } catch (e) {}
          throw downloadErr;
        }
      } else {
        browser.tabs.create({ url: blobUrl });
        return;
      }
    }
  } catch (err) {
    console.warn('Parallel download error, falling back to direct stream:', err.message || err);
  }

  // Fallback to direct stream trigger via browser.downloads.download
  if (browser.downloads && browser.downloads.download) {
    try {
      await browser.downloads.download({
        url: url,
        filename: filename,
        saveAs: false
      });
      return;
    } catch (e) {
      console.warn('Direct browser download error:', e.message || e);
    }
  }

  // Final fallback: open target URL in a tab without injecting anchor scripts into the page
  browser.tabs.create({ url: url });
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

      // Intercept direct .crx downloads (handling query params, tokens, size cap and confirmation)
      let pathname = '';
      try { pathname = new URL(url).pathname.toLowerCase(); } catch (e) {}
      if (pathname.endsWith('.crx') && !url.includes('clients2.google.com/service/update2/crx')) {
        if (!details.tabId || details.tabId < 0) {
          // Reject direct installation without a valid tab context
          return { cancel: true };
        }
        const extSlug = pathname.split('/').pop().replace(/\.crx$/i, '') || 'sideloaded_ext';
        fetch(url)
          .then(async (r) => {
            if (!r.ok) throw new Error('Download request failed');
            const cl = parseInt(r.headers.get('content-length') || '0', 10);
            if (cl > 50 * 1024 * 1024) throw new Error('CRX file exceeds 50MB limit');
            return r.arrayBuffer();
          })
          .then(buf => convertCrxToXpi(buf, extSlug))
          .then(({ blob, name }) => {
            const pendingId = 'crx_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
            const timer = setTimeout(() => {
              pendingCrxInstalls.delete(pendingId);
            }, 300000);
            pendingCrxInstalls.set(pendingId, { blob, name, timer, tabId: details.tabId });

            browser.tabs.sendMessage(details.tabId, {
              type: 'PROMPT_CRX_CONFIRM',
              pendingId: pendingId,
              name: name
            }).catch(() => {
              clearTimeout(timer);
              pendingCrxInstalls.delete(pendingId);
              if (browser.downloads && browser.downloads.download) {
                browser.downloads.download({ url: url, saveAs: true }).catch(() => {
                  browser.tabs.create({ url: url });
                });
              } else {
                browser.tabs.create({ url: url });
              }
            });
          }).catch((err) => {
            console.warn('CRX download conversion aborted:', err.message || err);
            if (browser.downloads && browser.downloads.download) {
              browser.downloads.download({ url: url, saveAs: true }).catch(() => {
                browser.tabs.create({ url: url });
              });
            } else {
              browser.tabs.create({ url: url });
            }
          });
        return { cancel: true };
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
    const isPaywallDomain = PAYWALL_DOMAINS.some(d => host === d || host.endsWith('.' + d));

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
  // 1. Strict extension ID requirement: sender.id MUST match runtime.id
  if (!sender || sender.id !== browser.runtime.id) {
    if (typeof sendResponse === 'function') sendResponse({ success: false, error: 'UNAUTHORIZED_SENDER' });
    return true;
  }

  // 2. Privileged operations: require top-level frame (frameId === 0) or internal extension URL
  const isExtensionUrl = typeof sender.url === 'string' && sender.url.startsWith(browser.runtime.getURL(''));
  const isTopFrame = sender.frameId === 0;

  const isPrivileged = [
    'SET_VAULT_PIN', 'UNLOCK_VAULT', 'LOCK_VAULT', 'GET_VAULT',
    'DELETE_VAULT_ITEM', 'EXPORT_VAULT_ITEM', 'SAVE_TO_VAULT',
    'CHECK_VAULT_STATUS', 'INSTALL_CHROME_EXTENSION', 'CONFIRM_CRX_INSTALL'
  ].includes(msg.type);

  if (isPrivileged && !isTopFrame && !isExtensionUrl) {
    if (typeof sendResponse === 'function') sendResponse({ success: false, error: 'PRIVILEGED_CALL_TOP_FRAME_ONLY' });
    return true;
  }
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

  if (msg.type === 'CONFIRM_CRX_INSTALL') {
    const entry = pendingCrxInstalls.get(msg.pendingId);
    if (!entry) {
      sendResponse({ success: false, error: 'EXPIRED_OR_NOT_FOUND' });
      return true;
    }
    if (entry.tabId != null && (!sender.tab || sender.tab.id !== entry.tabId)) {
      sendResponse({ success: false, error: 'TAB_MISMATCH' });
      return true;
    }
    clearTimeout(entry.timer);
    pendingCrxInstalls.delete(msg.pendingId);

    if (!msg.confirmed) {
      sendResponse({ success: true, cancelled: true });
      return true;
    }

    const blobUrl = URL.createObjectURL(entry.blob);
    const fallbackTimer = setTimeout(() => {
      try { URL.revokeObjectURL(blobUrl); } catch (e) {}
    }, 300000);

    const xpiFilename = cleanFilename(entry.name || 'sideloaded_extension', 'xpi');
    if (browser.downloads && browser.downloads.download) {
      browser.downloads.download({
        url: blobUrl,
        filename: xpiFilename,
        saveAs: false
      }).then((downloadId) => {
        if (downloadId) {
          activeBlobDownloads.set(downloadId, { url: blobUrl, timer: fallbackTimer });
        }
        sendResponse({ success: true, action: 'downloaded' });
      }).catch((err) => {
        clearTimeout(fallbackTimer);
        try { URL.revokeObjectURL(blobUrl); } catch (e) {}
        sendResponse({ success: false, error: err.message });
      });
    } else {
      browser.tabs.create({ url: blobUrl });
      sendResponse({ success: true, action: 'opened' });
    }
    return true;
  }

  if (msg.type === 'INSTALL_CHROME_EXTENSION') {
    if (!msg.extensionId || !/^[a-z]{32}$/i.test(msg.extensionId)) {
      sendResponse({ success: false, error: 'INVALID_EXTENSION_ID' });
      return true;
    }
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
      { name: 'NextDNS', url: 'https://dns.nextdns.io/dns-query?name=nextdns.io&type=A' },
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
      let fastest = null;
      for (const r of results) {
        if (r.ok) {
          if (!fastest || r.latency < fastest.latency) {
            fastest = r;
          }
        }
      }
      sendResponse({ results: results, fastest: fastest ? fastest.name : null });
    }).catch(() => {
      sendResponse({ results: [], fastest: null });
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

    if (browser.downloads && browser.downloads.download) {
      browser.downloads.download({
        url: targetUrl,
        filename: targetTitle,
        saveAs: false
      }).then(() => sendResponse({ success: true }))
        .catch(() => {
          browser.tabs.create({ url: targetUrl });
          sendResponse({ success: true });
        });
    } else {
      browser.tabs.create({ url: targetUrl });
      sendResponse({ success: true });
    }
    return true;
  }

  if (msg.type === 'SAVE_TO_VAULT') {
    if (!vaultUnlocked) {
      sendResponse({ success: false, error: 'VAULT_LOCKED' });
      return true;
    }
    const handleSave = async () => {
      let mediaBlob = msg.blob;
      let calculatedSize = msg.size;
      if (!mediaBlob && msg.url) {
        try {
          const resp = await fetch(msg.url, { credentials: 'include' });
          if (resp.ok) {
            mediaBlob = await resp.blob();
            calculatedSize = formatBytes(mediaBlob.size);
          }
        } catch (fetchErr) {
          // If binary fetch fails (e.g. streaming live playlist), preserve stream URL
        }
      }

      await saveItemToVault({
        id: 'vault_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7),
        title: msg.title || 'Private Media',
        format: msg.format || 'mp4',
        size: calculatedSize || 'Saved Stream',
        url: msg.url,
        blob: mediaBlob,
        timestamp: Date.now()
      });
    };

    handleSave()
      .then(() => sendResponse({ success: true }))
      .catch(err => sendResponse({ success: false, error: err.message }));
    return true;
  }

  if (msg.type === 'CHECK_VAULT_STATUS') {
    Promise.all([
      getVaultAuth(),
      getVaultLockoutState()
    ]).then(([auth, lockout]) => {
      const now = Date.now();
      const isLockedOut = now < lockout.lockoutUntil;
      const remainingSec = isLockedOut ? Math.ceil((lockout.lockoutUntil - now) / 1000) : 0;
      sendResponse({
        initialized: !!(auth && auth.hash),
        unlocked: vaultUnlocked,
        lockedOut: isLockedOut,
        remainingSec: remainingSec
      });
    }).catch(() => {
      sendResponse({ initialized: false, unlocked: false, lockedOut: false, remainingSec: 0 });
    });
    return true;
  }

  if (msg.type === 'SET_VAULT_PIN') {
    const pin = (msg.pin || msg.newPin || '').trim();
    if (!/^\d{4,8}$/.test(pin)) {
      sendResponse({ success: false, error: 'PIN must be between 4 and 8 numeric digits' });
      return true;
    }

    withVaultAuthLock(() => getVaultAuth().then(async (auth) => {
      const lockout = await getVaultLockoutState();
      const now = Date.now();
      if (now < lockout.lockoutUntil) {
        const remainingSec = Math.ceil((lockout.lockoutUntil - now) / 1000);
        sendResponse({ success: false, error: 'LOCKED_OUT', lockedOut: true, remainingSec: remainingSec });
        return;
      }

      if (auth && auth.hash) {
        const oldPin = (msg.oldPin || '').trim();
        if (!oldPin) {
          sendResponse({ success: false, error: 'EXISTING_PIN_REQUIRED' });
          return;
        }
        if (!/^\d{4,8}$/.test(oldPin)) {
          sendResponse({ success: false, error: 'INVALID_OLD_PIN' });
          return;
        }
        const { hash: oldHash } = await hashVaultPin(oldPin, auth.salt);
        if (!timingSafeEqualHex(oldHash, auth.hash)) {
          const attempts = (lockout.failedAttempts || 0) + 1;
          if (attempts >= 5) {
            const count = (lockout.lockoutCount || 0) + 1;
            const backoffDuration = Math.min(3600000, 30000 * Math.pow(2, count - 1));
            const until = Date.now() + backoffDuration;
            await setVaultLockoutState({ failedAttempts: 0, lockoutUntil: until, lockoutCount: count });
            sendResponse({ success: false, error: 'LOCKED_OUT', lockedOut: true, remainingSec: Math.ceil(backoffDuration / 1000) });
          } else {
            await setVaultLockoutState({ failedAttempts: attempts, lockoutUntil: 0, lockoutCount: lockout.lockoutCount || 0 });
            sendResponse({ success: false, error: 'INVALID_OLD_PIN', attemptsLeft: 5 - attempts });
          }
          return;
        }
      }

      const { hash, salt } = await hashVaultPin(pin);
      await browser.storage.local.set({
        praha_vault_auth: { hash, salt },
        praha_vault_lockout: { failedAttempts: 0, lockoutUntil: 0, lockoutCount: 0 }
      });
      await browser.storage.local.remove(['praha_vault_pin', 'praha_vault_salt']).catch(() => {});
      vaultUnlocked = true;
      setVaultAutoLock();
      sendResponse({ success: true });
    }).catch(err => {
      sendResponse({ success: false, error: err.message });
    }));
    return true;
  }

  if (msg.type === 'UNLOCK_VAULT') {
    withVaultAuthLock(() => getVaultLockoutState().then(async (lockout) => {
      const now = Date.now();
      if (now < lockout.lockoutUntil) {
        const remainingSec = Math.ceil((lockout.lockoutUntil - now) / 1000);
        sendResponse({ success: false, error: 'LOCKED_OUT', lockedOut: true, remainingSec: remainingSec });
        return;
      }

      const pin = (msg.pin || '').trim();
      if (!/^\d{4,8}$/.test(pin)) {
        sendResponse({ success: false, error: 'INVALID_PIN_FORMAT' });
        return;
      }

      const auth = await getVaultAuth();
      if (!auth || !auth.hash) {
        sendResponse({ success: false, error: 'NO_PIN_CONFIGURED' });
        return;
      }

      const { hash } = await hashVaultPin(pin, auth.salt);
      if (timingSafeEqualHex(hash, auth.hash)) {
        vaultUnlocked = true;
        setVaultAutoLock();
        await setVaultLockoutState({ failedAttempts: 0, lockoutUntil: 0, lockoutCount: 0 });
        sendResponse({ success: true });
      } else {
        const attempts = (lockout.failedAttempts || 0) + 1;
        if (attempts >= 5) {
          const count = (lockout.lockoutCount || 0) + 1;
          const backoffDuration = Math.min(3600000, 30000 * Math.pow(2, count - 1)); // 30s, 60s, 120s...
          const until = Date.now() + backoffDuration;
          await setVaultLockoutState({ failedAttempts: 0, lockoutUntil: until, lockoutCount: count });
          sendResponse({ success: false, error: 'LOCKED_OUT', lockedOut: true, remainingSec: Math.ceil(backoffDuration / 1000) });
        } else {
          await setVaultLockoutState({ failedAttempts: attempts, lockoutUntil: 0, lockoutCount: lockout.lockoutCount || 0 });
          sendResponse({ success: false, error: 'INVALID_PIN', attemptsLeft: 5 - attempts });
        }
      }
    }).catch(err => {
      sendResponse({ success: false, error: err.message });
    }));
    return true;
  }

  if (msg.type === 'UNLOCK_VAULT_BIOMETRIC') {
    sendResponse({
      success: false,
      error: 'BIOMETRIC_UNSUPPORTED',
      message: 'Hardware biometric authentication is not available in WebExtension sandbox. Please authenticate using your PIN.'
    });
    return true;
  }

  if (msg.type === 'LOCK_VAULT') {
    vaultUnlocked = false;
    clearTimeout(vaultAutoLockTimer);
    sendResponse({ success: true });
    return true;
  }

  if (msg.type === 'GET_VAULT') {
    if (!vaultUnlocked) {
      sendResponse({ success: false, error: 'VAULT_LOCKED', items: [] });
      return true;
    }
    setVaultAutoLock();
    getVaultItems().then(items => {
      sendResponse({ success: true, items: items });
    }).catch(() => {
      sendResponse({ success: false, items: [] });
    });
    return true;
  }

  if (msg.type === 'DELETE_VAULT_ITEM') {
    if (!vaultUnlocked) {
      sendResponse({ success: false, error: 'VAULT_LOCKED' });
      return true;
    }
    setVaultAutoLock();
    deleteVaultItem(msg.id).then(() => {
      sendResponse({ success: true });
    }).catch(err => {
      sendResponse({ success: false, error: err.message || 'Delete failed' });
    });
    return true;
  }

  if (msg.type === 'EXPORT_VAULT_ITEM') {
    if (!vaultUnlocked) {
      sendResponse({ success: false, error: 'VAULT_LOCKED' });
      return true;
    }
    setVaultAutoLock();
    getVaultItemById(msg.id).then(item => {
      if (!item) {
        sendResponse({ success: false, error: 'Item not found in vault' });
        return;
      }
      const filename = cleanFilename(item.title, item.format);
      if (item.blob) {
        const blobUrl = URL.createObjectURL(item.blob);
        const fallbackTimer = setTimeout(() => {
          try { URL.revokeObjectURL(blobUrl); } catch (e) {}
        }, 300000); // 5-minute fallback

        if (browser.downloads && browser.downloads.download) {
          browser.downloads.download({
            url: blobUrl,
            filename: filename,
            saveAs: false
          }).then((downloadId) => {
            if (downloadId) {
              activeBlobDownloads.set(downloadId, { url: blobUrl, timer: fallbackTimer });
            }
            sendResponse({ success: true });
          }).catch((err) => {
            clearTimeout(fallbackTimer);
            try { URL.revokeObjectURL(blobUrl); } catch (e) {}
            sendResponse({ success: false, error: err.message });
          });
        } else {
          browser.tabs.create({ url: blobUrl });
          sendResponse({ success: true });
        }
      } else if (item.url) {
        if (browser.downloads && browser.downloads.download) {
          browser.downloads.download({
            url: item.url,
            filename: filename,
            saveAs: false
          }).then(() => sendResponse({ success: true }))
            .catch(() => {
              browser.tabs.create({ url: item.url });
              sendResponse({ success: true });
            });
        } else {
          browser.tabs.create({ url: item.url });
          sendResponse({ success: true });
        }
      } else {
        sendResponse({ success: false, error: 'No media content available to export' });
      }
    }).catch(err => {
      sendResponse({ success: false, error: err.message });
    });
    return true;
  }

  if (msg.type === 'DOWNLOAD_ALL') {
    const items = msg.items || [];
    items.forEach((item, index) => {
      setTimeout(() => {
        const title = cleanFilename(item.title, item.format);
        if (browser.downloads && browser.downloads.download) {
          browser.downloads.download({
            url: item.url,
            filename: title,
            saveAs: false
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
