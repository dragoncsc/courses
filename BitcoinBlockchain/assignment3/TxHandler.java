import java.util.*;

public class TxHandler {
    private UTXOPool pLedger;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.pLedger = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool(){
        return this.pLedger;
    }
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        
        ArrayList<UTXO> utxo = this.pLedger.getAllUTXO();
        HashSet<UTXO> seenUTXO = new HashSet<UTXO>();
        double outputSum = 0;
        double inputSum = 0;

        int n = inputs.size();
        for (int i=0; i < n; i++){
            UTXO curTrans = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);

            // If the current set of unspent transactions doesn't contain the origin of
            // the input transaction, OR the input transaction has already been spent
            // return false
            if (!this.pLedger.contains(curTrans) || seenUTXO.contains(curTrans))
                return false;
            
            // if the input signature does not match the input data (in index) and the intended address
            // reject
            if (!Crypto.verifySignature(this.pLedger.getTxOutput(curTrans).address, 
                tx.getRawDataToSign(i), inputs.get(i).signature)){
                return false;
            }
            inputSum += this.pLedger.getTxOutput(curTrans).value;
            seenUTXO.add(curTrans);
        }

        for ( int i=0; i<outputs.size(); i ++ ){
            if (outputs.get(i).value <0)
                return false;
            outputSum += outputs.get(i).value;
        }

        if (outputSum > inputSum)
            return false;
        

        return true;
    }
    
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
        ArrayList<Transaction> txs = new ArrayList<Transaction>();
        HashSet<UTXO> usedTX = new HashSet<UTXO>();
        // check that each individual transaction within the recieved block
        // is valid
        // if so then grab the transaction its referencing from UTXO pool 
        // and add it to a map where kep: old trans hash, val: value of the 
        // old transaction minus the amount this transaction is using
        // if another new transaction refers to this old transaction
        // then subtract its value in this map from the old transaction
        int n = possibleTxs.length;
        for (int i=0; i < n; i++){
            ArrayList<Transaction.Input> input = possibleTxs[i].getInputs();
            boolean valid = true;
            if (this.isValidTx(possibleTxs[i])) {
                // for each input, get the UTXO 
                for (Transaction.Input tmp : possibleTxs[i].getInputs() ){
                    UTXO prevTrans = new UTXO( tmp.prevTxHash, tmp.outputIndex );
                    // check if its been used by a previous block of transactions
                    if (!usedTX.contains(prevTrans))
                        // if no then add to the set of current transactions
                        usedTX.add(prevTrans);
                    else{
                        valid = false;
                        break;
                    }
                }
            }
            else
                valid = false;
            // this transaction has already been seen, break
            if (!valid) continue;
            // get the list of outputs
            ArrayList<Transaction.Output> output = possibleTxs[i].getOutputs();
            // start counting to remember which index the output is at
            int outCntr = 0;
            // for each output
            for ( Transaction.Output cur : possibleTxs[i].getOutputs() ){
                // create new utxo
                UTXO curUT = new UTXO( possibleTxs[i].getHash(), outCntr );
                // add it and the corresponding output to map
                this.pLedger.addUTXO(curUT, cur);
                // increment counter
                outCntr +=1;
            }

            txs.add(possibleTxs[i]);
        }

        for (Transaction cur : txs ){
            int outCntr = 0;
            for ( Transaction.Input tmp : cur.getInputs() ){
                UTXO prevTrans = new UTXO( tmp.prevTxHash, tmp.outputIndex );
                this.pLedger.removeUTXO( prevTrans );
                outCntr += 1;
            }

        }

        Transaction[] output = txs.toArray( new Transaction[txs.size()] );
        return output;

    }
    
}
