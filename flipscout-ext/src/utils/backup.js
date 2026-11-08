// Parse the app's backup format ({app, version, items, sellers}) and merge by id,
// keeping existing rows. Returns null when the text isn't a FlipScout backup.
export function importBackup(text, { items, sellers }) {
  let root;
  try {
    root = JSON.parse(text.trim());
  } catch (e) {
    return null;
  }
  if (!root || !Array.isArray(root.items || [])) return null;
  const incomingItems = (root.items || []).filter((i) => i && i.id);
  const incomingSellers = (root.sellers || []).filter((s) => s && s.id);
  if (!incomingItems.length && !incomingSellers.length) return null;

  const haveItems = new Set(items.map((i) => i.id));
  const haveSellers = new Set(sellers.map((s) => s.id));
  const newItems = incomingItems.filter((i) => !haveItems.has(i.id));
  const newSellers = incomingSellers.filter((s) => !haveSellers.has(s.id));
  return {
    items: [...newItems, ...items],
    sellers: [...newSellers, ...sellers],
    addedItems: newItems.length,
  };
}
