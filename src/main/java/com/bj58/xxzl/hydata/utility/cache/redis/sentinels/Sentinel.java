package com.bj58.xxzl.hydata.utility.cache.redis.sentinels;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Sentinel 外层实例类
 * @author jiangchunzhi
 * 
 */
public class Sentinel {

	private SentinelClient client;

	public Sentinel(final String host, final int port) {
		client = new SentinelClient(host, port);
	}

	/**
	 * <pre>
	 * redis 127.0.0.1:26381> sentinel get-master-addr-by-name mymaster
	 * 1) "127.0.0.1"
	 * 2) "6379"
	 * </pre>
	 * 
	 * @param masterName
	 * @return two elements list of strings : host and port.
	 */
	public List<String> getMasterIp(String sentinelMonitor) {
		checkIsInMulti();
		client.getMasterIp(sentinelMonitor);
		return client.getMultiBulkReply();
	}

	/**
	 * 获取指定master的从服务器 127.0.0.1:26379> sentinel slaves mymaster1
	 * 
	 * @param sentinelMonitor
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getSlaves(String sentinelMonitor) {
		checkIsInMulti();
		// 发送获取slave命令
		client.getSlaves(sentinelMonitor);
		// 获取slave数据
		List<Object> list = client.getObjectMultiBulkReply();
		List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
		for (Object obj : list) {
			Map<String, String> mSlave = new HashMap<String, String>();
			List<byte[]> slave = (List<byte[]>) obj;
			for (int i = 0; i < slave.size() / 2; i++) {
				mSlave.put(new String(slave.get(i * 2)), new String(slave.get(i * 2 + 1)));
			}
			ret.add(mSlave);
		}
		return ret;
	}
	
	/**
	 * 获取存活状态的slave
	 * @param sentinelMonitor
	 * @return
	 */
	public List<Map<String, String>> getSlavesAlive(String sentinelMonitor) {
		List<Map<String, String>> slaveList = this.getSlaves(sentinelMonitor);
		List<Map<String, String>> ret = new LinkedList<Map<String,String>>();
		
		for(Map<String, String> slave : slaveList) {
			String flags = slave.get("flags");
			//过滤掉非连接状态slave
			if(flags.contains("s_down") || flags.contains("disconnected")) continue;
			ret.add(slave);
		}
		
		return ret;
	}
		
	
	/**
	 * 从sentinel获取所有master redis信息,
	 * 127.0.0.1:26379> sentinel masters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getAllMasters() {
		checkIsInMulti();
		this.client.getMasters();
		List<Object> list = this.client.getObjectMultiBulkReply();
		List<Map<String, String>> ret = new ArrayList<Map<String,String>>();
		for (Object obj : list) {
			Map<String, String> mSlave = new HashMap<String, String>();
			List<byte[]> slave = (List<byte[]>) obj;
			for (int i = 0; i < slave.size() / 2; i++) {
				mSlave.put(new String(slave.get(i * 2)), new String(slave.get(i * 2 + 1)));
			}
			ret.add(mSlave);
		}
		return ret;
	}
	
	/**
	 * 获取所有的master的ip:port信息
	 * @return
	 */
	public Set<String> getAllMastersServerIpPort() {
		Set<String> serverSet = new HashSet<String>();
		//首先获取所有master信息
		List<Map<String, String>> masterList = this.getAllMasters();
		//遍历master集合
		for(Map<String, String> master : masterList) {
			String server = master.get("ip") + ":" + master.get("port");
			serverSet.add(server);
		}
		return serverSet;
	}
	
	public Set<String> getAllMastersName() {
		Set<String> serverSet = new HashSet<String>();
		//首先获取所有master信息
		List<Map<String, String>> masterList = this.getAllMasters();
		//遍历master集合
		for(Map<String, String> master : masterList) {
			String server = master.get("name");
			serverSet.add(server);
		}
		return serverSet;
	}
	
	/**
	 * 获取指定master的所有slave
	 * @param masterName
	 * @return
	 */
	public List<String> getSlavesName(String masterName) {
		List<String> serverList = new ArrayList<String>();
		//首先获取所有master信息
		List<Map<String, String>> slaveList = this.getSlavesAlive(masterName);
		//遍历master集合
		for(Map<String, String> slave : slaveList) {
			String server = slave.get("name");
			serverList.add(server);
		}
		return serverList;
	}

	/**
	 * <pre>
	 * redis 127.0.0.1:26381> sentinel reset mymaster
	 * (integer) 1
	 * </pre>
	 * 
	 * @param pattern
	 * @return
	 */
	public Long reset(String pattern) {
		checkIsInMulti();
		client.sentinelReset(pattern);
		return client.getIntegerReply();
	}

	protected void checkIsInMulti() {
		if (client.isInMulti()) {
			throw new JedisDataException(
					"Cannot use Sentinel when in Multi. Please use JedisTransaction instead.");
		}
	}

	public Object bytes2Object(byte[] objBytes) throws Exception {
		if (objBytes == null || objBytes.length == 0) {
			return null;
		}
		ByteArrayInputStream bi = new ByteArrayInputStream(objBytes);
		ObjectInputStream oi = new ObjectInputStream(bi);
		Object obj = oi.readObject();
		bi.close();
		oi.close();
		return obj;
	}
	
	public void close() {
		this.client.disconnect();
	}

	public static void main(String args[]) {
		Sentinel sentinel = new Sentinel("10.5.18.62", 26379);
		try {
			List<Map<String, String>> masterList = sentinel.getAllMasters();
			for(Map<String, String> master : masterList) {
				String masterName = master.get("name");
				System.out.println("master=" + masterName);
				List<Map<String, String>> slaveList = sentinel.getSlavesAlive(masterName);
				for(Map<String, String> slave : slaveList) {
					String slaveName = slave.get("name");
					System.out.println("master=" + masterName + ", slave=" + slaveName);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.print(e.getMessage());
		}
	}

}
