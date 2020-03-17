package com.webank.wecross.config;

import com.moandjiezana.toml.Toml;
import com.webank.wecross.common.WeCrossDefault;
import com.webank.wecross.exception.ErrorCode;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.StubFactory;
import com.webank.wecross.stub.StubManager;
import com.webank.wecross.utils.ConfigUtils;
import com.webank.wecross.zone.Chain;
import com.webank.wecross.zone.Zone;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZonesConfig {
    private Logger logger = LoggerFactory.getLogger(ZonesConfig.class);

    @Resource Toml toml;

    @Resource StubManager stubManager;

    @Bean
    public Map<String, Zone> newZoneMap() {
        Map<String, Zone> result = new HashMap<>();
        try {
            String network = toml.getString("common.network");
            if (network == null) {
                String errorMessage =
                        "\"network\" in [common] item  not found, please check "
                                + WeCrossDefault.MAIN_CONFIG_FILE;
                logger.error(errorMessage);
                System.exit(1);
            }

            Boolean visible = toml.getBoolean("common.visible");
            if (visible == null) {
                String errorMessage =
                        "\"visible\" in [common] item  not found, please check "
                                + WeCrossDefault.MAIN_CONFIG_FILE;
                logger.error(errorMessage);
                System.exit(1);
            }

            String stubsPath = toml.getString("stubs.path");
            if (stubsPath == null) {
                String errorMessage =
                        "\"path\" in [stubs] item  not found, please check "
                                + WeCrossDefault.MAIN_CONFIG_FILE;
                throw new WeCrossException(ErrorCode.FIELD_MISSING, errorMessage);
            }

            Map<String, String> stubsDir = ConfigUtils.getStubsDir(stubsPath);
            Map<String, Chain> stubsBean = getChains(network, stubsDir);
            Zone networkBean = new Zone();
            if (stubsBean != null) {
                // init network bean
                networkBean.setChains(stubsBean);
                networkBean.setVisible(visible);
                result.put(network, networkBean);
            } else {
                logger.error("No stubs found in {}", network);
                System.exit(1);
            }

        } catch (WeCrossException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }

        return result;
    }

    public Map<String, Chain> getChains(String network, Map<String, String> stubsDir)
            throws WeCrossException {
        Map<String, Chain> stubMap = new HashMap<>();

        for (String chainName : stubsDir.keySet()) {
            String stubPath = stubsDir.get(chainName);
            Toml stubToml;
            try {
                stubToml = ConfigUtils.getToml(stubPath);
            } catch (WeCrossException e) {
                String errorMessage = "Parse " + stubPath + " failed";
                throw new WeCrossException(ErrorCode.UNEXPECTED_CONFIG, errorMessage);
            }

            String type = stubToml.getString("common.type");
            if (type == null) {
                String errorMessage =
                        "\"type\" in [common] item  not found, please check " + stubPath;
                throw new WeCrossException(ErrorCode.FIELD_MISSING, errorMessage);
            }

            StubFactory stubFactory = stubManager.getStubFactory(type);
            if (stubFactory == null) {
                logger.error("Can not find stub type: {}", type);

                throw new WeCrossException(-1, "Cannot find stub type: " + type);
            }
            Connection connection = stubFactory.newConnection(stubPath);
            List<ResourceInfo> resources = connection.getResources();

            Chain chain = new Chain();
            for (ResourceInfo resourceInfo : resources) {
                com.webank.wecross.resource.Resource resource =
                        new com.webank.wecross.resource.Resource();
                resource.setDriver(stubFactory.newDriver());
                resource.addConnection(null, connection);
                resource.setType(type);
                resource.setResourceInfo(resourceInfo);
                chain.getResources().put(resourceInfo.getName(), resource);
            }

            chain.start();
            logger.info("Start block header sync: {}", network + "." + chainName);
            stubMap.put(chainName, chain);
        }

        return stubMap;
    }

    public Toml getToml() {
        return toml;
    }

    public void setToml(Toml toml) {
        this.toml = toml;
    }
}
