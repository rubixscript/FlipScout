import React, { useState } from 'react';
import { Sparkles, Trash2, X } from 'lucide-react';
import { money } from '../../styles';
import { PLATFORMS, STATUSES, listingPrompt, profit, roi } from '../../utils/logic';
import { Card, Chip, btn, copy, input, openUrl } from '../ui';

function Num({ t, label, value, onChange }) {
  return (
    <label style={{ fontSize: 11.5, color: t.muted, fontWeight: 700 }}>
      {label}
      <input type="number" min="0" step="0.01" value={value || ''} placeholder="0" onChange={(e) => onChange(parseFloat(e.target.value) || 0)} style={input(t, { marginTop: 4 })} />
    </label>
  );
}

export default function Editor({ t, item, sellers, categories, onChange, onClose, onDelete, toast }) {
  const set = (patch) => onChange({ ...item, ...patch });
  const p = profit(item);
  const [tagText, setTagText] = useState((item.tags || []).join(', '));
  const sold = item.status === 'SOLD';
  const prompt = (platform) => {
    copy(listingPrompt(item, platform));
    toast('Listing prompt copied — paste it into ChatGPT or Gemini with your photos');
  };
  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
        <button style={btn(t)} onClick={onClose}><X size={14} /> Done</button>
        <div style={{ flex: 1, fontWeight: 800, fontSize: 15 }}>Edit flip</div>
        <button style={btn(t, { style: { color: t.loss } })} onClick={onDelete}><Trash2 size={14} /> Delete</button>
      </div>
      <input value={item.title} onChange={(e) => set({ title: e.target.value })} placeholder="Title" style={input(t, { marginBottom: 10, fontWeight: 700 })} />
      <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
        {STATUSES.map((s) => (
          <Chip key={s} t={t} active={item.status === s} onClick={() => set({ status: s, soldAt: s === 'SOLD' ? item.soldAt || Date.now() : item.soldAt })}>{s[0] + s.slice(1).toLowerCase()}</Chip>
        ))}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 8, marginBottom: 10 }}>
        <Num t={t} label="Bought for" value={item.buy} onChange={(v) => set({ buy: v })} />
        <Num t={t} label={sold ? 'Sold for' : 'Target price'} value={item.sell} onChange={(v) => set({ sell: v })} />
        <Num t={t} label="Fees" value={item.fees} onChange={(v) => set({ fees: v })} />
        <Num t={t} label="Shipping" value={item.ship} onChange={(v) => set({ ship: v })} />
      </div>
      <Card t={t} style={{ padding: '10px 14px', marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <span style={{ color: t.muted, fontSize: 13 }}>{sold ? 'Profit' : 'Projected profit'}</span>
        <b style={{ color: p < 0 ? t.loss : t.emerald }}>{money(Math.round(p * 100) / 100)} · {Math.round(roi(item))}% ROI</b>
      </Card>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 80px', gap: 8, marginBottom: 10 }}>
        <input list="fs-cats" value={item.cat} onChange={(e) => set({ cat: e.target.value })} placeholder="Category" style={input(t)} />
        <datalist id="fs-cats">{categories.map((c) => <option key={c} value={c} />)}</datalist>
        <select value={item.plat} onChange={(e) => set({ plat: e.target.value })} style={input(t)}>
          <option value="">Platform</option>
          {PLATFORMS.map((x) => <option key={x}>{x}</option>)}
        </select>
        <input type="number" min="1" value={item.qty} onChange={(e) => set({ qty: Math.max(1, parseInt(e.target.value, 10) || 1) })} title="Quantity" style={input(t)} />
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
        <input value={tagText} onChange={(e) => { setTagText(e.target.value); set({ tags: e.target.value.split(',').map((x) => x.trim()).filter(Boolean) }); }} placeholder="Tags, comma separated" style={input(t)} />
        <select value={item.seller} onChange={(e) => set({ seller: e.target.value })} style={input(t)}>
          <option value="">No seller</option>
          {sellers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
      </div>
      <input value={item.link} onChange={(e) => set({ link: e.target.value })} placeholder="Listing link" style={input(t, { marginBottom: 10 })} />
      <textarea value={item.notes} onChange={(e) => set({ notes: e.target.value })} placeholder="Notes / condition" rows={3} style={input(t, { marginBottom: 12, resize: 'vertical' })} />
      <Card t={t}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, fontWeight: 700, fontSize: 13.5 }}>
          <Sparkles size={15} /> Listing assistant
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={btn(t, { primary: true })} onClick={() => prompt('eBay')}>Copy eBay prompt</button>
          <button style={btn(t)} onClick={() => prompt('Facebook Marketplace')}>Copy Marketplace prompt</button>
          <button style={btn(t)} onClick={() => { prompt('eBay'); openUrl('https://chatgpt.com/'); }}>Copy + open ChatGPT</button>
        </div>
      </Card>
    </div>
  );
}
