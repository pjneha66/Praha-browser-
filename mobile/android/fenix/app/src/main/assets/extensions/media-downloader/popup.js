// Praha Media & Power Suite - Popup Script
document.addEventListener('DOMContentLoaded', async () => {
  const container = document.getElementById('media-container');
  const status = document.getElementById('media-status');
  const footer = document.getElementById('popup-footer');
  const refreshBtn = document.getElementById('btn-refresh');
  const downloadAllBtn = document.getElementById('btn-download-all');
  const torrentBtn = document.getElementById('btn-open-torrent');

  let currentItems = [];
  let currentTabId = null;

  if (torrentBtn) {
    torrentBtn.addEventListener('click', () => {
      browser.tabs.create({ url: browser.runtime.getURL('webtorrent.html') });
    });
  }

  async function loadMedia() {
    try {
      const tabs = await browser.tabs.query({ active: true, currentWindow: true });
      const activeTab = tabs[0];
      if (!activeTab) return;
      currentTabId = activeTab.id;

      const res = await browser.runtime.sendMessage({
        type: 'GET_MEDIA',
        tabId: activeTab.id
      });

      currentItems = (res && res.items) || [];
      if (res && res.power) {
        updatePowerUI(res.power.speed || 1.0, res.power.boost || 1.0, res.power.doh || 'Cloudflare', res.power.geo);
      }
      render(currentItems, activeTab.id);
    } catch (e) {
      status.textContent = 'Ready';
    }
  }

  function updatePowerUI(speed, boost, doh, geo) {
    document.querySelectorAll('#speed-pills .pill').forEach(p => {
      const s = parseFloat(p.getAttribute('data-speed'));
      if (s === speed) p.classList.add('pill-active');
      else p.classList.remove('pill-active');
    });

    document.querySelectorAll('#boost-pills .pill').forEach(p => {
      const b = parseFloat(p.getAttribute('data-boost'));
      if (b === boost) p.classList.add('pill-active');
      else p.classList.remove('pill-active');
    });

    document.querySelectorAll('#doh-pills .pill').forEach(p => {
      const d = p.getAttribute('data-doh');
      if (d === doh) p.classList.add('pill-active');
      else p.classList.remove('pill-active');
    });

    if (geo) {
      document.querySelectorAll('#geo-pills .pill').forEach(p => {
        const g = p.getAttribute('data-geo');
        let active = false;
        if (geo.mode === 'off' && g === 'Passthrough') active = true;
        else if (geo.mode === 'hide' && g === 'Hide') active = true;
        else if (geo.mode === 'spoof' && geo.preset === g) active = true;

        if (active) p.classList.add('pill-active');
        else p.classList.remove('pill-active');
      });
    }
  }

  // Setup speed pill listeners
  document.querySelectorAll('#speed-pills .pill').forEach(p => {
    p.addEventListener('click', () => {
      const speed = parseFloat(p.getAttribute('data-speed'));
      document.querySelectorAll('#speed-pills .pill').forEach(b => b.classList.remove('pill-active'));
      p.classList.add('pill-active');
      if (currentTabId) {
        browser.runtime.sendMessage({
          type: 'SET_PLAYBACK_SPEED',
          tabId: currentTabId,
          speed: speed
        }).catch(() => {});
      }
    });
  });

  // Setup boost pill listeners
  document.querySelectorAll('#boost-pills .pill').forEach(p => {
    p.addEventListener('click', () => {
      const boost = parseFloat(p.getAttribute('data-boost'));
      document.querySelectorAll('#boost-pills .pill').forEach(b => b.classList.remove('pill-active'));
      p.classList.add('pill-active');
      if (currentTabId) {
        browser.runtime.sendMessage({
          type: 'SET_AUDIO_BOOST',
          tabId: currentTabId,
          boost: boost
        }).catch(() => {});
      }
    });
  });

  // Setup DoH pill listeners
  document.querySelectorAll('#doh-pills .pill').forEach(p => {
    p.addEventListener('click', () => {
      const doh = p.getAttribute('data-doh');
      document.querySelectorAll('#doh-pills .pill').forEach(b => b.classList.remove('pill-active'));
      p.classList.add('pill-active');
      browser.runtime.sendMessage({
        type: 'SET_DOH_PROVIDER',
        doh: doh
      }).catch(() => {});
    });
  });

  // Setup Geo pill listeners
  const GEO_MAP = {
    'Passthrough': { mode: 'off' },
    'Hide': { mode: 'hide' },
    'Tokyo': { mode: 'spoof', preset: 'Tokyo', lat: 35.6762, lng: 139.6503 },
    'London': { mode: 'spoof', preset: 'London', lat: 51.5074, lng: -0.1278 },
    'New York': { mode: 'spoof', preset: 'New York', lat: 40.7128, lng: -74.0060 },
    'Zurich': { mode: 'spoof', preset: 'Zurich', lat: 47.3769, lng: 8.5417 }
  };

  document.querySelectorAll('#geo-pills .pill').forEach(p => {
    p.addEventListener('click', () => {
      const key = p.getAttribute('data-geo');
      const cfg = GEO_MAP[key] || { mode: 'off' };
      document.querySelectorAll('#geo-pills .pill').forEach(b => b.classList.remove('pill-active'));
      p.classList.add('pill-active');
      browser.runtime.sendMessage({
        type: 'SET_GEO',
        tabId: currentTabId,
        mode: cfg.mode,
        preset: key,
        lat: cfg.lat,
        lng: cfg.lng
      }).catch(() => {});
    });
  });

  // DNS Benchmarker & Auto-Fastest Switcher
  const benchBtn = document.getElementById('btn-bench-dns');
  if (benchBtn) {
    benchBtn.addEventListener('click', () => {
      benchBtn.textContent = 'Pinging...';
      browser.runtime.sendMessage({ type: 'BENCHMARK_DNS' }).then(res => {
        benchBtn.textContent = res.fastest ? 'Fastest: ' + res.fastest : 'All providers unreachable';
        if (res.results && Array.isArray(res.results)) {
          res.results.forEach(r => {
            const pill = document.querySelector(`#doh-pills .pill[data-doh="${r.name}"]`);
            if (pill) {
              pill.textContent = r.latency < 999 ? `${r.name} (${r.latency}ms)` : `${r.name} (Err)`;
            }
          });
        }
        if (res.fastest) {
          document.querySelectorAll('#doh-pills .pill').forEach(b => b.classList.remove('pill-active'));
          const fastestPill = document.querySelector(`#doh-pills .pill[data-doh="${res.fastest}"]`);
          if (fastestPill) fastestPill.classList.add('pill-active');
          browser.runtime.sendMessage({ type: 'SET_DOH_PROVIDER', doh: res.fastest }).catch(() => {});
        }
      }).catch(() => {
        benchBtn.textContent = 'Bench DNS';
      });
    });
  }

  // PIN Vault trigger
  const vaultBtn = document.getElementById('btn-open-vault');
  if (vaultBtn) {
    vaultBtn.addEventListener('click', async () => {
      if (currentTabId) {
        browser.tabs.sendMessage(currentTabId, { type: 'OPEN_VAULT_SHEET' })
          .catch(() =>
            browser.tabs.executeScript(currentTabId, { file: 'content.js' })
              .then(() => new Promise(resolve => setTimeout(resolve, 400)))
              .then(() => browser.tabs.sendMessage(currentTabId, { type: 'OPEN_VAULT_SHEET' }))
              .catch(() => {})
          )
          .finally(() => window.close());
      }
    });
  }

  function render(items, tabId) {
    const count = items.length;
    status.textContent = `${count} media found`;

    if (count === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <svg viewBox="0 0 24 24" width="36" height="36" fill="#5A5A72">
            <path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/>
          </svg>
          <div class="empty-title">No Media Detected</div>
          <div class="empty-desc">Play video/audio on this page to capture its stream.</div>
        </div>
      `;
      footer.style.display = 'none';
      return;
    }

    footer.style.display = count > 1 ? 'block' : 'none';

    let html = '';
    items.forEach((item, index) => {
      const isHls = item.format === 'M3U8';
      const isAudio = item.type === 'audio';
      const badgeColor = isHls ? '#FF9100' : isAudio ? '#00E5FF' : '#7C4DFF';

      html += `
        <div class="media-card">
          <div class="card-top">
            <span class="badge-format" style="background:${badgeColor}22; color:${badgeColor}; border:1px solid ${badgeColor}44;">${item.format}</span>
            ${item.quality ? `<span class="badge-format" style="background:rgba(255,255,255,0.08);color:#D0D0E8;">${item.quality}</span>` : ''}
            <span class="badge-size">${item.size || 'Stream'}</span>
          </div>
          <div class="card-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title)}</div>
          <div class="card-actions">
            <button class="btn btn-turbo btn-turbo-dl" data-index="${index}">Turbo</button>
            <button class="btn btn-primary btn-dl" data-index="${index}">Download</button>
            <button class="btn btn-secondary btn-copy" data-url="${escapeHtml(item.url)}">Copy</button>
            <button class="btn btn-secondary btn-play" data-url="${escapeHtml(item.url)}">Play</button>
          </div>
        </div>
      `;
    });

    container.innerHTML = html;

    container.querySelectorAll('.btn-turbo-dl').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.getAttribute('data-index'), 10);
        const item = items[idx];
        if (item) {
          browser.runtime.sendMessage({
            type: 'TRIGGER_TURBO_DOWNLOAD',
            tabId: tabId,
            url: item.url,
            title: item.title,
            format: item.format
          });
          btn.textContent = 'Turbo...';
        }
      });
    });

    container.querySelectorAll('.btn-dl').forEach(btn => {
      btn.addEventListener('click', () => {
        const idx = parseInt(btn.getAttribute('data-index'), 10);
        const item = items[idx];
        if (item) {
          browser.runtime.sendMessage({
            type: 'TRIGGER_DOWNLOAD',
            tabId: tabId,
            url: item.url,
            title: item.title,
            format: item.format
          });
          btn.textContent = 'Downloading...';
        }
      });
    });

    container.querySelectorAll('.btn-copy').forEach(btn => {
      btn.addEventListener('click', () => {
        const url = btn.getAttribute('data-url');
        if (url) {
          navigator.clipboard.writeText(url);
          btn.textContent = 'Copied!';
          setTimeout(() => btn.textContent = 'Copy', 1500);
        }
      });
    });

    container.querySelectorAll('.btn-play').forEach(btn => {
      btn.addEventListener('click', () => {
        const url = btn.getAttribute('data-url');
        if (url) window.open(url, '_blank');
      });
    });
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

  refreshBtn.addEventListener('click', () => {
    browser.tabs.query({ active: true, currentWindow: true }).then(tabs => {
      if (tabs[0]) {
        return browser.tabs.executeScript(tabs[0].id, {
          code: 'window.__praha_media_injected = false; location.reload();'
        });
      }
    }).catch(() => {
      status.textContent = 'Rescan unavailable on this page';
    });
  });

  downloadAllBtn.addEventListener('click', () => {
    browser.tabs.query({ active: true, currentWindow: true }).then(tabs => {
      if (tabs[0]) {
        browser.runtime.sendMessage({
          type: 'DOWNLOAD_ALL',
          tabId: tabs[0].id,
          items: currentItems
        });
      }
    });
  });

  loadMedia();
});
