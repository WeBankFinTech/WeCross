package com.webank.wecross.stub;

public interface Driver {
    interface Callback {
        void onTransactionResponse(TransactionResponse transactionResponse);
    }

    /**
     * Decode an encoded transaction request binary data.
     *
     * @param data the encoded transaction request binary data
     * @return TransactionRequest
     */
    public TransactionContext<TransactionRequest> decodeTransactionRequest(byte[] data);

    /**
     * Check if the request is transaction
     *
     * @param request
     * @return true if transaction
     */
    public boolean isTransaction(Request request);

    /**
     * Decode an encoded block header binary data.
     *
     * @param data the encoded block header
     * @return BlockHeader
     */
    public BlockHeader decodeBlockHeader(byte[] data);

    /**
     * Call the interface of contract or chaincode
     *
     * @param request the transaction request
     * @param connection the connection of a chain
     * @return the transaction response
     */
    public TransactionResponse call(
            TransactionContext<TransactionRequest> request, Connection connection);

    /**
     * Async Call the interface of contract or chaincode Just fake async for compatibility, you need
     * to override this function
     *
     * @param request the transaction request
     * @param connection the connection of a chain
     * @param callback the callback class for async call
     * @return the transaction response
     */
    default void asyncCall(
            TransactionContext<TransactionRequest> request,
            Connection connection,
            Driver.Callback callback) {
        Thread thread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onTransactionResponse(call(request, connection));
                            }
                        });
        thread.start();
    }

    /**
     * Send transaction to the interface of contract or chaincode
     *
     * @param request the transaction request
     * @return the transaction response
     */
    public TransactionResponse sendTransaction(
            TransactionContext<TransactionRequest> request, Connection connection);

    /**
     * Async transaction the interface of contract or chaincode Just fake async for compatibility,
     * you need to override this function
     *
     * @param request the transaction request
     * @param connection the connection of a chain
     * @param callback the callback class for async sendTransaction
     * @return the transaction response
     */
    default void asyncSendTransaction(
            TransactionContext<TransactionRequest> request,
            Connection connection,
            Driver.Callback callback) {
        Thread thread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                callback.onTransactionResponse(
                                        sendTransaction(request, connection));
                            }
                        });
        thread.start();
    }

    /**
     * Get block number
     *
     * @return block number
     */
    public long getBlockNumber(Connection connection);

    /**
     * Get block header
     *
     * @param blockNumber
     * @return BlockHeader
     */
    public byte[] getBlockHeader(long blockNumber, Connection connection);

    /**
     * Get verified transaction info of the Chain
     *
     * @param transactionHash
     * @param blockNumber
     * @param blockHeaderManager
     * @param connection
     * @return null if the transaction has not been verified
     */
    public VerifiedTransaction getVerifiedTransaction(
            String transactionHash,
            long blockNumber,
            BlockHeaderManager blockHeaderManager,
            Connection connection);
}
