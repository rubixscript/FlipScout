import React from 'react';
import { Download, ListChecks, Maximize2, Upload } from 'lucide-react';
import { btn } from '../tabs/ui';

export default function Header({ t, count, onScan, canScan, onExport, onImport, onExpand }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '2px 2px 12px' }}>
      <div style={{ width: 28, height: 28, borderRadius: 9, background: 'linear-gradient(160deg,#5A5B63,#151517)', color: t.emerald, fontWeight: 800, fontSize: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>F</div>
      <div style={{ fontWeight: 800, fontSize: 19, letterSpacing: -0.4, flex: 1 }}>
        FlipScout <span style={{ color: t.muted, fontSize: 12.5, fontWeight: 600 }}>{count > 0 ? `${count} tracked` : ''}</span>
      </div>
      {onExpand && <button style={btn(t)} onClick={onExpand} title="Open full screen"><Maximize2 size={14} /></button>}
      {onScan && (
        <button
          style={btn(t)}
          onClick={onScan}
          disabled={!canScan}
          title={canScan ? 'Capture every listing visible in the Marketplace feed' : 'Open a Facebook Marketplace feed to scan'}
        >
          <ListChecks size={14} /> Scan
        </button>
      )}
      <button style={btn(t)} onClick={onImport} title="Import a backup from the app"><Upload size={14} /> Import</button>
      <button style={btn(t)} onClick={onExport} disabled={!count} title="Export a backup the app can import"><Download size={14} /> Export</button>
    </div>
  );
}
