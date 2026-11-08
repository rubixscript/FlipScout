/* global chrome */
import { useCallback, useEffect, useState } from 'react';

// Bulk feed scan: finds an open Marketplace feed tab (works from the popup and the
// fullscreen page alike — the feed does not have to be the active tab), asks its content
// script for every listing card it has seen, and saves only what the user picks in the
// modal. Nothing is written until Add.
// Add eBay search pages here when scanEbaySearch lands in content.js.
const FEED_TAB_PATTERNS = ['https://www.facebook.com/marketplace/*'];

export default function useFeedScan() {
  const [tabId, setTabId] = useState(null); // feed tab that answered ping with list:true
  const [listings, setListings] = useState(null); // null = modal closed
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const tabs = [
          ...(await chrome.tabs.query({ active: true, lastFocusedWindow: true })),
          ...(await chrome.tabs.query({ url: FEED_TAB_PATTERNS })),
        ];
        const seen = new Set();
        for (const tab of tabs) {
          if (!tab || !tab.id || seen.has(tab.id)) continue;
          seen.add(tab.id);
          const ping = await chrome.tabs.sendMessage(tab.id, { action: 'ping' }).catch(() => null);
          if (ping && ping.ok && ping.list) return setTabId(tab.id);
        }
      } catch (e) {
        /* no feed tab open — Scan stays disabled */
      }
    })();
  }, []);

  // Returns the scan result so the caller can hint "scroll the feed, then Scan again".
  const scan = useCallback(async () => {
    if (!tabId || busy) return { ok: false, error: 'no feed tab' };
    setBusy(true);
    try {
      const res = await chrome.tabs.sendMessage(tabId, { action: 'scanListings' }).catch(() => null);
      if (!res || !res.ok) return res || { ok: false, error: 'feed tab is gone' };
      if (res.listings.length) setListings(res.listings);
      return res;
    } finally {
      setBusy(false);
    }
  }, [tabId, busy]);

  // Saves only the picked rows (the modal never passes duplicates). Returns counts + ids.
  const add = useCallback(async (selected) => {
    if (!selected || !selected.length || busy) return { ok: false, added: 0, ids: [] };
    setBusy(true);
    try {
      return await chrome.runtime.sendMessage({ action: 'saveListings', listings: selected });
    } finally {
      setBusy(false);
    }
  }, [busy]);

  const close = useCallback(() => setListings(null), []);

  return { canScan: tabId !== null, listings, busy, scan, add, close };
}
