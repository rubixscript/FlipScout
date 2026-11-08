/* global chrome */
// chrome.storage.local is the single source of truth (house pattern). Keys mirror the app:
// items, sellers, searches (SavedSearch: {id,q,pin,cat,at}).

const KEYS = ['items', 'sellers', 'searches'];

export async function loadAll() {
  const { items = [], sellers = [], searches = [] } = await chrome.storage.local.get(KEYS);
  return { items, sellers, searches };
}

export const saveKey = (key, value) => chrome.storage.local.set({ [key]: value });

export function onStorageChanged(callback) {
  const listener = (changes, area) => {
    if (area === 'local' && KEYS.some((k) => changes[k])) callback();
  };
  chrome.storage.onChanged.addListener(listener);
  return () => chrome.storage.onChanged.removeListener(listener);
}
