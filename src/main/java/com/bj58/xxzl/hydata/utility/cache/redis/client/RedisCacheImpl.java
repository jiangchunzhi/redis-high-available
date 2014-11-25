package com.bj58.xxzl.hydata.utility.cache.redis.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ShardedJedis;

/**
 * Redis 指令封装实现
 * 
 * @author jiangchunzhi
 * 
 */
public class RedisCacheImpl implements RedisCache {

//	private static final Logger log = Logger.getLogger(RedisCacheImpl.class);
	
	private ThreadLocal<ShardedJedis> threadLocal = new ThreadLocal<ShardedJedis>();

	@Override
	public void putToRedis(String key, Serializable value, int seconds) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.set(key.getBytes(), this.object2Bytes(value));
		if (seconds > 0) {
			jedis.expire(key.getBytes(), seconds);// 设置过期时间
		}
	}

	@Override
	public void putStringToRedis(String key, String value, int seconds) {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.set(key, value);
		if (seconds > 0) {
			jedis.expire(key, seconds);// 设置过期时间
		}
	}

	@Override
	public Object getFromRedis(String key) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		byte[] obj = jedis.get(key.getBytes());
		return this.bytes2Object(obj);
	}

	@Override
	public String getStringFromRedis(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.get(key);
	}

	@Override
	public long putStringToRedisList(String key, String entry, boolean isR,
			int seconds) {
		ShardedJedis jedis = this.threadLocal.get();
		long ret = 0;
		if (isR) {
			ret = jedis.rpush(key, entry);
		} else {
			ret = jedis.lpush(key, entry);
		}
		if (seconds > 0) {
			jedis.expire(key.getBytes(), seconds);// 设置过期时间
		}
		return ret;
	}

	@Override
	public long putToRedisList(String key, Serializable entry, boolean isR,
			int seconds) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		long ret = 0;
		if (isR) {
			ret = jedis.rpush(key.getBytes(), this.object2Bytes(entry));
		} else {
			ret = jedis.lpush(key.getBytes(), this.object2Bytes(entry));
		}
		if (seconds > 0) {
			jedis.expire(key.getBytes(), seconds);// 设置过期时间
		}
		return ret;
	}

	@Override
	public long putStringToRedisMap(String key, String field, String value,
			int seconds) {
		ShardedJedis jedis = this.threadLocal.get();
		long ret = 0;
		ret = jedis.hset(key, field, value);
		if (seconds > 0) {
			jedis.expire(key.getBytes(), seconds);// 设置过期时间
		}
		return ret;
	}

	@Override
	public long delStringFromRedisMap(String key, String field) {
		ShardedJedis jedis = this.threadLocal.get();
		long ret = jedis.hdel(key, field);
		return ret;
	}

	@Override
	public String getStringFromRedisMap(String key, String field) {
		ShardedJedis jedis = this.threadLocal.get();
		String ret = jedis.hget(key, field);
		return ret;
	}

	@Override
	public Map<String, String> getMapFromRedisMap(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		Map<String, String> ret = jedis.hgetAll(key);
		return ret;
	}

	@Override
	public List<String> getStringFromRedisList(String key, long start, long end) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.lrange(key, start, end);
	}

	@Override
	public String getStringFromRedisList(String key, boolean isR) {
		ShardedJedis jedis = this.threadLocal.get();
		String ret = null;
		if (isR) {
			ret = jedis.rpop(key);
		} else {
			ret = jedis.lpop(key);
		}
		return ret;
	}

	@Override
	public Object getFromRedisList(String key, boolean isR) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		byte[] bRet = null;
		if (isR) {
			bRet = jedis.rpop(key.getBytes());
		} else {
			bRet = jedis.lpop(key.getBytes());
		}
		Object ret = this.bytes2Object(bRet);
		return ret;
	}

	@Override
	public void remove(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.del(key);
	}

	@Override
	public boolean exists(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		boolean bool = jedis.exists(key);
		return bool;
	}

	@Override
	public void expire(String key, int seconds) {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.expire(key, seconds);
	}

	@Override
	public void put(String key, Serializable value) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.set(key.getBytes(), this.object2Bytes(value));
	}

	@Override
	public Object get(String key) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		byte[] obj = jedis.get(key.getBytes());

		Object _obj = this.bytes2Object(obj);
		return _obj;
	}

	private byte[] object2Bytes(Serializable obj) throws Exception {
		if (obj == null) {
			return null;
		}
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream oo = new ObjectOutputStream(bo);
		oo.writeObject(obj);
		bo.close();
		oo.close();
		return bo.toByteArray();
	}

	private Object bytes2Object(byte[] objBytes) throws Exception {
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

	@Override
	public long addStringToRedisSet(String key, String value) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.sadd(key.getBytes(), this.object2Bytes(value));
	}

	@Override
	public long getRedisSetSize(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.scard(key.getBytes());
	}

	@Override
	public long removeFromRedisSet(String key, String value) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.srem(key.getBytes(), this.object2Bytes(value));
	}

	@Override
	public String getRandomStringFromRedisSet(String key) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		byte[] arr = jedis.srandmember(key.getBytes());
		return (String) this.bytes2Object(arr);
	}

	@Override
	public Set<String> getAllElementsFromRedisSet(String key) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		Set<byte[]> byteSet = jedis.smembers(key.getBytes());
		Set<String> retSet = new HashSet<String>();
		for (byte[] bs : byteSet) {
			retSet.add((String) this.bytes2Object(bs));
		}
		return retSet;
	}

	@Override
	public boolean isElementExistInRedisSet(String key, String value) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.sismember(key.getBytes(), this.object2Bytes(value));
	}

	@Override
	public ScanResult<String> scanRedisSet(String key, String cursor) {
		ShardedJedis jedis = this.threadLocal.get();
		ScanResult<String> sr = jedis.sscan(key, cursor);
		return sr;
	}

	@Override
	public String getFromRedisWithIndex(String key, long index) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.lindex(key, index);
	}

	@Override
	public long setNx(String key, String value) throws Exception {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.setnx(key.getBytes(), this.object2Bytes(value));
	}

	@Override
	public long increment(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.incr(key);
	}

	@Override
	public String getByStrKey(String key) {
		ShardedJedis jedis = this.threadLocal.get();
		return jedis.get(key);
	}

	@Override
	public void putByStrKey(String key, String value) {
		ShardedJedis jedis = this.threadLocal.get();
		jedis.set(key, value);
	}

	@Override
	public void putToRedis(String key, Serializable value) throws Exception {
		this.putToRedis(key, value, 0);
	}

	@Override
	public void putStringToRedis(String key, String value) {
		this.putStringToRedis(key, value, 0);
	}

	@Override
	public long putStringToRedisList(String key, String entry, boolean isR) {
		return this.putStringToRedisList(key, entry, isR, 0);
	}

	@Override
	public long putToRedisList(String key, Serializable entry, boolean isR) throws Exception {
		return this.putToRedisList(key, entry, isR, 0);
	}

	@Override
	public long putStringToRedisMap(String key, String field, String value) {
		return this.putStringToRedisMap(key, field, value, 0);
	}
	
	@Override
	public void setShardedJedis(ShardedJedis jedis) {
		this.threadLocal.set(jedis);
	}

}












