import { useEffect, useMemo, useState } from 'react';
import { makeTheme } from '../styles';

const QUERY = '(prefers-color-scheme: dark)';

// Follows the OS colour scheme, like the app.
export default function useTheme() {
  const [dark, setDark] = useState(() => !!window.matchMedia && window.matchMedia(QUERY).matches);
  useEffect(() => {
    const mq = window.matchMedia(QUERY);
    const onChange = (e) => setDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);
  return useMemo(() => makeTheme(dark), [dark]);
}
