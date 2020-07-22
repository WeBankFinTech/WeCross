package com.webank.wecross.routine.xa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.Path;
import com.webank.wecross.stub.TransactionRequest;
import com.webank.wecross.zone.Chain;
import com.webank.wecross.zone.ZoneManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XATransactionManager {
    private Logger logger = LoggerFactory.getLogger(XATransactionManager.class);
    ObjectMapper objectMapper = new ObjectMapper();
    private ZoneManager zoneManager;

    public interface Callback {
        public void onResponse(Exception e, int result);
    }

    public Map<String, Set<Path>> getChainPaths(Set<Path> resources) {
        Map<String, Set<Path>> zone2Path = new HashMap<String, Set<Path>>();
        for (Path path : resources) {
            String key = path.getZone() + "." + path.getChain();
            if (zone2Path.get(key) == null) {
                zone2Path.put(key, new HashSet<Path>());
            }

            zone2Path.get(key).add(path);
        }

        return zone2Path;
    }

    private Callback getReduceCallback(int size, Callback callback) {
        AtomicInteger finished = new AtomicInteger();
        List<Exception> fatals = Collections.synchronizedList(new LinkedList<Exception>());

        Callback reduceCallback =
                (error, result) -> {
                    if (error != null) {
                        logger.error("Process error!", error);
                        fatals.add(error);
                    }

                    int current = finished.addAndGet(1);
                    if (current == size) {
                        // all finished
                        if (fatals.size() > 0) {
                            logger.error("Failed in progress", fatals);

                            callback.onResponse(new WeCrossException(-1, "Failed in progress"), -1);
                        } else {
                            callback.onResponse(null, 0);
                        }
                    }
                };

        return reduceCallback;
    }

    public void asyncStartTransaction(
            String transactionID,
            Map<String, Account> accounts,
            Set<Path> resources,
            Callback callback) {
        try {
            logger.info("Start transaction, resources: {}", resources);

            Map<String, Set<Path>> zone2Path = getChainPaths(resources);

            Callback reduceCallback = getReduceCallback(zone2Path.size(), callback);

            for (Map.Entry<String, Set<Path>> entry : zone2Path.entrySet()) {
                Path chainPath = entry.getValue().iterator().next();

                Chain chain = zoneManager.getZone(chainPath).getChain(chainPath);

                // send prepare transaction
                TransactionRequest transactionRequest = new TransactionRequest();
                transactionRequest.setMethod("startTransaction");
                String[] args = new String[entry.getValue().size() + resources.size() + 2];

                args[0] = transactionID;
                args[1] = String.valueOf(entry.getValue().size());

                int i = 0;
                for (Path path : entry.getValue()) {
                    args[i + 2] = path.toString();
                    ++i;
                }

                for (Path path : resources) {
                    args[i + 2] = path.toString();
                    ++i;
                }

                transactionRequest.setArgs(args);
                transactionRequest.getOptions().put(Resource.RAW_TRANSACTION, true);

                Path proxyPath = new Path(chainPath);
                proxyPath.setResource("WeCrossProxy");
                Resource resource = zoneManager.fetchResource(proxyPath);

                Account account = accounts.get(resource.getStubType());
                if (Objects.isNull(account)) {
                    String errorMsg =
                            "account with type '" + resource.getStubType() + "' not found";
                    logger.error(errorMsg);
                    reduceCallback.onResponse(new Exception(errorMsg), -1);
                    return;
                }

                resource.asyncSendTransaction(
                        transactionRequest,
                        account,
                        (error, response) -> {
                            if (error != null) {
                                logger.error("Send prepare transaction system error", error);

                                reduceCallback.onResponse(error, -1);
                                return;
                            }

                            if (response.getErrorCode() != 0) {
                                logger.error(
                                        "Send startTransaction chain error: {} {}",
                                        response.getErrorCode(),
                                        response.getErrorMessage());

                                reduceCallback.onResponse(
                                        new WeCrossException(
                                                -1, "Send startTransaction chain error"),
                                        -1);
                                return;
                            }

                            int errorCode = Integer.parseInt(response.getResult()[0]);
                            if (errorCode != 0) {
                                logger.error("Send prepare transaction proxy error: {}", errorCode);
                            }

                            reduceCallback.onResponse(null, errorCode);
                        });
            }
        } catch (Exception e) {
            logger.error("Prepare error", e);

            callback.onResponse(new WeCrossException(-1, "Prepare error", e), -1);
        }
    };

    public void asyncCommitTransaction(
            String transactionID,
            Map<String, Account> accounts,
            Set<Path> chains,
            Callback callback) {
        try {
            logger.info("Commit transaction, chains: {}", chains);

            Callback reduceCallback = getReduceCallback(chains.size(), callback);

            for (Path chainPath : chains) {
                Chain chain = zoneManager.getZone(chainPath).getChain(chainPath);

                // send prepare transaction
                TransactionRequest transactionRequest = new TransactionRequest();
                transactionRequest.setMethod("commitTransaction");
                String[] args = new String[] {transactionID};
                transactionRequest.setArgs(args);
                transactionRequest.getOptions().put(Resource.RAW_TRANSACTION, true);

                Path proxyPath = new Path(chainPath);
                proxyPath.setResource("WeCrossProxy");
                Resource resource = zoneManager.fetchResource(proxyPath);

                Account account = accounts.get(resource.getStubType());
                if (Objects.isNull(account)) {
                    String errorMsg =
                            "account with type '" + resource.getStubType() + "' not found";
                    logger.error(errorMsg);
                    reduceCallback.onResponse(new Exception(errorMsg), -1);
                    return;
                }

                resource.asyncSendTransaction(
                        transactionRequest,
                        account,
                        (error, response) -> {
                            if (error != null) {
                                logger.error("Send commit transaction error", error);

                                reduceCallback.onResponse(
                                        new WeCrossException(
                                                -1, "Send commit transaction error", error),
                                        -1);
                                return;
                            }

                            int errorCode = Integer.parseInt(response.getResult()[0]);
                            if (errorCode != 0) {
                                logger.error("Send prepare transaction proxy error: {}", errorCode);
                            }

                            reduceCallback.onResponse(null, errorCode);
                        });
            }
        } catch (Exception e) {
            logger.error("Commit error", e);

            callback.onResponse(new WeCrossException(-1, "Commit error", e), -1);
        }
    };

    public void asyncRollback(
            String transactionID,
            Map<String, Account> accounts,
            Set<Path> chains,
            Callback callback) {
        try {
            Callback reduceCallback = getReduceCallback(chains.size(), callback);

            for (Path chainPath : chains) {
                Chain chain = zoneManager.getZone(chainPath).getChain(chainPath);

                // send rollback transaction
                TransactionRequest transactionRequest = new TransactionRequest();
                transactionRequest.setMethod("rollbackTransaction");
                String[] args = new String[] {transactionID};
                transactionRequest.setArgs(args);
                transactionRequest.getOptions().put(Resource.RAW_TRANSACTION, true);

                Path proxyPath = new Path(chainPath);
                proxyPath.setResource("WeCrossProxy");
                Resource resource = zoneManager.fetchResource(proxyPath);

                Account account = accounts.get(resource.getStubType());
                if (Objects.isNull(account)) {
                    String errorMsg =
                            "account with type '" + resource.getStubType() + "' not found";
                    logger.error(errorMsg);
                    reduceCallback.onResponse(new Exception(errorMsg), -1);
                    return;
                }

                resource.asyncSendTransaction(
                        transactionRequest,
                        account,
                        (error, response) -> {
                            if (error != null) {
                                logger.error("Send rollback transaction error", error);

                                reduceCallback.onResponse(
                                        new WeCrossException(
                                                -1, "Send rollback transaction error", error),
                                        -1);
                                return;
                            }

                            int errorCode = Integer.parseInt(response.getResult()[0]);
                            if (errorCode != 0) {
                                logger.error("Send prepare transaction proxy error: {}", errorCode);
                            }

                            reduceCallback.onResponse(null, errorCode);
                        });
            }
        } catch (Exception e) {
            logger.error("Rollback error", e);

            callback.onResponse(new WeCrossException(-1, "Rollback error", e), -1);
        }
    }

    public interface GetTransactionInfoCallback {
        public void onResponse(Exception e, XATransactionInfo xaTransactionInfo);
    }

    private GetTransactionInfoCallback getTransactionInfoReduceCallback(
            int size, GetTransactionInfoCallback callback) {
        AtomicInteger finished = new AtomicInteger();
        List<Exception> fatals = Collections.synchronizedList(new LinkedList<Exception>());
        List<XATransactionInfo> infos =
                Collections.synchronizedList(new LinkedList<XATransactionInfo>());

        GetTransactionInfoCallback reduceCallback =
                (error, info) -> {
                    if (error != null) {
                        logger.error("Get transactionInfo error in progress", error);
                        fatals.add(error);
                    } else {
                        infos.add(info);
                    }

                    int current = finished.addAndGet(1);
                    if (current == size) {
                        if (infos.isEmpty()) {
                            logger.error("Empty result");
                            callback.onResponse(new WeCrossException(-1, "Empty result"), null);

                            return;
                        }

                        if (infos.size() < size) {
                            logger.warn("GetTransactionInfo missing some steps");
                        }

                        List<XATransactionStep> allSteps = new ArrayList<XATransactionStep>();
                        for (XATransactionInfo returnInfo : infos) {
                            allSteps.addAll(returnInfo.getTransactionSteps());
                        }

                        allSteps.sort(
                                (left, right) -> {
                                    return Integer.compare(left.getSeq(), right.getSeq());
                                });

                        XATransactionInfo xaTransactionInfo = infos.get(0);
                        xaTransactionInfo.setTransactionSteps(allSteps);

                        callback.onResponse(null, xaTransactionInfo);
                    }
                };

        return reduceCallback;
    }

    public void asyncGetTransactionInfo(
            String transactionID,
            Map<String, Account> accounts,
            Set<Path> chains,
            GetTransactionInfoCallback callback) {
        try {
            GetTransactionInfoCallback reduceCallback =
                    getTransactionInfoReduceCallback(chains.size(), callback);

            for (Path path : chains) {
                Chain chain = zoneManager.getZone(path).getChain(path);

                TransactionRequest transactionRequest = new TransactionRequest();
                transactionRequest.setMethod("getTransactionInfo");
                String[] args = new String[] {transactionID};
                transactionRequest.setArgs(args);
                transactionRequest.getOptions().put(Resource.RAW_TRANSACTION, true);

                Path proxyPath = new Path(path);
                proxyPath.setResource("WeCrossProxy");
                Resource resource = zoneManager.getResource(proxyPath);

                Account account = accounts.get(resource.getStubType());
                if (Objects.isNull(account)) {
                    String errorMsg =
                            "account with type '" + resource.getStubType() + "' not found";
                    logger.error(errorMsg);
                    reduceCallback.onResponse(new Exception(errorMsg), null);
                    return;
                }

                resource.asyncCall(
                        transactionRequest,
                        account,
                        (error, response) -> {
                            if (error != null) {
                                logger.error("Send prepare transaction error", error);

                                reduceCallback.onResponse(
                                        new WeCrossException(-1, "GetTransactionInfo error", error),
                                        null);
                                return;
                            }

                            // decode return value
                            try {
                                XATransactionInfo xaTransactionInfo =
                                        objectMapper.readValue(
                                                response.getResult()[0], XATransactionInfo.class);

                                reduceCallback.onResponse(null, xaTransactionInfo);
                            } catch (Exception e) {
                                logger.error("Decode transactionInfo json error", e);

                                reduceCallback.onResponse(
                                        new WeCrossException(
                                                -1, "Decode transactionInfo json error", e),
                                        null);
                                return;
                            }
                        });
            }
        } catch (Exception e) {
            logger.error("GetTransactionInfo error", e);

            callback.onResponse(new WeCrossException(-1, "GetTransactionInfo error", e), null);
        }
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public void setZoneManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }
}
