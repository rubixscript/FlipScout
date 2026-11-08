import React from 'react';

export default function Toast({ t, toast, onDismiss }) {
  if (!toast) return null;
  return (
    <div
      style={{
        position: 'fixed', left: 16, right: 16, bottom: 14, background: t.ink, color: t.onInk, borderRadius: 14,
        padding: '11px 16px', display: 'flex', alignItems: 'center', gap: 12, fontSize: 13.5, fontWeight: 600,
        boxShadow: '0 6px 24px rgba(0,0,0,.3)', zIndex: 10,
      }}
    >
      <span style={{ flex: 1 }}>{toast.message}</span>
      {toast.undo && (
        <button
          onClick={() => { toast.undo(); onDismiss(); }}
          style={{ background: 'transparent', border: 'none', color: '#34B27A', fontWeight: 800, fontSize: 13.5, cursor: 'pointer', fontFamily: 'inherit' }}
        >
          UNDO
        </button>
      )}
    </div>
  );
}
