import React, { useState } from 'react';
import { ExternalLink, TrendingUp } from 'lucide-react';
import { money } from '../../styles';
import { ageDays, isStale, nextStatus, profit, roi, searchUrl } from '../../utils/logic';
import { StatusPill, btn, openUrl } from '../ui';

export default function ItemRow({ t, item, onOpen, onAdvance }) {
  const [hover, setHover] = useState(false);
  const p = profit(item);
  const stale = isStale(item);
  // A watched listing hasn't been bought yet: its price is what the seller is asking.
  const buyLabel = item.status === 'WATCHING' ? 'asking' : 'paid';
  return (
    <div onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 4px', borderBottom: `1px solid ${t.outlineVar}` }}>
      {item.photoUrl ? (
        <img src={item.photoUrl} alt="" style={{ width: 44, height: 44, borderRadius: 10, objectFit: 'cover' }} />
      ) : (
        <div style={{ width: 44, height: 44, borderRadius: 10, background: t.surfaceVar, flexShrink: 0 }} />
      )}
      <div onClick={onOpen} style={{ flex: 1, minWidth: 0, cursor: 'pointer' }}>
        <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.title || 'Untitled'}</div>
        <div style={{ fontSize: 12, color: t.muted, marginTop: 2 }}>
          {item.plat || '—'}
          {stale && <span style={{ color: '#F0A93B' }}> · {ageDays(item)}d, reprice?</span>}
        </div>
      </div>
      <div style={{ textAlign: 'right', minWidth: 78 }}>
        <div style={{ fontSize: 15, fontWeight: 800 }}>
          {item.sell > 0 ? money(item.sell) : item.buy > 0 ? money(item.buy) : '—'}
        </div>
        <div style={{ fontSize: 11, color: t.muted }}>
          {item.sell > 0 ? (item.status === 'SOLD' ? 'sold' : 'target') : item.buy > 0 ? buyLabel : 'no price'}
          {item.sell > 0 && item.buy > 0 && ` · ${buyLabel} ${money(item.buy)}`}
        </div>
        {item.sell > 0 && (
          <div style={{ fontSize: 11.5, fontWeight: 700, color: p < 0 ? t.loss : t.emerald }}>
            {p >= 0 ? '+' : ''}{money(Math.round(p * 100) / 100)} · {Math.round(roi(item))}%
          </div>
        )}
      </div>
      <StatusPill
        status={item.status}
        onClick={nextStatus(item.status) ? onAdvance : undefined}
        title={nextStatus(item.status) ? `Tap to move to ${nextStatus(item.status)}` : undefined}
      />
      <div style={{ display: 'flex', gap: 4, opacity: hover ? 1 : 0.25, transition: 'opacity 0.15s' }}>
        {item.title && (
          <button onClick={() => openUrl(searchUrl('EBAY_SOLD', item.title))} title="eBay sold prices" style={{ ...btn(t), border: 'none', padding: 6 }}><TrendingUp size={14} /></button>
        )}
        {item.link && (
          <button onClick={() => openUrl(item.link)} title="Open listing" style={{ ...btn(t), border: 'none', padding: 6 }}><ExternalLink size={14} /></button>
        )}
      </div>
    </div>
  );
}

