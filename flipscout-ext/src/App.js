/* global chrome */
import React, { useState } from 'react';
import { BarChart3, Package, Search, Store } from 'lucide-react';
import useStore from './hooks/useStore';
import useTheme from './hooks/useTheme';
import useToast from './hooks/useToast';
import useActiveListing from './hooks/useActiveListing';
import useFeedScan from './hooks/useFeedScan';
import Header from './components/Header';
import TabBar from './components/TabBar';
import CaptureCard from './components/CaptureCard';
import ScanModal from './components/ScanModal';
import Toast from './components/Toast';
import Items from './tabs/Items';
import Sellers from './tabs/Sellers';
import Insights from './tabs/Insights';
import SearchTab from './tabs/Search';
import { importBackup } from './utils/backup';

const TABS = [
  { id: 'items', label: 'Items', Icon: Package },
  { id: 'sellers', label: 'Sellers', Icon: Store },
  { id: 'insights', label: 'Insights', Icon: BarChart3 },
  { id: 'search', label: 'Search', Icon: Search },
];

export default function App() {
  const t = useTheme();
  const store = useStore();
  const { toast, show, dismiss } = useToast();
  const listing = useActiveListing();
  const scan = useFeedScan();
  const [tab, setTab] = useState('items');
  // fullscreen.html mounts the same app with a full-width dashboard layout.
  const fullscreen = document.body.classList.contains('fullscreen');

  const exportBackup = () => chrome.runtime.sendMessage({ action: 'exportBackup' });
  const openFullscreen = () => chrome.tabs.create({ url: chrome.runtime.getURL('fullscreen.html') });
  const runScan = async () => {
    const res = await scan.scan();
    if (!res || !res.ok) show('Open a Facebook Marketplace feed, then Scan');
    else if (!res.listings.length) show('No listings found — scroll the feed a little, then Scan again');
  };
  const addScanned = async (selected) => {
    const res = await scan.add(selected);
    scan.close();
    if (res && res.ok && res.added > 0) {
      show(`Added ${res.added} listing${res.added === 1 ? '' : 's'}`, () => {
        (res.ids || []).forEach((id) => chrome.runtime.sendMessage({ action: 'undoSave', id }));
      });
    }
  };
  const onImport = async () => {
    try {
      const merged = importBackup(await navigator.clipboard.readText(), store);
      if (!merged) return show('Copy the app’s backup JSON first, then tap Import');
      store.setItems(merged.items);
      store.setSellers(merged.sellers);
      show(`Imported ${merged.addedItems} item${merged.addedItems === 1 ? '' : 's'}`, () => {
        store.setItems(store.items);
        store.setSellers(store.sellers);
      });
    } catch (e) {
      show('Could not read the clipboard');
    }
  };

  return (
    <div style={{
      background: t.bg, color: t.text, minHeight: fullscreen ? '100vh' : 560,
      padding: fullscreen ? '24px 28px 90px' : '16px 18px 70px',
    }}>
      <Header t={t} count={store.items.length} onScan={runScan} canScan={scan.canScan} onExport={exportBackup} onImport={onImport} onExpand={fullscreen ? null : openFullscreen} />
      <CaptureCard
        t={t}
        phase={listing.phase}
        listing={listing.listing}
        savedItem={listing.savedItem}
        busy={listing.busy}
        onSave={listing.save}
        onTrackLink={listing.saveFromUrl}
        onUndo={listing.undo}
      />
      <TabBar t={t} tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'items' && <Items t={t} items={store.items} sellers={store.sellers} setItems={store.setItems} toast={show} />}
      {tab === 'sellers' && <Sellers t={t} items={store.items} sellers={store.sellers} setSellers={store.setSellers} toast={show} />}
      {tab === 'insights' && <Insights t={t} items={store.items} />}
      {tab === 'search' && <SearchTab t={t} searches={store.searches} setSearches={store.setSearches} toast={show} />}
      {scan.listings && (
        <ScanModal
          t={t}
          listings={scan.listings}
          existingLinks={store.items.map((i) => i.link)}
          busy={scan.busy}
          onAdd={addScanned}
          onClose={scan.close}
        />
      )}
      <Toast t={t} toast={toast} onDismiss={dismiss} />
    </div>
  );
}
