import React, { useEffect, useMemo, useState } from 'react';
import { Check } from 'lucide-react';
import { btn } from '../tabs/ui';
import { PALETTE } from '../styles';

// Review-before-add for feed scans (RedScout's bulk-select pattern): every card the
// content script found, already-tracked ones pre-flagged and locked off, and nothing
// written until the user clicks Add.
const Thumb = ({ t, url }) => (
  <div style={{ width: 44, height: 44, borderRadius: 10, background: t.surfaceVar, flexShrink: 0, overflow: 'hidden' }}>
    {url ? (
      <img src={url} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} onError={(e) => { e.currentTarget.style.display = 'none'; }} />
    ) : null}
  </div>
);

export default function ScanModal({ t, listings, existingLinks, busy, onAdd, onClose }) {
  // Links saved before URL canonicalization landed may still carry a trailing slash.
  const tracked = useMemo(
    () => new Set((existingLinks || []).map((l) => String(l || '').replace(/\/+$/, ''))),
    [existingLinks]
  );
  const [picked, setPicked] = useState(
    () => new Set(listings.filter((l) => !tracked.has(l.url)).map((l) => l.url))
  );
  const dupCount = listings.filter((l) => tracked.has(l.url)).length;
  const fresh = listings.filter((l) => !tracked.has(l.url));

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && !busy && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [busy, onClose]);

  const toggle = (url) => setPicked((prev) => {
    const next = new Set(prev);
    if (next.has(url)) next.delete(url);
    else next.add(url);
    return next;
  });

  const mini = { background: 'none', border: 'none', color: t.muted, fontSize: 12.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit', padding: '4px 6px' };

  return (
    <div
      onClick={busy ? undefined : onClose}
      style={{ position: 'fixed', inset: 0, zIndex: 50, background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{ background: t.bg, border: `1px solid ${t.outlineVar}`, borderRadius: 18, padding: 16, width: 'min(560px, 100%)', maxHeight: '90vh', display: 'flex', flexDirection: 'column', boxShadow: '0 24px 60px rgba(0,0,0,0.35)' }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 10 }}>
          <div style={{ fontWeight: 800, fontSize: 16, flex: 1 }}>
            Scan results{' '}
            <span style={{ color: t.muted, fontSize: 12.5, fontWeight: 600 }}>
              {listings.length} found{dupCount ? ` · ${dupCount} already tracked` : ''}
            </span>
          </div>
          <button style={mini} onClick={() => setPicked(new Set(fresh.map((l) => l.url)))} disabled={busy}>All</button>
          <button style={mini} onClick={() => setPicked(new Set())} disabled={busy}>None</button>
        </div>

        <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 2 }}>
          {listings.map((l) => {
            const isDup = tracked.has(l.url);
            const on = !isDup && picked.has(l.url);
            return (
              <div
                key={l.url}
                onClick={() => !isDup && !busy && toggle(l.url)}
                style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', borderRadius: 12, cursor: isDup ? 'default' : 'pointer', background: on ? t.surfaceVar : 'transparent', opacity: isDup ? 0.55 : 1 }}
              >
                <div style={{ width: 18, height: 18, borderRadius: 6, flexShrink: 0, border: `1.5px solid ${on ? t.ink : t.outline}`, background: on ? t.ink : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {on && <Check size={13} strokeWidth={3} color={t.onInk} />}
                </div>
                <Thumb t={t} url={l.photoUrl} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13.5, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {l.title || 'Untitled listing'}
                  </div>
                  <div style={{ fontSize: 12, color: t.muted, marginTop: 2 }}>{l.priceText || (l.price ? `$${l.price}` : '—')}</div>
                </div>
                {isDup && (
                  <span style={{ fontSize: 11, fontWeight: 700, color: PALETTE.amber, background: PALETTE.amber + '22', borderRadius: 999, padding: '3px 9px', flexShrink: 0 }}>Tracked</span>
                )}
              </div>
            );
          })}
        </div>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 12 }}>
          <button style={btn(t)} onClick={onClose} disabled={busy}>Cancel</button>
          <button
            style={btn(t, { primary: true })}
            disabled={!picked.size || busy}
            onClick={() => onAdd(listings.filter((l) => picked.has(l.url)))}
          >
            {busy ? 'Adding…' : `Add ${picked.size}`}
          </button>
        </div>
      </div>
    </div>
  );
}
