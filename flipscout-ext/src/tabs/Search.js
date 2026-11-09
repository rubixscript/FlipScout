import React, { useState } from 'react';
import { ArrowLeft, ClipboardPaste, MapPin, MoreHorizontal, Pin, PinOff, Search as SearchIcon, Sparkles, Tag, X } from 'lucide-react';
import { KEYWORD_PACKS, keywordsPrompt, parseKeywords, parseMarketplaceLocation, searchUrl } from '../utils/logic';
import { Card, ConfirmDelete, TileDelete, btn, copy, input, openUrl } from './ui';

const TARGETS = [['EBAY', 'eBay'], ['EBAY_SOLD', 'Sold'], ['MARKETPLACE', 'Marketplace']];
const RECENT = 'Recent'; // unpinned searches get their own tile at the end of the grid

export default function SearchTab({ t, searches, setSearches, settings = {}, setSettings, toast }) {
  const loc = settings.mpLocation || '';
  const [q, setQ] = useState('');
  const [openCat, setOpenCat] = useState(null); // null = the category tile grid
  const [topic, setTopic] = useState('');
  const [showTools, setShowTools] = useState(false);

  // Remember searches (recent = unpinned, last 30, deduped) and fire the engine in one step.
  const go = (target, query) => {
    const text = query.trim();
    if (!text) return;
    const existing = searches.find((s) => s.q.toLowerCase() === text.toLowerCase());
    if (!existing) {
      const recent = searches.filter((s) => !s.pin).slice(0, 29);
      setSearches([{ id: crypto.randomUUID(), q: text, pin: false, cat: '', at: Date.now() }, ...searches.filter((s) => s.pin), ...recent]);
    }
    openUrl(searchUrl(target, text, { loc }));
  };
  const toggle = (s) => setSearches(searches.map((x) => (x.id === s.id ? { ...x, pin: !x.pin } : x)));
  const del = (s) => { setSearches(searches.filter((x) => x.id !== s.id)); toast('Keyword removed', () => setSearches(searches)); };

  const addKeywords = (list) => {
    const have = new Set(searches.map((s) => s.q.toLowerCase()));
    const fresh = [];
    list.forEach((k) => { if (!have.has(k.q.toLowerCase())) { have.add(k.q.toLowerCase()); fresh.push({ id: crypto.randomUUID(), q: k.q, pin: true, cat: k.cat, at: Date.now() }); } });
    // Pin existing matches into the category too.
    const pinned = searches.map((s) => {
      const hit = list.find((k) => k.q.toLowerCase() === s.q.toLowerCase());
      return hit ? { ...s, pin: true, cat: s.cat || hit.cat } : s;
    });
    setSearches([...fresh, ...pinned]);
    toast(`Added ${fresh.length} keyword${fresh.length === 1 ? '' : 's'}`);
  };
  const importClipboard = async () => {
    try {
      const list = parseKeywords(await navigator.clipboard.readText());
      if (!list.length) return toast('No keywords found on the clipboard — copy the AI reply first');
      addKeywords(list);
    } catch (e) { toast('Could not read the clipboard'); }
  };

  // Paste any Marketplace link (or an id/city slug) once; every Marketplace search then uses it.
  const saveLocation = (text) => {
    if (!text.trim()) { setSettings({ ...settings, mpLocation: '' }); return toast('Location cleared'); }
    const next = parseMarketplaceLocation(text);
    if (!next) return toast('Paste a Marketplace link like facebook.com/marketplace/109493702409366/search');
    setSettings({ ...settings, mpLocation: next });
    toast('Marketplace searches now use that location');
  };

  // Removes the category's pinned keywords. Undo puts them back.
  const delCategory = (cat) => {
    const before = searches;
    const n = searches.filter((s) => s.pin && (s.cat || 'Uncategorised') === cat).length;
    setSearches(searches.filter((s) => !(s.pin && (s.cat || 'Uncategorised') === cat)));
    setOpenCat(null);
    toast(`Deleted “${cat}” and ${n} keyword${n === 1 ? '' : 's'}`, () => setSearches(before));
  };
  // Clears the unpinned recent searches (the library is untouched). Undo puts them back.
  const clearRecent = () => {
    const before = searches;
    setSearches(searches.filter((s) => s.pin));
    toast('Cleared recent searches', () => setSearches(before));
  };

  const recent = searches.filter((s) => !s.pin);
  const groups = {};
  searches.filter((s) => s.pin).forEach((s) => { (groups[s.cat || 'Uncategorised'] = groups[s.cat || 'Uncategorised'] || []).push(s); });
  const cats = Object.keys(groups).sort().map((name) => ({ name, list: groups[name] }));
  // Falls back to the grid if the open category was emptied (e.g. last keyword deleted).
  const current = openCat === RECENT ? { name: RECENT, list: recent } : cats.find((c) => c.name === openCat);

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <SearchIcon size={15} color={t.muted} style={{ position: 'absolute', left: 12, top: 13 }} />
          <input value={q} onChange={(e) => setQ(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && go('EBAY', q)} placeholder="What are you hunting for?" style={input(t, { paddingLeft: 34 })} />
        </div>
        {TARGETS.map(([k, label]) => (
          <button key={k} style={btn(t, { primary: k === 'EBAY' })} disabled={!q.trim()} onClick={() => go(k, q)}>{label}</button>
        ))}
      </div>

      <LocationField key={loc} t={t} loc={loc} onSave={saveLocation} />

      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <button style={btn(t)} onClick={() => setShowTools(!showTools)}><Sparkles size={14} /> Add keywords</button>
        <button style={btn(t)} onClick={importClipboard}><ClipboardPaste size={14} /> Import AI reply from clipboard</button>
      </div>

      {showTools && (
        <Card t={t} style={{ marginBottom: 12 }}>
          <div style={{ fontWeight: 700, fontSize: 13.5, marginBottom: 8 }}>Starter packs — one tap adds the whole pack</div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
            {KEYWORD_PACKS.map((p) => (
              <button key={p.name} style={btn(t, { style: { padding: '5px 12px', fontSize: 12.5, borderRadius: 999 } })} onClick={() => addKeywords(p.keywords.map((q2) => ({ q: q2, cat: p.name })))}>
                + {p.name}
              </button>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input value={topic} onChange={(e) => setTopic(e.target.value)} placeholder="Topic for AI keyword mining, e.g. vintage cameras" style={input(t, { flex: 1 })} />
            <button style={btn(t, { primary: true })} onClick={() => { copy(keywordsPrompt(topic)); toast('Prompt copied — paste in ChatGPT, then copy its reply and tap Import'); openUrl('https://chatgpt.com/'); }}>Copy prompt + open ChatGPT</button>
          </div>
        </Card>
      )}

      {current ? (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
            <button onClick={() => setOpenCat(null)} style={{ ...btn(t), border: 'none', padding: '4px 2px', fontWeight: 800, fontSize: 15, flex: 1, justifyContent: 'flex-start' }}>
              <ArrowLeft size={16} /> {current.name} <span style={{ color: t.muted, fontWeight: 600, fontSize: 12.5 }}>{current.list.length}</span>
            </button>
            {current.name === RECENT
              ? current.list.length > 0 && <ConfirmDelete t={t} label="Clear recent searches" onConfirm={clearRecent} />
              : <ConfirmDelete t={t} label="Delete category and its keywords" onConfirm={() => delCategory(current.name)} />}
          </div>
          {current.list.length === 0 ? (
            <div style={{ textAlign: 'center', color: t.muted, fontSize: 13, padding: 20 }}>Nothing here — searches you run land in Recent.</div>
          ) : (
            current.list.map((s) => <KeywordRow key={s.id} t={t} s={s} go={go} toggle={toggle} del={del} />)
          )}
        </div>
      ) : cats.length + (recent.length > 0 ? 1 : 0) === 0 ? (
        <div style={{ textAlign: 'center', color: t.muted, fontSize: 13, padding: 30 }}>Type a search above, or tap “Add keywords” to start with a pack.</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 12 }}>
          {cats.map((c) => (
            <KeywordTile key={c.name} t={t} name={c.name} list={c.list} onOpen={() => setOpenCat(c.name)} onDelete={() => delCategory(c.name)} />
          ))}
          {recent.length > 0 && (
            <KeywordTile t={t} name={RECENT} list={recent} onOpen={() => setOpenCat(RECENT)} onDelete={clearRecent} />
          )}
        </div>
      )}
    </div>
  );
}

function LocationField({ t, loc, onSave }) {
  const [text, setText] = useState(loc);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
      <MapPin size={15} color={loc ? t.emerald : t.muted} />
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        onBlur={() => text !== loc && onSave(text)}
        onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.blur()}
        placeholder="Marketplace location: paste any Marketplace link (optional)"
        spellCheck={false}
        style={input(t, { padding: '7px 10px', fontSize: 12.5 })}
      />
    </div>
  );
}

