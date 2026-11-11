import { createContext, useCallback, useContext, useState } from "react";

const NotifyContext = createContext(null);
export const useNotify = () => useContext(NotifyContext);

export function NotifyProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const push = useCallback((type, message) => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, type, message }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 5000);
  }, []);

  const notify = { success: (m) => push("success", m), error: (m) => push("danger", m) };

  return (
    <NotifyContext.Provider value={notify}>
      {children}
      <div className="toast-container position-fixed top-0 end-0 p-3" style={{ zIndex: 2000 }}>
        {toasts.map((t) => (
          <div key={t.id} className={`toast show align-items-center text-bg-${t.type} border-0 mb-2`} role="alert">
            <div className="d-flex"><div className="toast-body">{t.message}</div></div>
          </div>
        ))}
      </div>
    </NotifyContext.Provider>
  );
}
