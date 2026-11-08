import React, { useMemo } from 'react';
import { BarChart3 } from 'lucide-react';
import { money } from '../styles';
import { monthlyProfit, profitBy } from '../utils/logic';
import { Card, Empty } from './ui';

export default function Insights({ t, items }) {
  const months = useMemo(() => monthlyProfit(items, 6), [items]);
  const byCat = useMemo(() => profitBy(items, (i) => i.cat, 'Uncategorised'), [items]);
  const byPlat = useMemo(() => profitBy(items, (i) => i.plat, 'No platform'), [items]);
  if (!items.some((i) => i.status === 'SOLD')) {
    return <Empty t={t} icon={<BarChart3 size={36} strokeWidth={1.5} />} title="Nothing sold yet" hint="Move a flip to SOLD (tap › on its row) and your profit charts appear here." />;
  }
  const max = Math.max(1, ...months.map((m) => Math.abs(m.profit)));
  return (
    <div>
      <Card t={t} style={{ marginBottom: 12 }}>
        <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 12 }}>Monthly profit</div>
        <div style={{ display: 'flex', alignItems: 'stretch', gap: 10, height: 150 }}>
          {months.map((m) => {
            const h = (Math.abs(m.profit) / max) * 60;
            return (
              <div key={m.label} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <div style={{ fontSize: 11, color: t.muted, height: 16 }}>{m.profit ? money(Math.round(m.profit)) : ''}</div>
                <div style={{ flex: 1, width: '100%', display: 'flex', flexDirection: 'column' }}>
                  <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end' }}>
                    {m.profit > 0 && <div style={{ width: '100%', height: `${h / 0.6}%`, background: t.emerald, borderRadius: 6 }} />}
                  </div>
                  <div style={{ flex: 0.4, display: 'flex', alignItems: 'flex-start' }}>
                    {m.profit < 0 && <div style={{ width: '100%', height: `${h / 0.2}%`, maxHeight: '100%', background: t.loss, borderRadius: 6 }} />}
                  </div>
                </div>
                <div style={{ fontSize: 11.5, color: t.muted, marginTop: 4 }}>{m.label}</div>
              </div>
            );
          })}
        </div>
      </Card>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <Breakdown t={t} title="Profit by category" rows={byCat} />
        <Breakdown t={t} title="Profit by platform" rows={byPlat} />
      </div>
    </div>
  );
}

function Breakdown({ t, title, rows }) {
  const max = Math.max(1, ...rows.map((r) => Math.abs(r.profit)));
  return (
    <Card t={t}>
      <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 10 }}>{title}</div>
      {rows.map((r) => (
        <div key={r.label} style={{ marginBottom: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.label} <span style={{ color: t.muted }}>· {r.count}</span></span>
            <b style={{ color: r.profit < 0 ? t.loss : t.emerald }}>{money(Math.round(r.profit * 100) / 100)}</b>
          </div>
          <div style={{ height: 5, borderRadius: 3, background: t.surfaceVar, marginTop: 4 }}>
            <div style={{ height: 5, borderRadius: 3, width: `${(Math.abs(r.profit) / max) * 100}%`, background: r.profit < 0 ? t.loss : t.emerald }} />
          </div>
        </div>
      ))}
    </Card>
  );
}
