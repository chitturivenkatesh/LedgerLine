import MoneyForm from "../components/MoneyForm";
import { withdraw } from "../services/transactionService";

// Balance sufficiency is decided by the backend inside the locked database transaction, not here.
export default function Withdraw() { return <MoneyForm title="Withdraw" verb="withdraw" action={withdraw} />; }
