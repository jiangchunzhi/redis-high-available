package com.redis.client;

import java.lang.reflect.Proxy;

/**
 * redis cache factory
 * @author jiangchunzhi
 *
 */
public class RedisCacheFactory {
	
	public static void main(String[] args) throws Exception {
		RedisCacheHandler handler = new RedisCacheHandler("10.5.18.62:26379,10.5.18.62:26380");
		RedisCache rc = (RedisCache)Proxy.newProxyInstance(RedisCacheFactory.class.getClassLoader(), 
				new Class[]{RedisCache.class}, handler);
		rc.addStringToRedisSet("test_mul", "test_key_1991");
		System.out.println(rc.getAllElementsFromRedisSet("test_mul"));
	}

}












