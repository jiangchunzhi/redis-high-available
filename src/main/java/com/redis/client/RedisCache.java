package com.redis.client;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.redis.annotation.RedisRead;
import com.redis.annotation.RehashRedisRead;

import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ShardedJedis;

/**
 * Redis 指令封装接口
 * @author jiangchunzhi
 *
 */
public interface RedisCache {
	
	/**
	 * 写入一个无时限、可序列化的对象，key采用二进制形式
	 * @param key
	 * @param value
	 * @throws Exception 
	 */
	public void put(String key,Serializable value) throws Exception;
	
	/**
	 * 写入一个无时限、可序列化的对象,key使用字符串形式.
	 * @param key
	 * @param value
	 */
	public void putByStrKey(String key,String value);
	
	/**
	 * 读出一个对象，从slave中读取,重hash时使用轮询读法
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public Object get(String key) throws Exception;
	
	/**
	 * 使用字符串类型的key
	 * @param key
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public String getByStrKey(String key);
	
	/**
	 * 写入一个无时限、可序列化的对象，Redis
	 * @param key
	 * @param value
	 * @throws Exception 
	 */
	public void putToRedis(String key,Serializable  value) throws Exception;
	
	/**
	 * 写入一个有时限、可序列化的对象，Redis
	 * @param key
	 * @param value
	 * @param seconds 设定一个key的活动时间（s）
	 * @throws Exception 
	 */
	public void putToRedis(String key,Serializable  value,int seconds) throws Exception;
	
	/**
	 * 读出一个对象,Redis
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public Object getFromRedis(String key) throws Exception;
	
	/**
	 * 写入一个有时限的字符串，Redis
	 * @param key
	 * @param value
	 * @param seconds 设定一个key的活动时间（s）
	 */
	public void putStringToRedis(String key,String value,int seconds);
	
	/**
	 * 写入一个无时限的字符串，Redis
	 * @param key
	 * @param value
	 */
	public void putStringToRedis(String key,String value);
	
	/**
	 * 读出一个字符串，Redis
	 * @param key
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public String getStringFromRedis(String key);
	
	/**
	 * 写入一个无时限的字符串，Redis的List集合 ，向key这个list添加元素，在尾部/头部添加
	 * @param key
	 * @param entry
	 * @param isR true=尾部，false=头部
	 * @return 推送操作后的列表内的元素的数目
	 */
	public long putStringToRedisList(String key, String entry, boolean isR);
	
	/**
	 * 获取redis列表中指定下标的元素,对应lindex命令
	 * @param key
	 * @param index
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public String getFromRedisWithIndex(String key, long index);
	
	/**
	 * 写入一个有时限的字符串，Redis的List集合 ，向key这个list添加元素，在尾部/头部添加
	 * @param key
	 * @param entry
	 * @param isR true=尾部，false=头部
	 * @param seconds 设定一个key的活动时间（s）
	 * @return 推送操作后的列表内的元素的数目
	 */
	public long putStringToRedisList(String key, String entry, boolean isR,int seconds);
	
	/**
	 * 写入一个无时限Redis的List集合，元素值为可序列化的对象，向key这个list添加元素，在尾部/头部添加
	 * @param key
	 * @param entry
	 * @param isR true=尾部，false=头部
	 * @param seconds 设定一个key的活动时间（s）
	 * @return 推送操作后的列表内的元素的数目
	 * @throws Exception 
	 */
	public long putToRedisList(String key, Serializable entry, boolean isR) throws Exception;
	
	/**
	 * 写入一个有时限Redis的List集合，元素值为可序列化的对象， 向key这个list添加元素，在尾部/头部添加
	 * @param key
	 * @param entry
	 * @param isR true=尾部，false=头部
	 * @param seconds 设定一个key的活动时间（s）
	 * @return 推送操作后的列表内的元素的数目
	 * @throws Exception 
	 */
	public long putToRedisList(String key, Serializable entry, boolean isR,int seconds) throws Exception;
	
