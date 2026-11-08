import React, { useState } from 'react';
import { ExternalLink, Star, Store, Trash2 } from 'lucide-react';
import { money } from '../styles';
import { profit } from '../utils/logic';
import { Card, Empty, btn, input, openUrl } from './ui';

const host = (url) => (url || '').replace(/^[a-z]+:\/\//, '').split('/')[0].replace(/^(www|m)\./, '');

export default function Sellers({ t, sellers, items, setSellers, toast }) {
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [openId, setOpenId] = useState(null);

  const add = () => {
    if (!name.trim()) return;
    setSellers([{ id: crypto.randomUUID(), name: name.trim(), url: url.trim(), notes: '', fav: false, at: Date.now() }, ...sellers]);
    setName(''); setUrl('');
  };
  const update = (s) => setSellers(sellers.map((x) => (x.id === s.id ? s : x)));
  const remove = (s) => {
    setSellers(sellers.filter((x) => x.id !== s.id));
    toast('Seller deleted', () => setSellers(sellers));
  };
  const sorted = [...sellers].sort((a, b) => Number(b.fav) - Number(a.fav) || b.at - a.at);

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} placeholder="Seller name" style={input(t, { flex: 1 })} />
        <input value={url} onChange={(e) => setUrl(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} placeholder="Profile link (optional)" style={input(t, { flex: 1.3 })} />
        <button style={btn(t, { primary: true })} onClick={add} disabled={!name.trim()}>+ Add</button>
      </div>
      {sorted.length === 0 && (
        <Empty t={t} icon={<Store size={36} strokeWidth={1.5} />} title="No sellers yet" hint="Sellers are saved automatically with the first listing you track from them. You can add one by hand above." />
      )}
      {sorted.map((s) => {
        const theirs = items.filter((i) => i.seller === s.id);
        const sold = theirs.filter((i) => i.status === 'SOLD');
        const total = sold.reduce((a, i) => a + profit(i), 0);
        const expanded = openId === s.id;
        return (
          <Card key={s.id} t={t} style={{ marginBottom: 8, padding: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <button onClick={() => update({ ...s, fav: !s.fav })} style={{ ...btn(t), padding: '6px 8px' }} title="Favourite">
                <Star size={15} fill={s.fav ? '#F0A93B' : 'none'} color={s.fav ? '#F0A93B' : t.muted} />
              </button>
              <div onClick={() => setOpenId(expanded ? null : s.id)} style={{ flex: 1, minWidth: 0, cursor: 'pointer' }}>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{s.name}</div>
                <div style={{ fontSize: 12, color: t.muted }}>
                  {host(s.url) || 'no link'} · {theirs.length} item{theirs.length === 1 ? '' : 's'}
                  {sold.length > 0 && <span> · {sold.length} sold, <b style={{ color: total < 0 ? t.loss : t.emerald }}>{money(Math.round(total * 100) / 100)}</b></span>}
                </div>
              </div>
              {s.url && <button style={{ ...btn(t), padding: '6px 8px' }} onClick={() => openUrl(s.url)}><ExternalLink size={14} /></button>}
            </div>
            {expanded && (
              <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
                <input value={s.notes} onChange={(e) => update({ ...s, notes: e.target.value })} placeholder="Notes (good prices, reliable…)" style={input(t, { flex: 1 })} />
                <button style={btn(t, { style: { color: t.loss } })} onClick={() => remove(s)}><Trash2 size={14} /></button>
              </div>
            )}
          </Card>
        );
      })}
    </div>
  );
}
