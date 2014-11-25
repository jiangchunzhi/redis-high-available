package com.bj58.xxzl.hydata.utility.cache.redis.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * 分片Redis连接池factory
 * @author jiangchunzhi
 * 
 */
public class ShardedPoolManagerFactory {

	private String sentinelConfig;

	private ShardedPoolManager shardedPoolManager;

	/**
	 * 构造方法，赋值基本信息
	 * 
	 * @param failOver
	 * @param hashingAlg
	 * @param poolConfig
	 */
	public ShardedPoolManagerFactory(String sentinelConfig) {
		this.sentinelConfig = sentinelConfig;
	}

	/**
	 * 获取ShardedPoolManager实例
	 * sentinel信息之间用逗号分隔
	 * @return
	 */
	public ShardedPoolManager getShardedPoolManager() {
		//将sentinel信息封装到list中
		String[] sentinelConfigArr = this.sentinelConfig.split(",");
		List<String> sentinelConfigSet = new ArrayList<String>(Arrays.asList(sentinelConfigArr));
		
		// 创建并初始化分片redis连接池Manager
		shardedPoolManager = new ShardedPoolManager(sentinelConfigSet);
		shardedPoolManager.initialize();
		return shardedPoolManager;
	}

	public void destroy() throws Exception {
		shardedPoolManager.destory();
	}
	
	public static void main(String[] args) throws Exception {
		ShardedPoolManagerFactory factory = 
				new ShardedPoolManagerFactory("10.5.18.62:26379,10.5.18.62:26380");
		ShardedPoolManager manager = factory.getShardedPoolManager();
		for(int i=0; i<200; i++) {
			String key = "test_key_" + i;
			ShardedJedisPool pool = manager.getShardedJedisPool(key);
			ShardedJedis sj = pool.getResource();
//			sj.set(key, "1");
			System.out.println(sj.exists(key));
			pool.returnResource(sj);
			System.out.println("key=" + key);
		}
		
//		for(int i=0; i<200; i++) {
//			String key = "test_key_" + i;
//			ShardedJedisPool pool = manager.getShardedJedisPool(key);
//			ShardedJedis sj = pool.getResource();
//			sj.set(key, "1");
////			System.out.println(sj.exists(key));
//			pool.returnResource(sj);
//			System.out.println("key=" + key);
//		}
//		
//		Thread.sleep(180000);
//		
//		for(int i=0; i<200; i++) {
//			String key = "test_key_" + i;
//			ShardedJedisPool pool = manager.getShardedJedisPool(key);
//			ShardedJedis sj = pool.getResource();
////			sj.set(key, "1");
//			System.out.println(sj.exists(key));
//			pool.returnResource(sj);
//			System.out.println("key=" + key);
//		}
	}

}













