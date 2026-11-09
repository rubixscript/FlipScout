import React, { useMemo, useState } from 'react';
import { ArrowLeft, Search as SearchIcon, PackageSearch, Tag } from 'lucide-react';
import { money } from '../styles';
import { STATUSES, guessCategory, isStale, newItem, nextStatus, portfolioStats, queryItems } from '../utils/logic';
import { Chip, ConfirmDelete, Empty, TileDelete, btn, input } from './ui';
import ItemEditor from './items/ItemEditor';
import ItemRow from './items/ItemRow';
import StatTile from './items/StatTile';

const SORTS = ['NEWEST', 'OLDEST', 'PROFIT', 'ROI', 'PRICE'];
const UNLISTED = 'Unlisted'; // items with no category

export default function Items({ t, items, sellers, searches = [], setItems, toast }) {
  const [text, setText] = useState('');
  const [status, setStatus] = useState(null);
  const [openCat, setOpenCat] = useState(null); // null = the category grid on home
  const [sort, setSort] = useState('NEWEST');
  const [editing, setEditing] = useState(null);
  const stats = useMemo(() => portfolioStats(items), [items]);
  const shown = useMemo(() => queryItems(items, { text, status, sort }), [items, text, status, sort]);
  // One card per category (A-Z), uncategorised items last under "Unlisted"; items inside keep the chosen sort.
  const groups = useMemo(() => {
    const by = {};
    shown.forEach((i) => { (by[i.cat || UNLISTED] = by[i.cat || UNLISTED] || []).push(i); });
    return Object.keys(by)
      .sort((a, b) => (a === UNLISTED) - (b === UNLISTED) || a.localeCompare(b))
      .map((name) => ({ name, items: by[name] }));
  }, [shown]);
  // Searching skips the grid and lists matches directly, so nothing hides behind a click.
  const searching = text.trim() !== '';
  const current = groups.find((g) => g.name === openCat); // falls back to the grid if a filter empties the category
  // Unlisted items whose titles match a saved keyword can be filed in one tap (with Undo).
  const guesses = useMemo(() => {
    const m = new Map();
    items.forEach((i) => { if (!i.cat) { const c = guessCategory(i.title, searches); if (c) m.set(i.id, c); } });
    return m;
  }, [items, searches]);
  const autoSort = () => {
    const before = items;
    setItems(items.map((i) => (guesses.has(i.id) ? { ...i, cat: guesses.get(i.id) } : i)));
    toast(`Filed ${guesses.size} item${guesses.size === 1 ? '' : 's'} by keyword`, () => setItems(before));
  };
  // Deleting a category never deletes items: they move to Unlisted. Deleting Unlisted itself
  // clears the items inside. Undo restores both.
  const deleteCategory = (name) => {
    const before = items;
    if (name === UNLISTED) {
      const n = before.filter((i) => !i.cat).length;
      setItems(before.filter((i) => i.cat));
      toast(`Deleted ${n} unlisted item${n === 1 ? '' : 's'}`, () => setItems(before));
    } else {
      setItems(before.map((i) => (i.cat === name ? { ...i, cat: '' } : i)));
      toast(`Deleted “${name}”, items moved to Unlisted`, () => setItems(before));
    }
    setOpenCat(null);
  };
  const counts = (s) => items.filter((i) => i.status === s).length;
  const unlistedCount = items.filter((i) => !i.cat).length;

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
      ) : searching ? (
        shown.map((item) => (
          <ItemRow key={item.id} t={t} item={item} onOpen={() => setEditing(item)} onAdvance={() => advance(item)} />
        ))
      ) : current ? (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
            <button onClick={() => setOpenCat(null)} style={{ ...btn(t), border: 'none', padding: '4px 2px', fontWeight: 800, fontSize: 15, flex: 1, justifyContent: 'flex-start' }}>
              <ArrowLeft size={16} /> {current.name} <span style={{ color: t.muted, fontWeight: 600, fontSize: 12.5 }}>{current.items.length}</span>
            </button>
            <ConfirmDelete t={t} label={current.name === UNLISTED ? 'Delete unlisted items' : 'Delete category'} onConfirm={() => deleteCategory(current.name)} />
          </div>
          {current.name === UNLISTED && guesses.size > 0 && (
            <button style={btn(t, { style: { margin: '2px 0 8px' } })} onClick={autoSort}>
              Sort {guesses.size} by keyword
            </button>
          )}
          {current.items.map((item) => (
            <ItemRow key={item.id} t={t} item={item} onOpen={() => setEditing(item)} onAdvance={() => advance(item)} />
          ))}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
          {groups.map((g) => (
            <CategoryCard
              key={g.name}
              t={t}
              group={g}
              onOpen={() => setOpenCat(g.name)}
              onDelete={() => deleteCategory(g.name)}
              deleteHint={g.name === UNLISTED ? `Deletes all ${unlistedCount} item${unlistedCount === 1 ? '' : 's'}` : 'Items move to Unlisted'}
            />
          ))}
        </div>
      )}

      {editing && (
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
      )}
    </div>
  );
}


// Square tile: the first item's photo fills it, with the category name, count and total over a dark fade.
// deleteHint says what deleting means for this card (items move vs. items deleted).
function CategoryCard({ t, group, onOpen, onDelete, deleteHint = 'Items move to Unlisted' }) {
  const [failed, setFailed] = useState(false);
  const cover = group.items.find((i) => i.photoUrl);
  const stale = group.items.filter((i) => isStale(i)).length;
  const worth = group.items.reduce((sum, i) => sum + (i.sell > 0 ? i.sell : i.buy || 0), 0);
  return (
    <div
      onClick={onOpen}
      style={{ position: 'relative', aspectRatio: '1 / 1', borderRadius: 18, overflow: 'hidden', cursor: 'pointer', background: t.surfaceVar, border: `1px solid ${t.outlineVar}` }}
    >
      {cover && !failed ? (
        <img src={cover.photoUrl} alt="" onError={() => setFailed(true)} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
      ) : (
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', color: t.muted }}>
          <Tag size={30} strokeWidth={1.5} />
        </div>
      )}
      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(0,0,0,0.75) 0%, rgba(0,0,0,0) 60%)' }} />
      {stale > 0 && (
        <div title="Bought or listed 14+ days ago" style={{ position: 'absolute', top: 8, left: 8, background: '#F0A93B', color: '#1a1a1a', borderRadius: 999, padding: '2px 9px', fontSize: 11.5, fontWeight: 800 }}>{stale} stale</div>
      )}
      <div style={{ position: 'absolute', top: 8, right: 8, background: 'rgba(0,0,0,0.6)', color: '#fff', borderRadius: 999, padding: '2px 9px', fontSize: 12, fontWeight: 700 }}>{group.items.length}</div>
      <div style={{ position: 'absolute', left: 12, right: 12, bottom: 10, color: '#fff' }}>
        <div style={{ fontWeight: 800, fontSize: 14.5, lineHeight: 1.2, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{group.name}</div>
        <div style={{ fontSize: 12, opacity: 0.85, marginTop: 2 }}>{money(Math.round(worth))}</div>
      </div>
      {onDelete && <TileDelete t={t} title={`Delete “${group.name}”?`} hint={deleteHint} onConfirm={onDelete} />}
    </div>
  );
}
