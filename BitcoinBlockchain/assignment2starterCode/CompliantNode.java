import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    double p_graph;
    double p_malicious;
    double p_txDistribution;
    int numRounds;
    int curRound;

    int[] peerTrust;
    int[] roundCnt = new int[100];
    Set<Transaction> confirmedT;
    Set<Transaction> evenT;
    Set<Transaction> oddT;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.peerTrust  = new int[100];
        this.roundCnt = new int[100];
        this.confirmedT = new HashSet<Transaction>();
        this.oddT = new HashSet<Transaction>();
        this.evenT = new HashSet<Transaction>();
        this.curRound = 0;

    }

    public void setFollowees(boolean[] followees) {
        for (int i = 0; i < followees.length; i ++ )
            peerTrust[i] = -2;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        for (Transaction cur : pendingTransactions){
            if (cur.id %2 == 0)
                this.evenT.add(cur);
            if (cur.id % 2 == 1)
                this.oddT.add(cur);
        }
        return;
    }

    public Set<Transaction> sendToFollowers() {
        this.curRound +=1;
        //System.out.println("Round: " + this.curRound);
        if (this.curRound == (this.numRounds+1)){
            this.confirmedT.addAll(this.evenT);
            this.confirmedT.addAll(this.oddT);
            System.out.println("Round: " + this.curRound);
            System.out.println(this.confirmedT.size());
            System.out.println(this.evenT.size());
            System.out.println(this.oddT.size());
            return this.confirmedT;
        }
        if (this.curRound<=this.numRounds-3){
            this.confirmedT.addAll(this.evenT);
            this.confirmedT.addAll(this.oddT);
            return this.confirmedT;
        }
        if (this.curRound % 2 == 0)
            return this.evenT;
        else
            return this.oddT;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        
        HashMap<Integer, LinkedList<Transaction>> curSet = new HashMap<Integer, LinkedList<Transaction>>();
        if (this.curRound <= this.numRounds-3){
            for (Candidate j : candidates)
                this.confirmedT.add(j.tx);
            return;
        }
        for ( Candidate cur: candidates ){
            if (this.curRound %2 == 0){
                if ((cur.tx.id%2 == 0) && this.peerTrust[cur.sender] != 0){
                    if (curSet.containsKey(cur.sender))
                        curSet.get(cur.sender).add(cur.tx);
                    else{
                        LinkedList<Transaction> tmp = new LinkedList<Transaction>();
                        tmp.add(cur.tx);
                        curSet.put(cur.sender, tmp);
                    }
                }
                else{
                    this.peerTrust[cur.sender] = 0;
                    if (curSet.containsKey(cur.sender))
                        curSet.remove(cur.sender);
                }
            }
            else{
                if ((cur.tx.id%2==1) && this.peerTrust[cur.sender] != 0)
                    if (curSet.containsKey(cur.sender))
                        curSet.get(cur.sender).add(cur.tx);
                    else{
                        LinkedList<Transaction> tmp = new LinkedList<Transaction>();
                        tmp.add(cur.tx);
                        curSet.put(cur.sender, tmp);
                    }
                else{
                    this.peerTrust[cur.sender] = 0;
                    if (curSet.containsKey(cur.sender))
                        curSet.remove(cur.sender);
                }
            }

        }

        for (Integer i : curSet.keySet()){
            for (Transaction j : curSet.get(i)){
                if (this.curRound % 2 == 0)
                    this.evenT.add(j);
                else
                    this.oddT.add(j);
            }
        }


    }
}