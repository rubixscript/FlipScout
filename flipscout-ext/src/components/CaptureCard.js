import React, { useState } from 'react';
import { ClipboardPaste, PackageSearch, RotateCcw, Tag } from 'lucide-react';
import { PALETTE, money } from '../styles';
import { isListingUrl } from '../utils/logic';
import { Card, btn, input } from '../tabs/ui';

function Thumb({ t, url }) {
  const [ok, setOk] = useState(true);
  const box = { width: 64, height: 64, borderRadius: 14, flexShrink: 0 };
  if (!url || !ok) {
    return (
      <div style={{ ...box, background: t.surfaceVar, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.muted }}>
        <Tag size={20} />
      </div>
    );
  }
  return <img src={url} alt="" onError={() => setOk(false)} style={{ ...box, objectFit: 'cover' }} />;
}

const Badge = ({ color, children }) => (
  <div style={{ width: 36, height: 36, borderRadius: 999, background: color + '22', color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, flexShrink: 0 }}>
    {children}
  </div>
);

// One-line capture for a listing link from anywhere (chat, search results, email):
// paste button + input + track. Background opens the link quietly and extracts it.
function LinkCapture({ t, busy, onTrack }) {
  const [text, setText] = useState('');
  const [note, setNote] = useState('');

  const track = async () => {
    const url = text.trim();
    if (!isListingUrl(url)) {
      setNote('Paste an eBay or Facebook Marketplace listing link');
      return;
    }
    setNote('');
    const res = await onTrack(url);
    if (res && !res.ok) setNote('Could not read that listing — open it in a tab and use the button');
  };

  const paste = async () => {
    try {
      const clip = await navigator.clipboard.readText();
      setText(clip.trim());
      setNote('');
    } catch (e) {
      setNote('Clipboard blocked — paste the link manually');
    }
  };

  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <input
          style={input(t, { flex: 1, padding: '9px 12px', fontSize: 13.5 })}
          placeholder="or paste a listing link"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !busy && track()}
          spellCheck={false}
        />
        <button style={btn(t, {})} onClick={paste} title="Paste from clipboard">
          <ClipboardPaste size={15} />
        </button>
        <button style={btn(t, { primary: true })} onClick={track} disabled={busy || !text.trim()}>
          {busy ? '…' : 'Track'}
        </button>
      </div>
      {note && <div style={{ color: t.muted, fontSize: 12, marginTop: 6 }}>{note}</div>}
    </div>
  );
}

// Top-of-popup card for the active tab. Renders nothing while checking or when the page
// has no listing (the tabs below already explain what to do), so it costs no space.
export default function CaptureCard({ t, phase, listing, savedItem, busy, onSave, onTrackLink, onUndo }) {
  if (phase === 'ready' && listing) {
    return (
      <Card t={t} style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 14 }}>
        <Thumb t={t} url={listing.photoUrl} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14.5, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{listing.title || 'Untitled listing'}</div>
          <div style={{ marginTop: 4, display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <span style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.5 }}>{listing.price > 0 ? money(listing.price) : '—'}</span>
            <span style={{ color: t.muted, fontSize: 12 }}>{listing.platform === 'EBAY' ? 'eBay' : 'Marketplace'}</span>
          </div>
        </div>
        <button style={btn(t, { primary: true, style: { height: 44, padding: '0 20px', borderRadius: 22 } })} onClick={onSave} disabled={busy}>
          <Tag size={15} /> {busy ? 'Saving…' : 'Track this flip'}
        </button>
      </Card>
    );
  }
  if (phase === 'saved' && savedItem) {
    return (
      <Card t={t} style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 12 }}>
        <Badge color={t.emerald}>✓</Badge>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Tracking “{savedItem.title || 'listing'}”</div>
          <div style={{ color: t.muted, fontSize: 12, marginTop: 2 }}>Saved as WATCHING {savedItem.buy > 0 ? 'at ' + money(savedItem.buy) : ''}</div>
        </div>
        <button style={btn(t)} onClick={onUndo}><RotateCcw size={14} /> Undo</button>
      </Card>
    );
  }
  if (phase === 'duplicate' && savedItem) {
    return (
      <Card t={t} style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 12 }}>
        <Badge color={PALETTE.amber}>!</Badge>
        <div>
          <div style={{ fontSize: 14, fontWeight: 700 }}>Already tracking this</div>
          <div style={{ color: t.muted, fontSize: 12, marginTop: 2 }}>Status {savedItem.status}</div>
        </div>
      </Card>
    );
  }
  if (phase === 'empty') {
    return (
      <div style={{ margin: '0 2px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: t.muted, fontSize: 12.5 }}>
          <PackageSearch size={15} /> Open an eBay or Marketplace listing to track it in one click (or right-click the listing / press Alt+Shift+S).
        </div>
        <LinkCapture t={t} busy={busy} onTrack={onTrackLink} />
      </div>
    );
  }
  return null;
}
