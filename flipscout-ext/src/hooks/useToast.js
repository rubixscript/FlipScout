import { useCallback, useRef, useState } from 'react';

// One-line snackbar with optional Undo (preferred over confirm dialogs).
export default function useToast(ms = 5000) {
  const [toast, setToast] = useState(null);
  const timer = useRef(null);

  const show = useCallback(
    (message, undo) => {
      clearTimeout(timer.current);
      setToast({ message, undo });
      timer.current = setTimeout(() => setToast(null), ms);
    },
    [ms]
  );
  const dismiss = useCallback(() => {
    clearTimeout(timer.current);
    setToast(null);
  }, []);

  return { toast, show, dismiss };
}