	/**
	 * 读出一个字符串，Redis的List集合，返回并删除名称为key的list中的首/尾元素
	 * @param key
	 * @param isR true=尾部，false=头部
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public String getStringFromRedisList(String key,boolean isR);
	
	/**
	 * 读出一个可序列化的对象，Redis的List集合，返回并删除名称为key的list中的首/尾元素
	 * @param key
	 * @param isR true=尾部，false=头部
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public Object getFromRedisList(String key,boolean isR) throws Exception;
	
	/**
	 * 读出一个字符串List，Redis的List集合，从第几个元素到第几个元素
	 * key start stop返回列表key中指定区间内的元素，区间以偏移量start和stop指定。
	 * 下标(index)参数start和stop都以0表示列表的第一个元素，以1表示列表的第二个元素，以此类推。
	 * 也可以使用负数下标，以-1表示列表的最后一个元素，-2表示列表的倒数第二个元素，以此类推。 
	 * @param key
	 * @param start 开始下标
	 * @param end 结束下标
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public List<String> getStringFromRedisList(String key,long start,long end);
	
	/**
	 * 删除一个key对应的数据
	 * @param key
	 */
	public void remove(String key);
	
	/**
	 * 检查key是否存在
	 * 
	 * @param key
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public boolean exists(String key);
	
	/**
	 * 原子操作,设置key value
	 * @param key
	 * @param value
	 * @return
	 * @throws Exception 
	 */
	public long setNx(String key, String value) throws Exception;
	
	/**
	 * 为给定key设置生命周期
	 * @param key
	 * @param seconds 生命周期 秒为单位
	 */
	public void expire(String key, int seconds);
	
	/**
	 * 写入一个无时限的Redis的Map，元素值为字符串 ，向key这个map添加元素field
	 * @param key
	 * @param field
	 * @param value
	 * @return 如果字段已经存在，update操作，返回0；如果一个新的字段，insert操作，返回1。
	 */
	public long putStringToRedisMap(String key, String field, String value);
	
	public long putStringToRedisMap(String key, String field, String value, int seconds);
	
	@RedisRead
	@RehashRedisRead
	public String getStringFromRedisMap(String key, String field);
	
	public long delStringFromRedisMap(String key, String field);
	
	/**
	 * Return all the fields and associated values in a hash.
	 * @param key
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public Map<String,String> getMapFromRedisMap(String key);
	
	//集合操作系列
	/**
	 * 向redis结合中添加数据
	 * @param key
	 * @param value
	 * @return 增加元素的个数
	 * @throws Exception 
	 */
	public long addStringToRedisSet(String key, String value) throws Exception;
	
	/**
	 * 获取redis结合的大小
	 * @param key
	 * @return 集合的大小
	 */
	@RedisRead
	@RehashRedisRead
	public long getRedisSetSize(String key);
	
	/**
	 * 从redis结合中移除指定元素
	 * @param key
	 * @param value
	 * @return 移除元素的个数
	 * @throws Exception 
	 */
	public long removeFromRedisSet(String key, String value) throws Exception;
	
	/**
	 * 在redis集合中随机获取一个元素
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public String getRandomStringFromRedisSet(String key) throws Exception;
	
	/**
	 * 获取redis集合的所有元素
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public Set<String> getAllElementsFromRedisSet(String key) throws Exception;
	
	/**
	 * 判断元素是否存在redis的集合中，如果存在，返回true，否则，返回false
	 * @param key
	 * @return
	 * @throws Exception 
	 */
	@RedisRead
	@RehashRedisRead
	public boolean isElementExistInRedisSet(String key, String value) throws Exception;
	
	/**
	 * 采用游标的方式对集合进行遍历(由于目前sscan不支持二级制操作,所以没有使用价值)
	 * @param key
	 * @param cursor
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public ScanResult<String> scanRedisSet(String key, String cursor);
	
	/**
	 * 将key所对应的值增加1
	 * @param key
	 * @return
	 */
	@RedisRead
	@RehashRedisRead
	public long increment(String key);
	
	/**
	 * 设置shard jedis连接
	 * @param jedis
	 */
	public void setShardedJedis(ShardedJedis jedis);
	
}













