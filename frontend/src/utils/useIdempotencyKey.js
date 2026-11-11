import { useRef } from "react";

/**
 * One key per LOGICAL request.
 *  - Same payload fingerprint -> same key (a retry after a timeout or network error reuses it, so the server can de-duplicate).
 *  - Different payload        -> new key.
 *  - After a confirmed success -> call reset() so the next intentional payment gets a fresh key.
 */
export default function useIdempotencyKey() {
  const ref = useRef({ fingerprint: null, key: null });
  return {
    keyFor(fingerprint) {
      if (ref.current.fingerprint !== fingerprint) {
        ref.current = { fingerprint, key: crypto.randomUUID() };
      }
      return ref.current.key;
    },
    reset() { ref.current = { fingerprint: null, key: null }; },
  };
}
