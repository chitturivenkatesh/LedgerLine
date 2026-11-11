import MoneyForm from "../components/MoneyForm";
import { deposit } from "../services/transactionService";

export default function Deposit() { return <MoneyForm title="Deposit" verb="deposit" action={deposit} />; }
