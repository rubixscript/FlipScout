import React from 'react';

export default function TabBar({ t, tabs, active, onChange }) {
  return (
    <div style={{ display: 'flex', gap: 4, background: t.surfaceVar, borderRadius: 14, padding: 4, marginBottom: 14 }}>
      {tabs.map(({ id, label, Icon }) => (
        <button
          key={id}
          onClick={() => onChange(id)}
          style={{
            flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7, border: 'none', cursor: 'pointer',
            borderRadius: 10, padding: '9px 0', fontSize: 13.5, fontWeight: 700, fontFamily: 'inherit',
            background: active === id ? t.bg : 'transparent', color: active === id ? t.text : t.muted,
          }}
        >
          <Icon size={15} /> {label}
        </button>
      ))}
    </div>
  );
}
