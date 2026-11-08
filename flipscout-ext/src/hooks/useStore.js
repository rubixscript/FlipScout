import { useCallback, useEffect, useState } from 'react';
import { loadAll, onStorageChanged, saveKey } from '../utils/storage';

// Loads items/sellers/searches from chrome.storage.local and keeps them live.
// Setters persist immediately (no save button) and update state optimistically.
export default function useStore() {
  const [data, setData] = useState({ items: [], sellers: [], searches: [] });

  useEffect(() => {
    const refresh = () => loadAll().then(setData);
    refresh();
    return onStorageChanged(refresh);
  }, []);

  const setter = useCallback(
    (key) => (value) => {
      setData((d) => ({ ...d, [key]: value }));
      saveKey(key, value);
    },
    []
  );

  return {
    ...data,
    setItems: setter('items'),
    setSellers: setter('sellers'),
    setSearches: setter('searches'),
  };
}
