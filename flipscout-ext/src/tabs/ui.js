/* global chrome */
import React, { useState } from 'react';
import { Trash2 } from 'lucide-react';
import { STATUS_COLOR } from '../styles';

export const Card = ({ t, children, style }) => (
  <div style={{ background: t.surface, borderRadius: 18, padding: 16, border: `1px solid ${t.outlineVar}`, ...style }}>{children}</div>
);

export const StatusPill = ({ status, onClick, title }) => {
  const c = STATUS_COLOR[status] || STATUS_COLOR.WATCHING;
  return (
    <span
      onClick={onClick}
      title={title}
      style={{ color: c, background: c + '22', borderRadius: 999, fontSize: 11, fontWeight: 700, letterSpacing: 0.4, padding: '4px 10px', whiteSpace: 'nowrap', cursor: onClick ? 'pointer' : 'default' }}
    >
      {status}
    </span>
  );
};

export const btn = (t, o = {}) => ({
  display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6, cursor: 'pointer', fontFamily: 'inherit',
  borderRadius: 12, fontSize: 13.5, fontWeight: 600, padding: '9px 14px',
  background: o.primary ? t.ink : 'transparent', color: o.primary ? t.onInk : t.text,
  border: o.primary ? 'none' : `1px solid ${t.outline}`, ...(o.style || {}),
});

export const input = (t, extra) => ({
  background: t.surfaceVar, color: t.text, border: `1px solid ${t.outlineVar}`, borderRadius: 12,
  padding: '10px 12px', fontSize: 14, fontFamily: 'inherit', outline: 'none', width: '100%', ...extra,
});

export const Chip = ({ t, active, children, onClick }) => (
  <button
    onClick={onClick}
    style={{ border: `1px solid ${active ? t.ink : t.outline}`, background: active ? t.ink : 'transparent', color: active ? t.onInk : t.muted, borderRadius: 999, padding: '5px 12px', fontSize: 12.5, fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit' }}
  >
    {children}
  </button>
);

export const Empty = ({ t, icon, title, hint }) => (
  <div style={{ textAlign: 'center', padding: '40px 20px', color: t.muted }}>
    <div style={{ marginBottom: 10 }}>{icon}</div>
    <div style={{ fontSize: 15, fontWeight: 700, color: t.text }}>{title}</div>
    <div style={{ fontSize: 13, marginTop: 6, lineHeight: 1.5 }}>{hint}</div>
  </div>
);

export const openUrl = (url) => url && chrome.tabs.create({ url });
export const copy = (text) => navigator.clipboard.writeText(text);

// Two-step delete: a trash icon that turns into "Delete? / Cancel" in place (no browser dialog).
export function ConfirmDelete({ t, label, onConfirm }) {
  const [asking, setAsking] = useState(false);
  if (!asking) {
    return <button onClick={() => setAsking(true)} title={label} style={{ ...btn(t), border: 'none', padding: 6, color: t.loss }}><Trash2 size={15} /></button>;
  }
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12.5, fontWeight: 600 }}>
      <button onClick={() => { setAsking(false); onConfirm(); }} style={btn(t, { style: { padding: '4px 10px', fontSize: 12, borderRadius: 999, color: t.loss, borderColor: t.loss } })}>Delete</button>
      <button onClick={() => setAsking(false)} style={btn(t, { style: { padding: '4px 10px', fontSize: 12, borderRadius: 999 } })}>Cancel</button>
    </span>
  );
}

// Same two-step delete, sized for a tile: a trash chip beside the count badge that covers the
// tile with a confirm overlay (title + hint + Delete/Cancel). Stops clicks so the tile never opens.
export function TileDelete({ t, title, hint, onConfirm }) {
  const [asking, setAsking] = useState(false);
  if (!asking) {
    return (
      <button
        onClick={(e) => { e.stopPropagation(); setAsking(true); }}
        title={title}
        style={{ position: 'absolute', top: 7, right: 46, width: 26, height: 26, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0, border: 'none', borderRadius: 999, background: 'rgba(0,0,0,0.6)', color: '#fff', cursor: 'pointer' }}
      >
        <Trash2 size={13} />
      </button>
    );
  }
  return (
    <div onClick={(e) => e.stopPropagation()} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.72)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 7, padding: 12, textAlign: 'center', color: '#fff' }}>
      <div style={{ fontSize: 13, fontWeight: 800, lineHeight: 1.25 }}>{title}</div>
      <div style={{ fontSize: 11.5, opacity: 0.75 }}>{hint}</div>
      <div style={{ display: 'flex', gap: 6, marginTop: 2 }}>
        <button onClick={(e) => { e.stopPropagation(); setAsking(false); onConfirm(); }} style={{ border: 'none', borderRadius: 999, padding: '5px 13px', fontSize: 12, fontWeight: 700, cursor: 'pointer', fontFamily: 'inherit', background: t.loss, color: '#fff' }}>Delete</button>
        <button onClick={(e) => { e.stopPropagation(); setAsking(false); }} style={{ border: 'none', borderRadius: 999, padding: '5px 13px', fontSize: 12, fontWeight: 700, cursor: 'pointer', fontFamily: 'inherit', background: 'rgba(255,255,255,0.18)', color: '#fff' }}>Cancel</button>
      </div>
    </div>
  );
}
