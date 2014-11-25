package com.bj58.xxzl.hydata.utility.cache.redis.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import com.bj58.xxzl.hydata.utility.cache.redis.annotation.RedisRead;
import com.bj58.xxzl.hydata.utility.cache.redis.annotation.RehashRedisRead;
import com.bj58.xxzl.hydata.utility.cache.redis.jedis.JedisPool;
import com.bj58.xxzl.hydata.utility.cache.redis.jedis.ShardedPoolManager;
import com.bj58.xxzl.hydata.utility.cache.redis.jedis.ShardedPoolManagerFactory;

/**
 * 动态代理内部处理handler
 * @author jiangchunzhi
 *
 */
public class RedisCacheHandler implements InvocationHandler {
	
	private ShardedPoolManager spm;
	
	private RedisCache rc;
	
	//重Hash状态,true表示正在进行重Hash,false表示没有进行重Hash
	private boolean rehash = true;
	
	public RedisCacheHandler(String sentinelConfig) {
		//获取redis cache实现
		this.rc = new RedisCacheImpl();
		//获取sharded pool manager
		ShardedPoolManagerFactory factory = new ShardedPoolManagerFactory(sentinelConfig);
		this.spm = factory.getShardedPoolManager();
	}

	/**
	 * 约定第0位参数是key
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String key = (String)args[0];
		RehashRedisRead rrr = method.getAnnotation(RehashRedisRead.class);
		long bucket = this.spm.getBucket(key);
		//如果正在进行重Hash,并且该方法需要支持重Hash,轮询查找master
		//通过bucket获取server信息
		String server = this.spm.getConsistentBuckets().get(bucket);
		//通过server信息获取连接池
		JedisPool jedisPool = this.getShardedJedis(method, server);
		ShardedJedisPool sjp = jedisPool.getShardedJedisPool();
		ShardedJedis sj = sjp.getResource();
		//设置连接
		this.rc.setShardedJedis(sj);
		//执行实际操作
		Object obj = method.invoke(this.rc, args);
		//回收资源
		this.returnResource(sjp, sj);
		if(this.rehash && rrr != null) {
			long lastBucket = bucket;
			//循环整个Hash环,直到获取数据
			while(obj == null) {
				long nextBucket = this.spm.getNextBucket(bucket);
				System.out.println("bucket[" + bucket + "]没有发现数据，获取下一个bucket[" + nextBucket + "]");
				//当循环回到起点还没有找到数据时，返回null
				if(nextBucket == lastBucket) {
					return null;
				}
				
				server = this.spm.getConsistentBuckets().get(nextBucket);
				jedisPool = this.getShardedJedis(method, server);
				sjp = jedisPool.getShardedJedisPool();
				sj = sjp.getResource();
				this.rc.setShardedJedis(sj);
				obj = method.invoke(this.rc, args);
				this.returnResource(sjp, sj);
				bucket = nextBucket;
			}
		}
		return obj;
	}
	
	/**
	 * 根据读写分离原则选择和设置连接
	 */
	private JedisPool getShardedJedis(Method method, String masterServer) {
		RedisRead rr = method.getAnnotation(RedisRead.class);
		JedisPool jp = this.spm.getSocketPool().get(masterServer);
		//如果设置了读写分离标识
		if(rr != null) {
			List<String> slaveList = this.spm.getMasterSlaveRef().get(masterServer);
			if(slaveList != null && slaveList.size() > 0) {
				int index = new Random().nextInt(slaveList.size());
				String slave = slaveList.get(index);
				jp = this.spm.getSocketPool().get(slave);
				System.out.println("method name=" + method.getName() + 
						", master=" + masterServer + ", 选中slave=" + slave);
			}
		}
		return jp;
	}
	
	/**
	 * 将连接资源返回连接池
	 * @param pool
	 * @param sj
	 */
	private void returnResource(ShardedJedisPool pool, ShardedJedis sj) {
		try {
			if (null != pool) {
				pool.returnResource(sj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}









