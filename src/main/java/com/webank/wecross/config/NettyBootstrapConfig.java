package com.webank.wecross.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webank.wecross.p2p.P2PConfig;
import com.webank.wecross.p2p.netty.NettyBootstrap;
import com.webank.wecross.p2p.netty.message.MessageCallBack;

@Configuration
public class NettyBootstrapConfig {
	@Resource
	P2PConfig p2pConfig;
	
	@Resource
	MessageCallBack messageCallBack;
	
	@Bean
	public NettyBootstrap newNettyBootstrap() {
		NettyBootstrap bootstrap = new NettyBootstrap();
		bootstrap.setConfig(p2pConfig);
		bootstrap.setMessageCallBack(messageCallBack);

		return bootstrap;
	}
}
