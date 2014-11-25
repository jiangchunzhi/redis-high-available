package com.redis.jedis;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

/**
 * Jedis连接池
 * @author jiangchunzhi
 *
 */
public class JedisPool {
	
	private Logger log = Logger.getLogger(this.getClass());	
	
	private ShardedJedisPool shardedJedisPool;
	
	public ShardedJedisPool getShardedJedisPool() {
		return shardedJedisPool;
	}

	/**
	 * 创建Jedis连接池
	 * @param redisHost
	 * @return
	 */
	public JedisPool(String redisHost){
		log.info("redisHost=" + redisHost);
		
		//封装配置信息
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(5000);
		config.setMaxIdle(100);
		config.setMaxWaitMillis(5000);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		
		//封装redis server的ip和port信息
		List<JedisShardInfo> list = new LinkedList<JedisShardInfo>();
		JedisShardInfo jedisShardInfo = new JedisShardInfo(redisHost.split(":")[0], Integer.parseInt(redisHost.split(":")[1]), 2000, 5);
		list.add(jedisShardInfo);
		
		//创建连接池
		shardedJedisPool = new ShardedJedisPool(config, list);
	}
	
	/**
	 * 销毁redis连接池
	 */
	public void destory(){
		if(shardedJedisPool != null){
			shardedJedisPool.destroy();
		}
	}
	
}














