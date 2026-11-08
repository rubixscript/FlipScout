import React, { useState } from 'react';
import { ChevronDown, ChevronRight, ClipboardPaste, Pin, PinOff, Search as SearchIcon, Sparkles, X } from 'lucide-react';
import { KEYWORD_PACKS, keywordsPrompt, parseKeywords, searchUrl } from '../utils/logic';
import { Card, btn, copy, input, openUrl } from './ui';

const TARGETS = [['EBAY', 'eBay'], ['EBAY_SOLD', 'Sold'], ['MARKETPLACE', 'Marketplace']];

export default function SearchTab({ t, searches, setSearches, toast }) {
  const [q, setQ] = useState('');
  const [collapsed, setCollapsed] = useState(() => new Set(['*']));
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
    openUrl(searchUrl(target, text));
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

  const pinned = searches.filter((s) => s.pin);
  const recent = searches.filter((s) => !s.pin);
  const groups = {};
  pinned.forEach((s) => { (groups[s.cat || 'Uncategorised'] = groups[s.cat || 'Uncategorised'] || []).push(s); });

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

      {Object.keys(groups).sort().map((cat) => {
        const open = !collapsed.has(cat);
        return (
          <div key={cat} style={{ marginBottom: 6 }}>
            <button onClick={() => { const n = new Set(collapsed); open ? n.add(cat) : n.delete(cat); setCollapsed(n); }} style={{ ...btn(t), width: '100%', justifyContent: 'flex-start', border: 'none', padding: '8px 2px', color: t.muted, fontWeight: 700 }}>
              {open ? <ChevronDown size={15} /> : <ChevronRight size={15} />} {cat.toUpperCase()} <span style={{ fontWeight: 500 }}>{groups[cat].length}</span>
            </button>
            {open && groups[cat].map((s) => <KeywordRow key={s.id} t={t} s={s} go={go} toggle={toggle} del={del} />)}
          </div>
        );
      })}

      {recent.length > 0 && (
        <div style={{ marginTop: 10 }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: t.muted, margin: '0 2px 6px' }}>RECENT {recent.length}</div>
          {recent.slice(0, 8).map((s) => <KeywordRow key={s.id} t={t} s={s} go={go} toggle={toggle} del={del} />)}
        </div>
      )}
      {searches.length === 0 && !showTools && (
        <div style={{ textAlign: 'center', color: t.muted, fontSize: 13, padding: 30 }}>Type a search above, or tap “Add keywords” to start with a pack.</div>
      )}
    </div>
  );
}

function KeywordRow({ t, s, go, toggle, del }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 2px', borderBottom: `1px solid ${t.outlineVar}` }}>
      <span style={{ flex: 1, fontSize: 13.5, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.q}</span>
      {TARGETS.map(([k, label]) => (
        <button key={k} onClick={() => go(k, s.q)} style={btn(t, { style: { padding: '4px 10px', fontSize: 12, borderRadius: 999 } })}>{label}</button>
      ))}
      <button onClick={() => toggle(s)} title={s.pin ? 'Unpin' : 'Pin to library'} style={{ ...btn(t), border: 'none', padding: 6 }}>{s.pin ? <PinOff size={14} /> : <Pin size={14} />}</button>
      <button onClick={() => del(s)} title="Remove" style={{ ...btn(t), border: 'none', padding: 6 }}><X size={14} /></button>
    </div>
  );
}
