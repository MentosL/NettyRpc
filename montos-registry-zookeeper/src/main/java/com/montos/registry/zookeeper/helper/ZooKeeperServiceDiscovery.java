package com.montos.registry.zookeeper.helper;

import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.montos.common.util.CollectionUtil;
import com.montos.registry.ServiceDiscovery;
import com.montos.registry.zookeeper.config.LoadBalance;
import com.montos.registry.zookeeper.constant.Constant;

/**
 * 基于 ZooKeeper 的服务发现接口实现
 * 
 * @author montos
 *
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

	private String zkAddress;

	public ZooKeeperServiceDiscovery(String zkAddress) {
		this.zkAddress = zkAddress;
	}

	@Override
	public String discover(String name) {
		// 创建 ZooKeeper 客户端
		ZkClient zkClient = new ZkClient(zkAddress, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
		LOGGER.debug("connect zookeeper");
		try {
			// 获取 service 节点
			String servicePath = Constant.ZK_REGISTRY_PATH + "/" + name;
			if (!zkClient.exists(servicePath)) {
				throw new RuntimeException(String.format("can not find any service node on path: %s", servicePath));
			}
			List<String> addressList = zkClient.getChildren(servicePath);
			if (CollectionUtil.isEmpty(addressList)) {
				throw new RuntimeException(String.format("can not find any address node on path: %s", servicePath));
			}
			// 获取 address 节点
			String address;
			int size = addressList.size();
			if (size == 1) {
				// 若只有一个地址，则获取该地址
				address = addressList.get(0);
				LOGGER.debug("get only address node: {}", address);
			} else {
				// 若存在多个地址，则根据负载均衡算法获取一个地址
				address = LoadBalance.Random_LoadBalance(addressList);
				LOGGER.debug("get random address node: {}", address);
			}
			// 获取 address 节点的值
			String addressPath = servicePath + "/" + address;
			return zkClient.readData(addressPath);
		} finally {
			zkClient.close();
		}
	}
}
