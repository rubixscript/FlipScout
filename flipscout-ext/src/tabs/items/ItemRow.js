import React from 'react';
import { ChevronRight, ExternalLink } from 'lucide-react';
import { STATUS_COLOR, money } from '../../styles';
import { ageDays, isStale, nextStatus, profit, roi } from '../../utils/logic';
import { StatusPill, btn, openUrl } from '../ui';

export default function ItemRow({ t, item, onOpen, onAdvance }) {
  const p = profit(item);
  const stale = isStale(item);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 4px', borderBottom: `1px solid ${t.outlineVar}` }}>
      {item.photoUrl ? (
        <img src={item.photoUrl} alt="" style={{ width: 44, height: 44, borderRadius: 10, objectFit: 'cover' }} />
      ) : (
        <div style={{ width: 44, height: 44, borderRadius: 10, background: t.surfaceVar, flexShrink: 0 }} />
      )}
      <div onClick={onOpen} style={{ flex: 1, minWidth: 0, cursor: 'pointer' }}>
        <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.title || 'Untitled'}</div>
        <div style={{ fontSize: 12, color: t.muted, marginTop: 2 }}>
          {item.plat || '—'} · bought {item.buy > 0 ? money(item.buy) : '—'}
          {stale && <span style={{ color: '#F0A93B' }}> · {ageDays(item)}d, reprice?</span>}
        </div>
      </div>
      {item.sell > 0 && (
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 13.5, fontWeight: 800, color: p < 0 ? t.loss : t.emerald }}>{p >= 0 ? '+' : ''}{money(Math.round(p * 100) / 100)}</div>
          <div style={{ fontSize: 11, color: t.muted }}>{Math.round(roi(item))}% ROI</div>
        </div>
      )}
      <StatusPill status={item.status} />
      {nextStatus(item.status) ? (
        <button onClick={onAdvance} title={`Move to ${nextStatus(item.status)}`} style={{ ...btn(t), padding: '6px 8px', color: STATUS_COLOR[nextStatus(item.status)] }}>
          <ChevronRight size={16} />
        </button>
      ) : <span style={{ width: 34 }} />}
      {item.link && (
        <button onClick={() => openUrl(item.link)} title="Open listing" style={{ ...btn(t), padding: '6px 8px' }}><ExternalLink size={14} /></button>
      )}
    </div>
  );
}

