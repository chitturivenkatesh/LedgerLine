export default function Loading({ text = "Loading..." }) {
  return (
    <div className="text-center p-5 text-muted">
      <div className="spinner-border spinner-border-sm me-2" role="status" />{text}
    </div>
  );
}
