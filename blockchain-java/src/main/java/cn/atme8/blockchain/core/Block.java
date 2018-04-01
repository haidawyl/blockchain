package cn.atme8.blockchain.core;


import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Block {
    int iIndex; // 索引
    String sProof; // 工作量证明，在这个例子里面，其实就是一个经过验证的正确的成语
    String sPreviousHash; // 前一个区块的Hash值
    Timestamp tsCreateTime; // 区块创建时间戳

    /*
     * 数据块
     * 用户每接上一个成语，会得到系统10元钱的奖励，同时会赢得前面一个用户的2元钱
     * 数据区同时需要记录自己的用户名和回答出上一个成语的用户名
     *
     **/
    String sSender; // 回答出上一个成语的用户名
    String sRecipient; // 回答出当前这个成语的用户名
    public static final int iMoneyAward = 10; // 系统奖励，数额固定
    public static int iMoneyWin = 2; // 赢取奖励，数额固定

    public Block(int iIndex, String sProof, String sPreviousHash, Timestamp tsCreateTime, String sSender, String sRecipient) {
        this.iIndex = iIndex;
        this.sProof = sProof;
        this.sPreviousHash = sPreviousHash;
        this.tsCreateTime = tsCreateTime;
        this.sSender = sSender;
        this.sRecipient = sRecipient;
    }

    public int getiIndex() {
        return iIndex;
    }

    public String getsProof() {
        return sProof;
    }

    public String getsPreviousHash() {
        return sPreviousHash;
    }

    public Timestamp getTsCreateTime() {
        return tsCreateTime;
    }

    public String getsSender() {
        return sSender;
    }

    public String getsRecipient() {
        return sRecipient;
    }

    public String toInfoString() {

        return this.iIndex + "#" + this.sProof + "#" + this.sPreviousHash + "#" + String.valueOf(this.tsCreateTime.getTime()) + "#" + this.sSender + "#" + this.sRecipient;
    }
}
