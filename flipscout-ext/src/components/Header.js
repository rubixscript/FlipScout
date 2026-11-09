import React, { useState } from 'react';
import { Download, ListChecks, Maximize2, MoreHorizontal, Upload } from 'lucide-react';
import { btn } from '../tabs/ui';

export default function Header({ t, count, onScan, canScan, onExport, onImport, onExpand }) {
  const [menu, setMenu] = useState(false);
  const item = { ...btn(t), width: '100%', justifyContent: 'flex-start', border: 'none', borderRadius: 8 };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '2px 2px 12px', position: 'relative' }}>
      <div style={{ width: 28, height: 28, borderRadius: 9, background: 'linear-gradient(160deg,#5A5B63,#151517)', color: t.emerald, fontWeight: 800, fontSize: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>F</div>
      <div style={{ fontWeight: 800, fontSize: 19, letterSpacing: -0.4, flex: 1 }}>
        FlipScout <span style={{ color: t.muted, fontSize: 12.5, fontWeight: 600 }}>{count > 0 ? `${count} tracked` : ''}</span>
      </div>
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
      {onExpand && <button style={btn(t)} onClick={onExpand} title="Open full screen"><Maximize2 size={14} /></button>}
      <button style={btn(t)} onClick={() => setMenu(!menu)} title="Import / Export"><MoreHorizontal size={14} /></button>
      {menu && (
        <div
          onMouseLeave={() => setMenu(false)}
          style={{ position: 'absolute', right: 0, top: 40, zIndex: 20, background: t.bg, border: `1px solid ${t.outlineVar}`, borderRadius: 12, padding: 4, minWidth: 190, boxShadow: '0 10px 30px rgba(0,0,0,0.25)' }}
        >
          <button style={item} onClick={() => { setMenu(false); onImport(); }}><Upload size={14} /> Import from clipboard</button>
          <button style={item} onClick={() => { setMenu(false); onExport(); }} disabled={!count}><Download size={14} /> Export backup</button>
        </div>
      )}
    </div>
  );
}
