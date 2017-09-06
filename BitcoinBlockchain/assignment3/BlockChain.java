import java.util.*;
import java.nio.ByteBuffer;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int  CUT_OFF_AGE = 10;

    private class QueuePair{
        ByteBuffer blockHash;
        Integer curCnt;

        private QueuePair(ByteBuffer blockHash, Integer curCnt){
            this.blockHash = blockHash;
            this.curCnt = curCnt;
        }
    }

    private class BlockClass{
        Block block;
        Integer height;
        ByteBuffer hash;

        private BlockClass(Block blockHash, Integer curCnt, ByteBuffer hash){
            this.block = blockHash;
            this.height = curCnt;
            this.hash = hash;
        }
    }
    // for knowing which node is the oldest/which to remove next
    Queue<QueuePair> chainAge;
    // the actual blockchain
    HashMap<ByteBuffer, BlockClass> curChain;
    /* for knowing which nodes on the chain are the front most node(s)
       using a map because there may be more than one front node since
       we allow forking in the tree 
    */
    HashMap<ByteBuffer, UTXOPool> edgeBlocks;
    TransactionPool tPool;
    // centralized time
    int nodeAge = 0;
    // current front most node
    BlockClass highestNode;
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     *
     * @chainAge Queue to keep track of age of each hash, Hashmap to store the actual nodes
     * Using ByteBuffer to not have to deal with strage hashing issues of byte[]
     * to keep track of the top of the chain/tree
     * @edgeBlocks Should maintain a map of block hashes to UTXOPools
     * @curChain inmem storage of blockchain
     */
    public BlockChain(Block genesisBlock) {
        this.curChain = new HashMap<ByteBuffer, BlockClass>();
        this.chainAge = new LinkedList<QueuePair>();
        this.edgeBlocks = new HashMap<ByteBuffer, UTXOPool>();
        this.nodeAge+=1;
        this.tPool = new TransactionPool();
        ByteBuffer hash = ByteBuffer.wrap(genesisBlock.getHash());
        QueuePair key = new QueuePair( hash, this.nodeAge);
        this.chainAge.add(key);
        // store block/height
        BlockClass gen = new BlockClass(genesisBlock, 1, hash);
        this.curChain.put(hash, gen);
        UTXOPool newPool = new UTXOPool();
        newPool = this.addGenUTXO( newPool, genesisBlock );
        this.highestNode = gen;
        this.edgeBlocks.put(hash, newPool);
    }

    private UTXOPool addGenUTXO(UTXOPool newPool, Block genBlock){
        Transaction tmp = genBlock.getCoinbase();
        ArrayList<Transaction> txs = genBlock.getTransactions();
        txs.add(tmp);
        for ( Transaction tx : genBlock.getTransactions() ){
            int index = 0;
            for (Transaction.Output opt : tx.getOutputs() ){
                newPool.addUTXO( new UTXO( tx.getHash(), index ), opt );
                index += 1;
            }
        }
        return newPool;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return this.highestNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // THE WAY THIS IS IMPLEMENTED ensures that the oldest block is always returned if
        // two blocks are at the same max height. this is becuase this.highestNode will only
        // change if a new node is strictly higher than the cur highest node
        return this.edgeBlocks.get(this.highestNode.hash);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return this.tPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        /*
        - need to increment age
        - need to check if another genesis block
        - need to check if parent is still in the hash table
        - need to check validity
        - need to grab prev UTXOPool
        - check if the highest, and if so update
        - add block to edgeNodes
        - add block to chainAge
        - add block to curChain
        */
        this.nodeAge += 1;
        //this.removeOldBlocks();
        this.checkTx(block);
        // can't have new genesis block
        if (block.getPrevBlockHash() == null)
            return false;
        ByteBuffer parentHash = ByteBuffer.wrap(block.getPrevBlockHash());
        // parent doesn't exist anymore
        if (!this.curChain.containsKey(parentHash))
            return false;
        UTXOPool prevPool = this.edgeBlocks.get(parentHash);
        TxHandler newHandler = new TxHandler(prevPool);
        // get all new transactions, convert to array, pass into UTXOPool handler
        // then check to make sure output Tx array is of same size as input Tx
        // array. if not reject bc all transactions have to be valid
        ByteBuffer curHash = ByteBuffer.wrap(block.getHash());
        Transaction[] validTrans = newHandler.handleTxs( block.getTransactions().
            toArray( new Transaction[block.getTransactions().size()] ) );
        UTXOPool newPool = newHandler.getUTXOPool();
        newPool.addUTXO( new UTXO(block.getCoinbase().getHash(), 0), block.getCoinbase().getOutput(0) );
        //this.edgeBlocks.put(parentHash, newPool);
        this.edgeBlocks.put(curHash, newPool);
        BlockClass tmp = new BlockClass( block, this.curChain.get(parentHash).height+1, curHash );
        this.checkHeights(tmp);
        this.curChain.put(curHash, tmp);
        QueuePair key = new QueuePair( curHash, this.nodeAge);
        this.chainAge.add(key);
        this.filterTransactions(validTrans);
        return true;
    }

    private void checkTx(Block block){

    }

    private void filterTransactions(Transaction[] validTrans){
        for (int i = 0; i < validTrans.length; i++)
            this.tPool.removeTransaction( validTrans[i].getHash() );
        
    }

    private void checkHeights(BlockClass cur){
        if(this.highestNode.height < cur.height)
            this.highestNode = cur;
        return;
    }

    // remove any block older than current epoch - max_age
    // remove from queue, block chain and edgeBlocks
    private void removeOldBlocks(){
        QueuePair curOldest = this.chainAge.peek();
        if (curOldest == null) return;
        while (curOldest.curCnt < this.nodeAge - this.CUT_OFF_AGE  ){
            this.curChain.remove(curOldest.blockHash);
            this.edgeBlocks.remove(curOldest.blockHash);
            this.chainAge.remove();
            curOldest = this.chainAge.peek();
        }
        return;
    }
    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.tPool.addTransaction(tx);
    }
}