// Marketplace is the one-tap default; eBay and Sold sit behind the "more" toggle.
function KeywordRow({ t, s, go, toggle, del }) {
  const [more, setMore] = useState(false);
  const chip = { padding: '4px 10px', fontSize: 12, borderRadius: 999 };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 2px', borderBottom: `1px solid ${t.outlineVar}` }}>
      <span onClick={() => go('MARKETPLACE', s.q)} title="Search Marketplace" style={{ flex: 1, fontSize: 13.5, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', cursor: 'pointer' }}>{s.q}</span>
      {more && TARGETS.filter(([k]) => k !== 'MARKETPLACE').map(([k, label]) => (
        <button key={k} onClick={() => go(k, s.q)} style={btn(t, { style: chip })}>{label}</button>
      ))}
      <button onClick={() => go('MARKETPLACE', s.q)} style={btn(t, { primary: true, style: chip })}>Marketplace</button>
      <button onClick={() => setMore(!more)} title="eBay / Sold" style={{ ...btn(t), border: 'none', padding: 6 }}><MoreHorizontal size={14} /></button>
      <button onClick={() => toggle(s)} title={s.pin ? 'Unpin' : 'Pin to library'} style={{ ...btn(t), border: 'none', padding: 6 }}>{s.pin ? <PinOff size={14} /> : <Pin size={14} />}</button>
      <button onClick={() => del(s)} title="Remove" style={{ ...btn(t), border: 'none', padding: 6 }}><X size={14} /></button>
    </div>
  );
}

// Square tile like the Items tab: category name, count badge and a preview of the first few
// keywords; tap opens the keyword list. Trash chip deletes (with confirm) without opening.
function KeywordTile({ t, name, list, onOpen, onDelete }) {
  const hint = name === RECENT
    ? `Clears ${list.length} recent search${list.length === 1 ? '' : 'es'}`
    : `Deletes ${list.length} keyword${list.length === 1 ? '' : 's'}`;
  return (
    <div
      onClick={onOpen}
      style={{ position: 'relative', aspectRatio: '1 / 1', borderRadius: 18, overflow: 'hidden', cursor: 'pointer', background: t.surfaceVar, border: `1px solid ${t.outlineVar}`, padding: '12px 12px 10px' }}
    >
      <div style={{ position: 'absolute', top: 8, right: 8, background: 'rgba(0,0,0,0.6)', color: '#fff', borderRadius: 999, padding: '2px 9px', fontSize: 12, fontWeight: 700 }}>{list.length}</div>
      <div style={{ fontWeight: 800, fontSize: 14.5, lineHeight: 1.25, paddingRight: 28, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{name}</div>
      <div style={{ marginTop: 6, fontSize: 11.5, color: t.muted, paddingRight: 26 }}>
        {list.slice(0, 3).map((s) => (
          <div key={s.id} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', lineHeight: 1.5 }}>{s.q}</div>
        ))}
      </div>
      <Tag size={26} strokeWidth={1.5} style={{ position: 'absolute', right: 10, bottom: 10, color: t.outline }} />
      {onDelete && <TileDelete t={t} title={`Delete “${name}”?`} hint={hint} onConfirm={onDelete} />}
    </div>
  );
}
