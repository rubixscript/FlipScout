/* global chrome */
import { useCallback, useEffect, useState } from 'react';

// Phases: checking -> ready (listing detected) | empty; ready -> saved | duplicate after save().
export default function useActiveListing() {
  const [phase, setPhase] = useState('checking');
  const [listing, setListing] = useState(null);
  const [savedItem, setSavedItem] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [tab] = await chrome.tabs.query({ active: true, lastFocusedWindow: true });
        if (!tab || !tab.id) return setPhase('empty');
        const ping = await chrome.tabs.sendMessage(tab.id, { action: 'ping' }).catch(() => null);
        if (!ping || !ping.ok || !ping.platform) return setPhase('empty');
        const res = await chrome.tabs.sendMessage(tab.id, { action: 'extractListing' });
        if (!res || !res.ok) return setPhase('empty');
        setListing(res.listing);
        setPhase('ready');
      } catch (e) {
        setPhase('empty');
      }
    })();
  }, []);

  const save = useCallback(async () => {
    if (!listing || busy) return;
    setBusy(true);
    try {
      const res = await chrome.runtime.sendMessage({ action: 'saveListing', listing });
      if (res && res.ok) {
        setSavedItem(res.item);
        setPhase(res.duplicate ? 'duplicate' : 'saved');
      }
    } finally {
      setBusy(false);
    }
  }, [listing, busy]);

  const undo = useCallback(async () => {
    if (!savedItem) return;
    await chrome.runtime.sendMessage({ action: 'undoSave', id: savedItem.id });
    setSavedItem(null);
    setPhase('ready');
  }, [savedItem]);

  // Capture from a pasted link (popup) — background reuses an open tab or opens one
  // quietly. Returns the save result so the caller can show an inline hint on failure.
  const saveFromUrl = useCallback(async (url) => {
    if (busy) return { ok: false, error: 'busy' };
    setBusy(true);
    try {
      const res = await chrome.runtime.sendMessage({ action: 'saveFromUrl', url });
      if (res && res.ok) {
        setSavedItem(res.item);
        setPhase(res.duplicate ? 'duplicate' : 'saved');
      }
      return res || { ok: false, error: 'no response' };
    } finally {
      setBusy(false);
    }
  }, [busy]);

  return { phase, listing, savedItem, busy, save, saveFromUrl, undo };
}
