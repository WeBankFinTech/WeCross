package com.webank.wecross.network.rpc.handler;

import static com.webank.wecross.exception.WeCrossException.ErrorCode.GET_UA_FAILED;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.wecross.account.AccountManager;
import com.webank.wecross.account.UniversalAccount;
import com.webank.wecross.account.UserContext;
import com.webank.wecross.common.NetworkQueryStatus;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.host.WeCrossHost;
import com.webank.wecross.network.rpc.CustomCommandRequest;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.resource.ResourceDetail;
import com.webank.wecross.restserver.RestRequest;
import com.webank.wecross.restserver.RestResponse;
import com.webank.wecross.routine.htlc.HTLCManager;
import com.webank.wecross.stub.Path;
import com.webank.wecross.stub.TransactionRequest;
import com.webank.wecross.zone.Chain;
import com.webank.wecross.zone.Zone;
import com.webank.wecross.zone.ZoneManager;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** GET/POST /network/stub/resource/method */
public class ResourceURIHandler implements URIHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResourceURIHandler.class);

    private WeCrossHost host;
    private ObjectMapper objectMapper = new ObjectMapper();

    public ResourceURIHandler(WeCrossHost host) {
        this.host = host;
    }

    public WeCrossHost getHost() {
        return host;
    }

    public void setHost(WeCrossHost host) {
        this.host = host;
    }

    private Resource getResource(Path path) {
        Resource resourceObj;
        try {
            resourceObj = host.getZoneManager().fetchResource(path);
        } catch (Exception e) {
            logger.error("getResource error", e);
            return null;
        }
        if (resourceObj == null) {
            logger.warn("Unable to find resource: {}", path);
        } else {
            HTLCManager htlcManager = host.getRoutineManager().getHtlcManager();
            resourceObj = htlcManager.filterHTLCResource(host.getZoneManager(), path, resourceObj);
        }

        return resourceObj;
    }

    @Override
    public void handle(
            UserContext userContext,
            String uri,
            String httpMethod,
            String content,
            Callback callback) {
        RestResponse<Object> restResponse = new RestResponse<>();
        try {
            UniversalAccount ua = host.getAccountManager().getUniversalAccount(userContext);
            if (ua == null) {
                throw new WeCrossException(GET_UA_FAILED, "");
            }

            String[] splits = uri.substring(1).split("/");

            Path path = new Path();
            path.setZone(splits[0]);
            path.setChain(splits[1]);

            String method = "";
            if (splits.length > 3) {
                path.setResource(splits[2]);
                method = splits[3];
            } else {
                method = splits[2];
            }

            if (logger.isDebugEnabled()) {
                logger.debug("request path: {}, method: {}, string: {}", path, method, content);
            }

            AccountManager accountManager = host.getAccountManager();

            switch (method.toLowerCase()) {
                case "status":
                    {
                        Resource resourceObj = getResource(path);
                        if (resourceObj == null) {
                            restResponse.setData("not exists");
                        } else {
                            restResponse.setData("exists");
                        }
                        break;
                    }
                case "detail":
                    {
                        Resource resourceObj = getResource(path);
                        if (resourceObj == null) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        } else {
                            ResourceDetail resourceDetail = new ResourceDetail();
                            restResponse.setData(
                                    resourceDetail.initResourceDetail(
                                            resourceObj, path.toString()));
                        }
                        break;
                    }
                case "sendtransaction":
                    {
                        RestRequest<TransactionRequest> restRequest =
                                objectMapper.readValue(
                                        content,
                                        new TypeReference<RestRequest<TransactionRequest>>() {});

                        restRequest.checkRestRequest(path.toString(), method);

                        TransactionRequest transactionRequest = restRequest.getData();
                        Resource resourceObj = getResource(path);
                        if (Objects.isNull(resourceObj)) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        }

                        String accountName = ua.getName();
                        logger.trace(
                                "sendTransaction request: {}, universal account: {}",
                                transactionRequest,
                                accountName);

                        resourceObj.asyncSendTransaction(
                                transactionRequest,
                                ua,
                                (transactionException, transactionResponse) -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(
                                                " TransactionResponse: {}, TransactionException, ",
                                                transactionResponse,
                                                transactionException);
                                    }

                                    if (transactionException != null
                                            && !transactionException.isSuccess()) {
                                        restResponse.setErrorCode(
                                                NetworkQueryStatus.TRANSACTION_ERROR
                                                        + transactionException.getErrorCode());
                                        restResponse.setMessage(transactionException.getMessage());
                                    } else {
                                        restResponse.setData(transactionResponse);
                                    }

                                    callback.onResponse(restResponse);
                                });
                        // Response Will be returned in the callback
                        return;
                    }
                case "call":
                    {
                        RestRequest<TransactionRequest> restRequest =
                                objectMapper.readValue(
                                        content,
                                        new TypeReference<RestRequest<TransactionRequest>>() {});

                        restRequest.checkRestRequest(path.toString(), method);

                        TransactionRequest transactionRequest = restRequest.getData();
                        Resource resourceObj = getResource(path);
                        if (Objects.isNull(resourceObj)) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        }

                        String accountName = ua.getName();
                        logger.trace(
                                "sendTransaction request: {}, universal account: {}",
                                transactionRequest,
                                accountName);

                        // TODO: byProxy
                        resourceObj.asyncCall(
                                transactionRequest,
                                ua,
                                (transactionException, transactionResponse) -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(
                                                " TransactionResponse: {}, TransactionException, ",
                                                transactionResponse,
                                                transactionException);
                                    }

                                    if (transactionException != null
                                            && !transactionException.isSuccess()) {
                                        restResponse.setErrorCode(
                                                NetworkQueryStatus.TRANSACTION_ERROR
                                                        + transactionException.getErrorCode());
                                        restResponse.setMessage(transactionException.getMessage());
                                    } else {
                                        restResponse.setData(transactionResponse);
                                    }

                                    callback.onResponse(restResponse);
                                });
                        // Response Will be returned in the callback
                        return;
                    }
                case "customcommand":
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("zone: {}, chain: {}", path.getZone(), path.getChain());
                        }

                        ZoneManager zoneManager = host.getZoneManager();
                        Zone zone = zoneManager.getZone(path.getZone());
                        if (Objects.isNull(zone)) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.INTERNAL_ERROR,
                                    " zone not exist, zone: " + path.getZone());
                        }

                        Chain chain = zone.getChain(path.getChain());
                        if (Objects.isNull(chain)) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.INTERNAL_ERROR,
                                    " chain not exist, chain: " + path.getChain());
                        }

                        if (chain == null) {
                            throw new WeCrossException(
                                    -1,
                                    "Chain: "
                                            + path.getZone()
                                            + "."
                                            + path.getChain()
                                            + " not found!");
                        }

                        RestRequest<CustomCommandRequest> restRequest =
                                objectMapper.readValue(
                                        content,
                                        new TypeReference<RestRequest<CustomCommandRequest>>() {});

                        chain.asyncCustomCommand(
                                restRequest.getData().getCommand(),
                                path,
                                restRequest.getData().getArgs().toArray(),
                                ua,
                                (e, response) -> {
                                    if (Objects.nonNull(e)) {
                                        restResponse.setErrorCode(
                                                NetworkQueryStatus.INTERNAL_ERROR);
                                        restResponse.setMessage(e.getMessage());
                                    } else {
                                        restResponse.setData(response);
                                    }

                                    callback.onResponse(restResponse);
                                });

                        return;
                    }
                default:
                    {
                        logger.warn("Unsupported method: {}", method);
                        restResponse.setErrorCode(NetworkQueryStatus.METHOD_ERROR);
                        restResponse.setMessage("Unsupported method: " + method);
                        break;
                    }
            }
        } catch (WeCrossException e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.EXCEPTION_FLAG + e.getErrorCode());
            restResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            logger.warn("Process request error:", e);
            restResponse.setErrorCode(NetworkQueryStatus.INTERNAL_ERROR);
            restResponse.setMessage(e.getLocalizedMessage());
        }

        callback.onResponse(restResponse);
    }
}
