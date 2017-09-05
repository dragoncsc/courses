import java.util.*;
import java.nio.ByteBuffer;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int  CUT_OFF_AGE = 10;

    private class QueuePair{
        ByteBuffer prevBlockHash;
        Integer curCnt;

        private QueuePair(ByteBuffer prevBlockHash, Integer curCnt){
            this.prevBlockHash = prevBlockHash;
            this.curCnt = curCnt;
        }
    }
    private class IntPair{
        int age;
        UTXOPool top;
        int height;

        private IntPair(int age, int height, UTXOPool pool){
            this.age = age;
            this.top = pool;
            this.height = height;
        }

        private IntPair(int age, int height){
            this.age = age;
            this.top = new UTXOPool();
            this.height = height;
        }
        private UTXOPool getPool(){
            return top;
        }
    }
    // for knowing which node is the oldest/which to remove next
    Queue<QueuePair> chainAge;
    // the actual blockchain
    HashMap<ByteBuffer, Block> curChain;
    /* for knowing which nodes on the chain are the front most node(s)
       using a map because there may be more than one front node since
       we allow forking in the tree 
    */
    HashMap<ByteBuffer, IntPair> edgeBlocks;
    TransactionPool cur;
    // centralized time
    int nodeAge = 0;
    // current front most node
    ByteBuffer highestNode;
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     *
     * Queue to keep track of age of each hash, Hashmap to store the actual nodes
     * Using ByteBuffer to not have to deal with strage hashing issues of byte[]
     * to keep track of the top of the chain/tree, should maintain a set of
     * blocks that are on the 'top' of the stack. This way we won't have to 
     * traverse all the way down each branch to get largest branch
     * this set of blocks should be represented by a a map from hash to 
     * <int, int> pairs, where (1) is the age and (2) is the height
     * this makes for easy incrementing
     */
    public BlockChain(Block genesisBlock) {
        this.curChain = new HashMap<ByteBuffer, Block>();
        this.chainAge = new LinkedList<QueuePair>();
        this.edgeBlocks = new HashMap<ByteBuffer, IntPair>();
        this.nodeAge+=1;
        this.cur = new TransactionPool();
        ByteBuffer hash = ByteBuffer.wrap(genesisBlock.getPrevBlockHash());
        QueuePair key = new QueuePair( hash, this.nodeAge);
        this.highestNode = hash;
        this.chainAge.add(key);
        this.curChain.put(hash, genesisBlock);
        IntPair firstPair = new IntPair(this.nodeAge, 1);
        this.edgeBlocks.put(hash, firstPair);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return this.curChain.get(this.highestNode);
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        this.edgeBlocks.get(this.highestNode).getPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
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
        // IMPLEMENT THIS
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.cur.addTransaction(tx);
    }
}