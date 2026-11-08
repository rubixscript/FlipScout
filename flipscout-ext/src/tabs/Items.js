import React, { useMemo, useState } from 'react';
import { Search as SearchIcon, PackageSearch } from 'lucide-react';
import { money } from '../styles';
import { STATUSES, newItem, nextStatus, portfolioStats, queryItems } from '../utils/logic';
import { Chip, Empty, btn, input } from './ui';
import ItemEditor from './items/ItemEditor';
import ItemRow from './items/ItemRow';
import StatTile from './items/StatTile';

const SORTS = ['NEWEST', 'OLDEST', 'PROFIT', 'ROI', 'PRICE'];

export default function Items({ t, items, sellers, setItems, toast }) {
  const [text, setText] = useState('');
  const [status, setStatus] = useState(null);
  const [sort, setSort] = useState('NEWEST');
  const [editing, setEditing] = useState(null);
  const stats = useMemo(() => portfolioStats(items), [items]);
  const shown = useMemo(() => queryItems(items, { text, status, sort }), [items, text, status, sort]);
  const counts = (s) => items.filter((i) => i.status === s).length;

  const update = (item) => setItems(items.map((i) => (i.id === item.id ? item : i)));
  const advance = (item) => {
    const n = nextStatus(item.status);
    if (!n) return;
    update({ ...item, status: n, soldAt: n === 'SOLD' ? Date.now() : item.soldAt });
    toast(`Moved to ${n}`, () => update(item));
  };
  const remove = (item) => {
    setItems(items.filter((i) => i.id !== item.id));
    setEditing(null);
    toast('Item deleted', () => setItems([item, ...items.filter((i) => i.id !== item.id)]));
  };

  if (editing) {
    return (
      <ItemEditor
        t={t}
        item={editing}
        sellers={sellers}
        categories={[...new Set(items.map((i) => i.cat).filter(Boolean))].sort()}
        onChange={(i) => { update(i); setEditing(i); }}
        onClose={() => setEditing(null)}
        onDelete={() => remove(editing)}
        toast={toast}
      />
    );
  }

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 8, marginBottom: 12 }}>
        <StatTile t={t} label="Profit" value={money(Math.round(stats.profit * 100) / 100)} color={stats.profit < 0 ? t.loss : t.emerald} />
        <StatTile t={t} label="In stock" value={money(Math.round(stats.inStock))} />
        <StatTile t={t} label="Sold" value={stats.soldCount} />
        <StatTile t={t} label="Avg ROI" value={Math.round(stats.avgRoi) + '%'} />
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <SearchIcon size={15} color={t.muted} style={{ position: 'absolute', left: 12, top: 13 }} />
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Search title, notes, tags" style={input(t, { paddingLeft: 34 })} />
        </div>
        <select value={sort} onChange={(e) => setSort(e.target.value)} style={input(t, { width: 110 })}>
          {SORTS.map((s) => <option key={s} value={s}>{s[0] + s.slice(1).toLowerCase()}</option>)}
        </select>
        <button style={btn(t, { primary: true })} onClick={() => setEditing(newItem())} title="Add item manually">+ Add</button>
      </div>

      <div style={{ display: 'flex', gap: 6, marginBottom: 10, flexWrap: 'wrap' }}>
        <Chip t={t} active={!status} onClick={() => setStatus(null)}>All {items.length}</Chip>
        {STATUSES.map((s) => (
          <Chip key={s} t={t} active={status === s} onClick={() => setStatus(status === s ? null : s)}>
            {s[0] + s.slice(1).toLowerCase()} {counts(s)}
          </Chip>
        ))}
      </div>

      {shown.length === 0 ? (
        <Empty t={t} icon={<PackageSearch size={36} strokeWidth={1.5} />}
          title={items.length ? 'Nothing matches' : 'No flips yet'}
          hint={items.length ? 'Clear the search or filter.' : 'Open an eBay or Marketplace listing and click “Track this flip”, or tap + Add.'} />
      ) : (
        shown.map((item) => (
          <ItemRow key={item.id} t={t} item={item} onOpen={() => setEditing(item)} onAdvance={() => advance(item)} />
        ))
      )}
    </div>
  );
}

