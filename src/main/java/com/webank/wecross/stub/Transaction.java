package com.webank.wecross.stub;

public class Transaction {

    private byte[] txBytes; // 交易信息
    private byte[] receiptBytes; // 交易回执信息

    private String transactionID;
    private String seq;
    private Path path;

    private long blockNumber;
    private String transactionHash;
    private TransactionRequest transactionRequest;
    private TransactionResponse transactionResponse;

    private boolean transactionByProxy = false;

    public Transaction(
            long blockNumber,
            String transactionHash,
            TransactionRequest transactionRequest,
            TransactionResponse transactionResponse) {
        this.blockNumber = blockNumber;
        this.transactionHash = transactionHash;
        this.transactionRequest = transactionRequest;
        this.transactionResponse = transactionResponse;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public TransactionRequest getTransactionRequest() {
        return transactionRequest;
    }

    public void setTransactionRequest(TransactionRequest transactionRequest) {
        this.transactionRequest = transactionRequest;
    }

    public TransactionResponse getTransactionResponse() {
        return transactionResponse;
    }

    public void setTransactionResponse(TransactionResponse transactionResponse) {
        this.transactionResponse = transactionResponse;
    }

    public byte[] getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(byte[] txBytes) {
        this.txBytes = txBytes;
    }

    public byte[] getReceiptBytes() {
        return receiptBytes;
    }

    public void setReceiptBytes(byte[] receiptBytes) {
        this.receiptBytes = receiptBytes;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public boolean isTransactionByProxy() {
        return transactionByProxy;
    }

    public void setTransactionByProxy(boolean transactionByProxy) {
        this.transactionByProxy = transactionByProxy;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    @Override
    public String toString() {
        return "Transaction{"
                + "transactionID='"
                + transactionID
                + '\''
                + ", seq="
                + seq
                + ", path="
                + path
                + ", blockNumber="
                + blockNumber
                + ", transactionHash='"
                + transactionHash
                + '\''
                + ", transactionByProxy="
                + transactionByProxy
                + ", transactionRequest="
                + transactionRequest
                + ", transactionResponse="
                + transactionResponse
                + '}';
    }
}