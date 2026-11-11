const fmt = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });
export default function formatCurrency(value) {
  if (value === null || value === undefined || value === "") return "-";
  return fmt.format(Number(value));
}
