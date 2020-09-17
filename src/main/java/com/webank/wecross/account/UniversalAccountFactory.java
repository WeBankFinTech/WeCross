package com.webank.wecross.account;

import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.StubFactory;
import com.webank.wecross.stubmanager.StubManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalAccountFactory {
    private static Logger logger = LoggerFactory.getLogger(UniversalAccountFactory.class);
    private StubManager stubManager;

    public UniversalAccount buildUA(UniversalAccount.UADetails uaDetails) throws WeCrossException {
        UniversalAccount ua =
                UniversalAccount.builder()
                        .username(uaDetails.getUsername())
                        .uaID(uaDetails.getUaID())
                        .pubKey(uaDetails.getPubKey())
                        .isAdmin(uaDetails.isAdmin())
                        .build();

        // foreach details, set default account into ua
        for (Map<Integer, UniversalAccount.ChainAccountDetails> chainAccountDetailsMap :
                uaDetails.getType2ChainAccounts().values()) {
            for (UniversalAccount.ChainAccountDetails details : chainAccountDetailsMap.values()) {

                String type = details.getType();
                StubFactory stubFactory = stubManager.getStubFactory(type);
                if (stubFactory == null) {
                    logger.error("Stub type not found: " + type);
                }

                Account account = stubFactory.newAccount(details.toProperties());

                if (account == null) {
                    logger.error(
                            "Default account generate failed! properties:"
                                    + details.toProperties());
                    continue;
                }

                ua.addAccount(type, details.getKeyID(), account, details);

                if (details.isDefault()) {
                    ua.setDefaultAccount(type, account);
                }
            }
        }

        return ua;
    }

    public void setStubManager(StubManager stubManager) {
        this.stubManager = stubManager;
    }
}
