export default function ConfirmModal({ show, title, children, confirmText = "Confirm", busy, onConfirm, onCancel }) {
  if (!show) return null;
  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header"><h5 className="modal-title">{title}</h5></div>
            <div className="modal-body">{children}</div>
            <div className="modal-footer">
              <button className="btn btn-outline-secondary" onClick={onCancel} disabled={busy}>Cancel</button>
              <button className="btn btn-primary" onClick={onConfirm} disabled={busy}>{busy ? "Processing..." : confirmText}</button>
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show" />
    </>
  );
}
