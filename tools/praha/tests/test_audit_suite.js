// Praha Browser Extension Audit Test Suite (VM Sandbox executing real background.js)
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const assert = require('assert');
const { webcrypto } = require('crypto');

console.log('--- PRAHA BROWSER EXTENSION VM INTEGRATION SUITE ---');

// Mock WebExtension environment
const storageData = new Map();
let onMessageListener = null;
let onBeforeSendHeadersListener = null;
let onBeforeRequestListener = null;

const mockBrowser = {
  runtime: {
    id: 'media-downloader@prahabrowser.org',
    getURL: (p) => 'moz-extension://media-downloader/' + p,
    onMessage: {
      addListener: (fn) => { onMessageListener = fn; }
    }
  },
  storage: {
    local: {
      get: async (keys) => {
        const out = {};
        const keyList = Array.isArray(keys) ? keys : [keys];
        for (const k of keyList) {
          if (storageData.has(k)) out[k] = storageData.get(k);
        }
        return out;
      },
      set: async (obj) => {
        for (const [k, v] of Object.entries(obj)) {
          storageData.set(k, v);
        }
      },
      remove: async (keys) => {
        const keyList = Array.isArray(keys) ? keys : [keys];
        for (const k of keyList) {
          storageData.delete(k);
        }
      }
    }
  },
  webRequest: {
    onBeforeRequest: {
      addListener: (fn) => { onBeforeRequestListener = fn; }
    },
    onBeforeSendHeaders: {
      addListener: (fn) => { onBeforeSendHeadersListener = fn; }
    },
    onResponseStarted: {
      addListener: () => {}
    }
  },
  tabs: {
    create: async (opts) => ({ id: 99, ...opts }),
    onRemoved: { addListener: () => {} },
    onUpdated: { addListener: () => {} },
    sendMessage: async () => {},
    executeScript: async () => {}
  },
  downloads: {
    download: async (opts) => 101,
    onChanged: { addListener: () => {} }
  }
};

const sandbox = {
  browser: mockBrowser,
  crypto: webcrypto,
  TextEncoder,
  TextDecoder,
  DataView,
  Uint8Array,
  ArrayBuffer,
  URL,
  Blob,
  setTimeout,
  clearTimeout,
  console,
  fetch: async (url) => {
    const buf = new Uint8Array([0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]).buffer;
    return {
      ok: true,
      headers: { get: () => '16' },
      arrayBuffer: async () => buf
    };
  },
  indexedDB: {
    open: () => ({
      onupgradeneeded: null,
      onsuccess: null,
      onerror: null
    })
  },
  JSZip: {
    loadAsync: async () => ({
      file: () => ({
        async: async () => JSON.stringify({ name: 'TestExtension' })
      }),
      generateAsync: async () => new Blob(['fake_xpi_data'], { type: 'application/x-xpinstall' })
    })
  }
};

const bgPath = path.resolve(__dirname, '../../../mobile/android/fenix/app/src/main/assets/extensions/media-downloader/background.js');
const bgCode = fs.readFileSync(bgPath, 'utf8');

const context = vm.createContext(sandbox);
vm.runInContext(bgCode, context);

assert(typeof onMessageListener === 'function', 'background.js must register onMessage listener');
assert(typeof onBeforeSendHeadersListener === 'function', 'background.js must register onBeforeSendHeaders listener');
assert(typeof onBeforeRequestListener === 'function', 'background.js must register onBeforeRequest listener');

function sendMessage(msg, sender = { id: 'media-downloader@prahabrowser.org', frameId: 0 }) {
  return new Promise((resolve) => {
    const res = onMessageListener(msg, sender, (response) => {
      resolve(response);
    });
    if (!res) resolve(undefined);
  });
}

