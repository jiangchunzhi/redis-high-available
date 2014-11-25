package com.redis.sentinels;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.redis.jedis.JedisPool;
import com.redis.jedis.ShardedPoolManager;

/**
 * Jedis心跳检测
 * @author jiangchunzhi
 * 
 */
public class SentinelHeartKeeper {

	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	
	//ShardedPoolManager实例索引，用来修改实例中的redis信息状态
	private ShardedPoolManager spm;

	public SentinelHeartKeeper(ShardedPoolManager spm) {
		this.spm = spm;
	}

	/**
	 * 启动心跳检测任务,执行周期为60秒
	 */
	public void start() {
		exec.scheduleAtFixedRate(new HeartTasks(), 10, 60, TimeUnit.SECONDS);
	}

	/**
	 * 停止心跳检测任务
	 */
	public void stop() {
		exec.shutdown();
	}

	/**
	 * 心跳线程
	 * @author jiangchunzhi
	 * 
	 */
	class HeartTasks implements Runnable {
		public void run() {
			try {
				System.out.println("sentinel heart beat task start");
				String[] serverPort = spm.getSentinelConfigList().get(0).split(",")[0].split(":");
				Sentinel sentinel = new Sentinel(serverPort[0], Integer.parseInt(serverPort[1]));
				
				//通过sentinel获取所有master信息
				Set<String> newMasterSet = sentinel.getAllMastersName();
				Set<String> oriMasterSet = spm.getMasterSlaveRef().keySet();
				if(newMasterSet.equals(oriMasterSet)) {
					//如果master没变，查看master对应的slave是否改变，因为只有slave不参加Hash，所以不重新初始化
					Set<String> masterNameSet = sentinel.getAllMastersName();
					for(String masterName : masterNameSet) {
						List<String> newSlaveSet = sentinel.getSlavesName(masterName);
						
						//获取master的ip和port
						List<String> ipPortList = sentinel.getMasterIp(masterName);
						String ipPort = ipPortList.get(0) + ":" + ipPortList.get(1);
						//设置master的name和ip:port对应关系
						spm.getMasterNameServerRef().put(masterName, ipPort);
						
						List<String> oriSlaveSet = spm.getMasterSlaveRef().get(masterName);
						if(!newSlaveSet.equals(oriSlaveSet)) {
							//--先生成连接
							//生成最新的slave连接，如果连接已经存在，跳过，如果不存在，生成，
							//如果就有slave在新的slave中不存在，删除。
							//1. 添加新的连接
							for(String newSlave : newSlaveSet) {
								if(!oriSlaveSet.contains(newSlave)) {
									JedisPool jps = new JedisPool(newSlave);
									spm.getSocketPool().put(newSlave, jps);
								}
							}
							//删除已经不存在的连接
							for(String oriSlave : oriSlaveSet) {
								if(!newSlaveSet.contains(oriSlave)) {
									JedisPool jp = spm.getSocketPool().remove(oriSlave);
									jp.destory();
								}
							}
							
							//--再添加master slave对应关系
							//如果新的slave和原有的slave不同，替换成最新的
							spm.getMasterSlaveRef().put(ipPort, newSlaveSet);
						}
					}
				} else {
					//如果master不相同，说明新增加了master，需要重新计算整个一致性hash结构，重新初始化
					spm.initialize();
				}
				sentinel.close();
				System.out.println("sentinel heart beat task end");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}












