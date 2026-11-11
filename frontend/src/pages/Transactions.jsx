import * as transactionService from "../services/transactionService";
import useFetch from "../utils/useFetch";
import Loading from "../components/Loading";
import ErrorMessage from "../components/ErrorMessage";
import TransactionTable from "../components/TransactionTable";

export default function Transactions() {
  const { data, loading, error, reload } = useFetch(transactionService.list);
  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={reload} />;
  return (<><h4 className="mb-3">Transaction History</h4><TransactionTable transactions={data} /></>);
}