(async () => {
  // ==========================================================================
  // Test 1: Sender validation on privileged message handler
  // ==========================================================================
  // 1a. Foreign sender ID
  const foreignSenderRes = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'malicious-extension-id', frameId: 0 }
  );
  assert.strictEqual(foreignSenderRes.success, false);
  assert.strictEqual(foreignSenderRes.error, 'UNAUTHORIZED_SENDER');

  // 1b. Missing sender ID
  const missingSenderRes = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    {}
  );
  assert.strictEqual(missingSenderRes.success, false);
  assert.strictEqual(missingSenderRes.error, 'UNAUTHORIZED_SENDER');

  // 1c. Missing frameId on privileged call
  const missingFrameIdRes = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'media-downloader@prahabrowser.org' }
  );
  assert.strictEqual(missingFrameIdRes.success, false);
  assert.strictEqual(missingFrameIdRes.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  // 1d. Subframe frameId > 0 with web page URL
  const subframeWebRes = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'media-downloader@prahabrowser.org', frameId: 1, url: 'https://evil.example.com/iframe' }
  );
  assert.strictEqual(subframeWebRes.success, false);
  assert.strictEqual(subframeWebRes.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  // 1e. Allowed extension page URL
  const extPageRes = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'media-downloader@prahabrowser.org', url: 'moz-extension://media-downloader/popup.html' }
  );
  assert.strictEqual(extPageRes.initialized, false); // allowed through to handler

  console.log('PASS: 1. Sender ID validation strictly rejected unauthorized / missing sender.');

  // ==========================================================================
  // Test 2: Vault initial status check
  // ==========================================================================
  const initialStatus = await sendMessage({ type: 'CHECK_VAULT_STATUS' });
  assert.strictEqual(initialStatus.initialized, false, 'Initial vault must not be initialized');
  assert.strictEqual(initialStatus.unlocked, false, 'Initial vault must be locked');
  assert.strictEqual(initialStatus.lockedOut, false, 'Initial vault must not be locked out');
  console.log('PASS: 2. Initial vault status checked.');

  // ==========================================================================
  // Test 3: Gated endpoints when locked
  // ==========================================================================
  const lockedGet = await sendMessage({ type: 'GET_VAULT' });
  assert.strictEqual(lockedGet.success, false);
  assert.strictEqual(lockedGet.error, 'VAULT_LOCKED');

  const lockedDel = await sendMessage({ type: 'DELETE_VAULT_ITEM', id: '123' });
  assert.strictEqual(lockedDel.success, false);
  assert.strictEqual(lockedDel.error, 'VAULT_LOCKED');

  const lockedExport = await sendMessage({ type: 'EXPORT_VAULT_ITEM', id: '123' });
  assert.strictEqual(lockedExport.success, false);
  assert.strictEqual(lockedExport.error, 'VAULT_LOCKED');
  console.log('PASS: 3. Vault endpoints securely gated behind locked state.');

  // ==========================================================================
  // Test 4: Biometric unlock rejection (hardware biometrics not in WebExtension)
  // ==========================================================================
  const bioRes = await sendMessage({ type: 'UNLOCK_VAULT_BIOMETRIC' });
  assert.strictEqual(bioRes.success, false);
  assert.strictEqual(bioRes.error, 'BIOMETRIC_UNSUPPORTED');
  console.log('PASS: 4. Dummy biometric bypass strictly rejected.');

  // ==========================================================================
  // Test 5: Setting Vault PIN & Overwrite Protection
  // ==========================================================================
  // Invalid formats
  const nonDigitRes = await sendMessage({ type: 'SET_VAULT_PIN', pin: 'abcd' });
  assert.strictEqual(nonDigitRes.success, false);
  assert.match(nonDigitRes.error, /numeric digits/);

  const shortPinRes = await sendMessage({ type: 'SET_VAULT_PIN', pin: '12' });
  assert.strictEqual(shortPinRes.success, false);

  // Set initial PIN (verifying both pin and newPin parameter acceptance)
  const setRes = await sendMessage({ type: 'SET_VAULT_PIN', newPin: '8492' });
  assert.strictEqual(setRes.success, true);

  // Check storage contains salt and domain-separated verifier hash
  const authRecord = storageData.get('praha_vault_auth');
  assert(authRecord && authRecord.hash && authRecord.salt, 'Salt and verifier must be stored in storage.local');
  assert.strictEqual(typeof authRecord.hash, 'string');
  assert.strictEqual(authRecord.hash.length, 64, 'SHA-256 verifier must be 64 hex chars');

  // Attempt overwrite without oldPin
  const overwriteNoOld = await sendMessage({ type: 'SET_VAULT_PIN', pin: '1234' });
  assert.strictEqual(overwriteNoOld.success, false);
  assert.strictEqual(overwriteNoOld.error, 'EXISTING_PIN_REQUIRED');

  // Attempt overwrite with wrong oldPin
  const overwriteWrongOld = await sendMessage({ type: 'SET_VAULT_PIN', pin: '1234', oldPin: '0000' });
  assert.strictEqual(overwriteWrongOld.success, false);
  assert.strictEqual(overwriteWrongOld.error, 'INVALID_OLD_PIN');

  // Attempt overwrite with correct oldPin
  const overwriteCorrect = await sendMessage({ type: 'SET_VAULT_PIN', pin: '5678', oldPin: '8492' });
  assert.strictEqual(overwriteCorrect.success, true);
  console.log('PASS: 5. PIN setting, format validation, and overwrite protection verified.');

  // ==========================================================================
  // Test 6: Lock, unlock with timingSafeEqualHex, format validation, and brute-force lockout
  // ==========================================================================
  // 6a. Format validation in UNLOCK_VAULT
  const invalidFormatUnlock = await sendMessage({ type: 'UNLOCK_VAULT', pin: 'abc' });
  assert.strictEqual(invalidFormatUnlock.success, false);
  assert.strictEqual(invalidFormatUnlock.error, 'INVALID_PIN_FORMAT');

  // Lock vault
  await sendMessage({ type: 'LOCK_VAULT' });
  const lockedStatus = await sendMessage({ type: 'CHECK_VAULT_STATUS' });
  assert.strictEqual(lockedStatus.unlocked, false);

  // Wrong PIN attempts 1 to 4
  for (let i = 1; i <= 4; i++) {
    const wrongRes = await sendMessage({ type: 'UNLOCK_VAULT', pin: '0000' });
    assert.strictEqual(wrongRes.success, false);
    assert.strictEqual(wrongRes.error, 'INVALID_PIN');
    assert.strictEqual(wrongRes.attemptsLeft, 5 - i);
  }

  // 5th wrong attempt triggers lockout
  const lockTriggerRes = await sendMessage({ type: 'UNLOCK_VAULT', pin: '0000' });
  assert.strictEqual(lockTriggerRes.success, false);
  assert.strictEqual(lockTriggerRes.error, 'LOCKED_OUT');
  assert.strictEqual(lockTriggerRes.lockedOut, true, 'Lockout response must include lockedOut: true flag');
  assert.strictEqual(lockTriggerRes.remainingSec, 30);

  // During lockout, even correct PIN is rejected
  const lockedAttempt = await sendMessage({ type: 'UNLOCK_VAULT', pin: '5678' });
  assert.strictEqual(lockedAttempt.success, false);
  assert.strictEqual(lockedAttempt.error, 'LOCKED_OUT');
  assert.strictEqual(lockedAttempt.lockedOut, true, 'Subsequent lockout response must include lockedOut: true flag');

  // Manually fast-forward lockout in storage
  const lockoutState = storageData.get('praha_vault_lockout');
  lockoutState.lockoutUntil = Date.now() - 1000;
  storageData.set('praha_vault_lockout', lockoutState);

  // Unlock with correct PIN
  const unlockRes = await sendMessage({ type: 'UNLOCK_VAULT', pin: '5678' });
  assert.strictEqual(unlockRes.success, true);

  // Lockout state must be reset
  const resetLockout = storageData.get('praha_vault_lockout');
  assert.strictEqual(resetLockout.failedAttempts, 0);
  assert.strictEqual(resetLockout.lockoutCount, 0);

  // 6b. Change PIN shared attempt counter & lockout verification
  await sendMessage({ type: 'LOCK_VAULT' });
  for (let i = 1; i <= 4; i++) {
    const wrongChangeRes = await sendMessage({
      type: 'SET_VAULT_PIN',
      oldPin: '0000',
      newPin: '1234'
    });
    assert.strictEqual(wrongChangeRes.success, false);
    assert.strictEqual(wrongChangeRes.error, 'INVALID_OLD_PIN');
    assert.strictEqual(wrongChangeRes.attemptsLeft, 5 - i);
  }

  // 5th wrong attempt on Change PIN triggers lockout
  const changeLockRes = await sendMessage({
    type: 'SET_VAULT_PIN',
    oldPin: '0000',
    newPin: '1234'
  });
  assert.strictEqual(changeLockRes.success, false);
  assert.strictEqual(changeLockRes.error, 'LOCKED_OUT');
  assert.strictEqual(changeLockRes.lockedOut, true);

  // Verify UNLOCK_VAULT is also blocked by the shared lockout
  const unlockBlockedRes = await sendMessage({ type: 'UNLOCK_VAULT', pin: '5678' });
  assert.strictEqual(unlockBlockedRes.success, false);
  assert.strictEqual(unlockBlockedRes.error, 'LOCKED_OUT');

  // Fast forward lockout
  const sharedLockout = storageData.get('praha_vault_lockout');
  sharedLockout.lockoutUntil = Date.now() - 1000;
  storageData.set('praha_vault_lockout', sharedLockout);

  // 6c. Legacy PIN migration verification
  storageData.delete('praha_vault_auth');
  storageData.set('praha_vault_pin', '7777');
  const statusAfterLegacy = await sendMessage({ type: 'CHECK_VAULT_STATUS' });
  assert.strictEqual(statusAfterLegacy.initialized, true, 'CHECK_VAULT_STATUS must report initialized after migrating legacy PIN');
  const migratedAuth = storageData.get('praha_vault_auth');
  assert(migratedAuth && migratedAuth.hash && migratedAuth.salt, 'Legacy PIN must be migrated to praha_vault_auth hash and salt');
  assert(!storageData.has('praha_vault_pin'), 'Legacy praha_vault_pin key must be purged');

  const legacyUnlockRes = await sendMessage({ type: 'UNLOCK_VAULT', pin: '7777' });
  assert.strictEqual(legacyUnlockRes.success, true, 'Migrated legacy PIN must unlock successfully');

  console.log('PASS: 6. Constant-time verification, lockout backoff, shared counter, and legacy migration verified.');

  // ==========================================================================
  // Test 7: Paywall domain suffix boundary matching
  // ==========================================================================
  const attackerDetails = {
    url: 'https://fakemedium.com/story/test',
    requestHeaders: [{ name: 'User-Agent', value: 'Mozilla/5.0' }]
  };
  const attackerRes = onBeforeSendHeadersListener(attackerDetails);
  assert(!attackerRes.requestHeaders, 'Attacker domain fakemedium.com must NOT trigger paywall header cleaner');
  assert.strictEqual(Object.keys(attackerRes).length, 0);

  const attacker2Details = {
    url: 'https://evilnytimes.com/article',
    requestHeaders: [{ name: 'User-Agent', value: 'Mozilla/5.0' }]
  };
  const attacker2Res = onBeforeSendHeadersListener(attacker2Details);
  assert(!attacker2Res.requestHeaders, 'Attacker domain evilnytimes.com must NOT trigger paywall header cleaner');
  assert.strictEqual(Object.keys(attacker2Res).length, 0);

  const validMediumDetails = {
    url: 'https://medium.com/@author/post',
    requestHeaders: [{ name: 'User-Agent', value: 'Mozilla/5.0' }]
  };
  const validRes = onBeforeSendHeadersListener(validMediumDetails);
  assert(validRes.requestHeaders && validRes.requestHeaders.some(h => h.name === 'Referer' && h.value === 'https://www.google.com/'), 'Valid domain medium.com must have Referer set to Google');

  const validSubdomainDetails = {
    url: 'https://subdomain.nytimes.com/article',
    requestHeaders: [{ name: 'User-Agent', value: 'Mozilla/5.0' }]
  };
  const validSubRes = onBeforeSendHeadersListener(validSubdomainDetails);
  assert(validSubRes.requestHeaders && validSubRes.requestHeaders.some(h => h.name === 'Referer'), 'Valid subdomain subdomain.nytimes.com must have Referer set');
  console.log('PASS: 7. Paywall domain boundary exact suffix matching verified.');

  // ==========================================================================
  // Test 8: CRX Conversion Bounds and Truncation Handling
  // ==========================================================================
  const convertCrxToXpi = context.convertCrxToXpi;
  assert(typeof convertCrxToXpi === 'function', 'convertCrxToXpi must be defined in background.js');

  // Truncated buffer
  try {
    await convertCrxToXpi(new ArrayBuffer(8), 'test');
    assert.fail('Truncated CRX must throw');
  } catch (err) {
    assert.match(err.message, /Truncated CRX/);
  }

  // Oversized buffer
  try {
    await convertCrxToXpi(new ArrayBuffer(51 * 1024 * 1024), 'test');
    assert.fail('Oversized CRX must throw');
  } catch (err) {
    assert.match(err.message, /exceeds 50MB/);
  }

  // Malformed CRX2 with out-of-bounds header length
  const malformedCrx2 = new ArrayBuffer(32);
  const v = new DataView(malformedCrx2);
  v.setUint32(0, 0x43723234, false); // "Cr24"
  v.setUint32(4, 2, true);          // version 2
  v.setUint32(8, 10000, true);      // pubKeyLen out of bounds
  v.setUint32(12, 10000, true);     // sigLen
  try {
    await convertCrxToXpi(malformedCrx2, 'test');
    assert.fail('Malformed CRX2 header must throw');
  } catch (err) {
    assert.match(err.message, /Truncated or malformed CRX2/);
  }

  console.log('PASS: 8. CRX header bounds and size limit validation verified.');

  // ==========================================================================
  // Test 9: Privileged message origin restriction (iframe frameId > 0 or missing frameId blocked)
  // ==========================================================================
  const iframeAttempt = await sendMessage(
    { type: 'UNLOCK_VAULT', pin: '5678' },
    { id: 'media-downloader@prahabrowser.org', frameId: 1 }
  );
  assert.strictEqual(iframeAttempt.success, false);
  assert.strictEqual(iframeAttempt.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  const missingFrameAttempt = await sendMessage(
    { type: 'UNLOCK_VAULT', pin: '5678' },
    { id: 'media-downloader@prahabrowser.org' }
  );
  assert.strictEqual(missingFrameAttempt.success, false);
  assert.strictEqual(missingFrameAttempt.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  const iframeStatus = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'media-downloader@prahabrowser.org', frameId: 1 }
  );
  assert.strictEqual(iframeStatus.success, false);
  assert.strictEqual(iframeStatus.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  const iframeSave = await sendMessage(
    { type: 'SAVE_TO_VAULT', title: 'Malicious' },
    { id: 'media-downloader@prahabrowser.org', frameId: 1 }
  );
  assert.strictEqual(iframeSave.success, false);
  assert.strictEqual(iframeSave.error, 'PRIVILEGED_CALL_TOP_FRAME_ONLY');

  const extPageAttempt = await sendMessage(
    { type: 'CHECK_VAULT_STATUS' },
    { id: 'media-downloader@prahabrowser.org', url: 'moz-extension://media-downloader/popup.html' }
  );
  assert.strictEqual(extPageAttempt.initialized, true);

  console.log('PASS: 9. Privileged calls strictly restricted to top frame (frameId === 0) or internal extension URL.');

  // ==========================================================================
  // Test 10: Extension ID validation on INSTALL_CHROME_EXTENSION
  // ==========================================================================
  const badShortId = await sendMessage({ type: 'INSTALL_CHROME_EXTENSION', extensionId: 'abc' });
  assert.strictEqual(badShortId.success, false);
  assert.strictEqual(badShortId.error, 'INVALID_EXTENSION_ID');

  const badCharId = await sendMessage({ type: 'INSTALL_CHROME_EXTENSION', extensionId: '12345678901234567890123456789012' });
  assert.strictEqual(badCharId.success, false);
  assert.strictEqual(badCharId.error, 'INVALID_EXTENSION_ID');

  console.log('PASS: 10. Chrome extension ID strictly validated against 32-char alphabetic pattern.');

  // ==========================================================================
  // Test 10b: CRX Installation confirmation and tab validation
  // ==========================================================================
  // Nonexistent / expired pendingId
  const crxNonexistentRes = await sendMessage({
    type: 'CONFIRM_CRX_INSTALL',
    pendingId: 'crx_nonexistent_999',
    confirmed: true
  });
  assert.strictEqual(crxNonexistentRes.success, false);
  assert.strictEqual(crxNonexistentRes.error, 'EXPIRED_OR_NOT_FOUND');

  // Prepare a pending CRX install for tab 10
  const validExtensionId = 'abcdefghijklmnopqrstuvwxyzabcdef';
  const prepInstall1 = await context.fetchCrxAndPrepareInstall(validExtensionId, 10);
  assert(prepInstall1 && prepInstall1.pendingId, 'fetchCrxAndPrepareInstall must return pendingId');

  // Tab mismatch: sender tab 99 tries to confirm install intended for tab 10
  const tabMismatchRes = await sendMessage(
    { type: 'CONFIRM_CRX_INSTALL', pendingId: prepInstall1.pendingId, confirmed: true },
    { id: 'media-downloader@prahabrowser.org', frameId: 0, tab: { id: 99 } }
  );
  assert.strictEqual(tabMismatchRes.success, false);
  assert.strictEqual(tabMismatchRes.error, 'TAB_MISMATCH');

  // Cancelled install: user clicks cancel in confirmation modal
  const prepInstall2 = await context.fetchCrxAndPrepareInstall(validExtensionId, 10);
  const cancelInstallRes = await sendMessage(
    { type: 'CONFIRM_CRX_INSTALL', pendingId: prepInstall2.pendingId, confirmed: false },
    { id: 'media-downloader@prahabrowser.org', frameId: 0, tab: { id: 10 } }
  );
  assert.strictEqual(cancelInstallRes.success, true);
  assert.strictEqual(cancelInstallRes.cancelled, true);

  // Confirmed install: user accepts installation in modal
  const prepInstall3 = await context.fetchCrxAndPrepareInstall(validExtensionId, 10);
  const confirmInstallRes = await sendMessage(
    { type: 'CONFIRM_CRX_INSTALL', pendingId: prepInstall3.pendingId, confirmed: true },
    { id: 'media-downloader@prahabrowser.org', frameId: 0, tab: { id: 10 } }
  );
  assert.strictEqual(confirmInstallRes.success, true);
  assert.strictEqual(confirmInstallRes.action, 'downloaded');
  console.log('PASS: 10b. CRX confirmation contract (tab validation, expiration, cancel, download) verified.');

  // ==========================================================================
  // Test 11: Change PIN UI message contract
  // ==========================================================================
  const changePinAttempt = await sendMessage({
    type: 'SET_VAULT_PIN',
    pin: '9999',
    newPin: '9999',
    oldPin: '7777'
  });
  assert.strictEqual(changePinAttempt.success, true);
  console.log('PASS: 11. Change PIN message contract verified with newPin and oldPin.');

  // ==========================================================================
  // Test 12: Content script sandbox VM execution (Geolocation, Drag, Shadow DOM)
  // ==========================================================================
  const contentScriptPath = path.resolve(__dirname, '../../../mobile/android/fenix/app/src/main/assets/extensions/media-downloader/content.js');
  const contentScriptCode = fs.readFileSync(contentScriptPath, 'utf8');

  let contentMsgListener = null;
  const mockContentBrowser = {
    runtime: {
      id: 'media-downloader@prahabrowser.org',
      getURL: (p) => 'moz-extension://media-downloader/' + p,
      onMessage: {
        addListener: (fn) => { contentMsgListener = fn; }
      },
      sendMessage: async (msg) => {
        if (msg.type === 'CHECK_VAULT_STATUS') {
          return { initialized: true, unlocked: false, lockedOut: false, remainingSec: 0 };
        }
        if (msg.type === 'GET_MEDIA') {
          return { items: [], power: { speed: 1.0, boost: 1.0 } };
        }
        return { success: true };
      }
    }
  };

  class MockElement {
    constructor(tag) {
      this.tagName = (tag || 'DIV').toUpperCase();
      this.children = [];
      this.style = {};
      this.classList = {
        add: () => {},
        remove: () => {},
        contains: () => false
      };
      this.listeners = new Map();
      this.attributes = new Map();
      this.id = '';
      this.isConnected = true;
      this._shadowRoot = null;
    }
    appendChild(child) {
      this.children.push(child);
      if (child.tagName === 'SCRIPT' && child.textContent) {
        try {
          vm.runInContext(child.textContent, contentVmContext);
        } catch(e) {}
      }
      return child;
    }
    prepend(child) {
      this.children.unshift(child);
      return child;
    }
    removeChild(child) {
      this.children = this.children.filter(c => c !== child);
      return child;
    }
    remove() {}
    setAttribute(k, v) { this.attributes.set(k, v); }
    getAttribute(k) { return this.attributes.get(k) || null; }
    addEventListener(evt, fn) {
      if (!this.listeners.has(evt)) this.listeners.set(evt, []);
      this.listeners.get(evt).push(fn);
    }
    dispatchEvent(evt) {
      const list = this.listeners.get(evt.type) || [];
      for (const fn of list) fn(evt);
    }
    attachShadow(opts) {
      this._shadowRoot = new MockElement('SHADOW-ROOT');
      return this._shadowRoot;
    }
    set innerHTML(html) {
      this._innerHTML = html;
      this.children = [];
      const idMatches = [...html.matchAll(/id=["']([^"']+)["']/g)];
      for (const m of idMatches) {
        const child = new MockElement('DIV');
        child.id = m[1];
        this.children.push(child);
      }
    }
    get innerHTML() {
      return this._innerHTML || '';
    }
    querySelector(sel) {
      const cleanSel = sel.replace(/^[#.]/, '');
      const findIn = (el) => {
        for (const c of el.children) {
          if (c.id === cleanSel || c.tagName.toLowerCase() === sel.toLowerCase()) return c;
          const found = findIn(c);
          if (found) return found;
        }
        return null;
      };
      return findIn(this);
    }
    querySelectorAll(sel) {
      const cleanSel = sel.replace(/^[#.]/, '');
      const results = [];
      const collect = (el) => {
        for (const c of el.children) {
          if (c.id === cleanSel || c.tagName.toLowerCase() === sel.toLowerCase()) results.push(c);
          collect(c);
        }
      };
      collect(this);
      return results;
    }
    getBoundingClientRect() {
      return { top: 100, bottom: 140, left: 20, right: 60, width: 40, height: 40 };
    }
    setPointerCapture() {}
    releasePointerCapture() {}
  }

  const nativeGeoDescriptor = {
    configurable: true,
    enumerable: true,
    writable: true,
    value: {
      getCurrentPosition: (cb) => { cb({ coords: { latitude: 0, longitude: 0 } }); },
      watchPosition: () => 999,
      clearWatch: () => {}
    }
  };

  const mockNavigator = {};
  Object.defineProperty(mockNavigator, 'geolocation', nativeGeoDescriptor);

  const mockDocument = {
    head: new MockElement('HEAD'),
    body: new MockElement('BODY'),
    documentElement: new MockElement('HTML'),
    createElement: (tag) => new MockElement(tag),
    getElementById: () => null,
    querySelector: () => null,
    querySelectorAll: () => [],
    addEventListener: () => {},
    title: 'Praha Test Tab'
  };

  const mockWindow = {
    wrappedJSObject: null,
    navigator: mockNavigator,
    document: mockDocument,
    innerHeight: 800,
    innerWidth: 400,
    addEventListener: () => {},
    location: {
      hostname: 'example.com',
      pathname: '/',
      href: 'https://example.com/'
    },
    scrollBy: () => {}
  };
  mockWindow.top = mockWindow;

  const contentSandbox = {
    window: mockWindow,
    document: mockDocument,
    navigator: mockNavigator,
    location: mockWindow.location,
    browser: mockContentBrowser,
    MutationObserver: class {
      observe() {}
      disconnect() {}
    },
    setTimeout,
    clearTimeout,
    setInterval,
    clearInterval,
    console,
    URL,
    Date,
    Math
  };

  const contentVmContext = vm.createContext(contentSandbox);
  vm.runInContext(contentScriptCode, contentVmContext);

  assert(typeof contentMsgListener === 'function', 'content.js must register runtime.onMessage listener');

  // Verify Geolocation override with sequential watch IDs via message dispatch
  const spoofGeo = { mode: 'spoof', preset: 'London', lat: 51.5074, lng: -0.1278 };
  contentMsgListener({ type: 'APPLY_GEO_CONFIG', geo: spoofGeo });

  assert(mockNavigator.geolocation !== nativeGeoDescriptor.value, 'Navigator geolocation must be overridden when mode is spoof');
  const watchId1 = mockNavigator.geolocation.watchPosition(() => {});
  const watchId2 = mockNavigator.geolocation.watchPosition(() => {});
  assert.strictEqual(watchId1 + 1, watchId2, 'Watch IDs must increment sequentially to eliminate collisions');

  // Verify clearWatch actively clears and deletes watcher timer
  assert(mockWindow.__praha_active_watchers.has(watchId1), 'Watcher timer must exist in __praha_active_watchers');
  mockNavigator.geolocation.clearWatch(watchId1);
  assert(!mockWindow.__praha_active_watchers.has(watchId1), 'Watcher timer must be cleared and deleted from __praha_active_watchers upon clearWatch');

  // Restore native geolocation on mode === 'off'
  contentMsgListener({ type: 'APPLY_GEO_CONFIG', geo: { mode: 'off' } });
  const restoredDesc = Object.getOwnPropertyDescriptor(mockNavigator, 'geolocation');
  assert(restoredDesc, 'Native geolocation descriptor must be restored on mode === off');

  // Verify CRX confirmation in isolated shadow root and message dispatch
  let crxConfirmedPayload = null;
  mockContentBrowser.runtime.sendMessage = async (msg) => {
    if (msg.type === 'CONFIRM_CRX_INSTALL') {
      crxConfirmedPayload = msg;
      return { success: true };
    }
    return { success: true };
  };

  contentMsgListener({ type: 'PROMPT_CRX_CONFIRM', name: 'sample.xpi', pendingId: 'crx_test_123' });
  const hostElem = mockDocument.body.querySelector('#praha-media-host') || mockDocument.documentElement.querySelector('#praha-media-host');
  assert(hostElem && hostElem._shadowRoot, 'Shadow root must exist on media host element');
  const crxDialog = hostElem._shadowRoot.querySelector('#praha-crx-confirm-dialog');
  assert(crxDialog, 'CRX confirmation dialog must be created in shadow root');
  const installBtn = crxDialog.querySelector('#praha-crx-ok-btn');
  assert(installBtn, 'Install Extension button must exist');
  installBtn.onclick();
  assert(crxConfirmedPayload, 'Clicking install must dispatch CONFIRM_CRX_INSTALL message');
  assert.strictEqual(crxConfirmedPayload.pendingId, 'crx_test_123');
  assert.strictEqual(crxConfirmedPayload.confirmed, true);

  // Direct CRX interception with tabId < 0 must cancel immediately without opening prompt
  const tabNegDetails = { url: 'https://example.com/test.crx', tabId: -1 };
  const tabNegRes = onBeforeRequestListener(tabNegDetails);
  assert.strictEqual(tabNegRes.cancel, true);

  // Verify floating media pill Pointer Events drag and click suppression
  contentMsgListener({
    type: 'PRAHA_MEDIA_DETECTED',
    items: [{ id: '1', title: 'test', format: 'MP4', url: 'http://example.com/v.mp4' }]
  });

  const findPill = (el) => {
    if (!el) return null;
    if (el.id === 'praha-media-pill') return el;
    for (const c of el.children) {
      const res = findPill(c);
      if (res) return res;
    }
    if (el._shadowRoot) {
      const res = findPill(el._shadowRoot);
      if (res) return res;
    }
    return null;
  };
  const pill = findPill(mockDocument.body) || findPill(mockDocument.documentElement);
  assert(pill, 'Praha media pill must be rendered in UI host / shadow root');

  // Simulate drag: pointerdown -> pointermove (delta > 5px) -> click -> pointerup
  pill.dispatchEvent({ type: 'pointerdown', clientY: 500, pointerId: 1 });
  pill.dispatchEvent({ type: 'pointermove', clientY: 480, pointerId: 1 });
  assert.strictEqual(pill.style.bottom, '680px', 'Pill style.bottom must be updated when dragged');
  let clickPrevented = false;
  pill.dispatchEvent({
    type: 'click',
    stopImmediatePropagation: () => { clickPrevented = true; },
    preventDefault: () => {}
  });
  assert(clickPrevented, 'Click event must be suppressed when drag has occurred');
  pill.dispatchEvent({ type: 'pointerup', pointerId: 1 });

  console.log('PASS: 12. Content script VM sandbox integration (Geolocation sequence, descriptor restoration, CRX modal, Pointer drag) verified.');

  console.log('\n--- ALL REAL CODE VM INTEGRATION TESTS PASSED ---');
  process.exit(0);
})();
