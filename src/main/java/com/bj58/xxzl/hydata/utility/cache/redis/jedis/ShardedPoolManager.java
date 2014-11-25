package com.bj58.xxzl.hydata.utility.cache.redis.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import redis.clients.jedis.ShardedJedisPool;

import com.bj58.xxzl.hydata.utility.cache.redis.sentinels.Sentinel;
import com.bj58.xxzl.hydata.utility.cache.redis.sentinels.SentinelHeartKeeper;
import com.bj58.xxzl.hydata.utility.cache.redis.util.HashAlgorithm;

/**
 * jedis pool 管理类，shardding实现
 * @author jiangchunzhi
 * 
 */
public class ShardedPoolManager {
	
	private static final Logger log = Logger.getLogger(ShardedPoolManager.class);

	//key为hash值，value为master name
	private TreeMap<Long, String> consistentBuckets;

	public TreeMap<Long, String> getConsistentBuckets() {
		return consistentBuckets;
	}

	//slave以ip:port为key,master以master name为key，Jedis连接池为value
	private Map<String, JedisPool> socketPool;

	public Map<String, JedisPool> getSocketPool() {
		return socketPool;
	}

	boolean initialized = false;

	private List<String> sentinelConfigList;
	
	public List<String> getSentinelConfigList() {
		return sentinelConfigList;
	}

	//存放master和slave的对应关系，master采用master name，slave采用ip:port
	private Map<String, List<String>> masterSlaveRef;

	public Map<String, List<String>> getMasterSlaveRef() {
		return masterSlaveRef;
	}
	
	//master名称和ip:port信息的映射
	private Map<String, String> masterNameServerRef;

	public Map<String, String> getMasterNameServerRef() {
		return masterNameServerRef;
	}

	/**
	 * 构造方法，赋值
	 * @param confList
	 * @param failOver
	 * @param hashingAlg
	 */
	public ShardedPoolManager(List<String> sentinelConfigList) {
		this.sentinelConfigList = sentinelConfigList;
		log.info("++++++++ sentinelConfigList : " + sentinelConfigList);
	}

	/**
	 * 初始化分片redis连接池Manager
	 */
	public void initialize() {
		//获取sentinel client,连接时首先使用第一个配置项
		String[] serverPort = this.sentinelConfigList.get(0).split(",")[0].split(":");
		Sentinel sentinel = new Sentinel(serverPort[0], Integer.parseInt(serverPort[1]));
		
		//通过sentinel获取所有master信息
		List<Map<String, String>> masterList = sentinel.getAllMasters();
		this.masterSlaveRef = new HashMap<String, List<String>>();
		this.masterNameServerRef = new HashMap<String, String>();
		//保存所有master和slave的对应信息
		for(Map<String, String> master : masterList) {
			String masterName = master.get("name");
			String ipPort = master.get("ip") + ":" + master.get("port");
			//存储映射信息
			this.masterNameServerRef.put(masterName, ipPort);
			
			List<String> slaveRef = new ArrayList<String>();
			//保存对应关系,key=ip:port
			this.masterSlaveRef.put(masterName, slaveRef);
			
			
			List<Map<String, String>> slaveList = sentinel.getSlavesAlive(masterName);
			for(Map<String, String> slave : slaveList) {
				//对于salve,name=ip:port
				String slaveName = slave.get("name");
				slaveRef.add(slaveName);
			}
		}
		
		//整理一致性Hash的bucket
		populateConsistentBuckets();
		
		//对每一个redis服务建立连接池
		connectAllRedis();
		
		//启动sentinel监控
		startSentinel();
		
		//设置初始化标记
		this.initialized = true;
	}

	/**
	 * 启动sentinel监控
	 */
	private void startSentinel() {
		SentinelHeartKeeper keeper = new SentinelHeartKeeper(this);
		keeper.start();
	}

	/**
	 * 连接所有的Redis服务,并保存连接池信息
	 */
	private void connectAllRedis() {
		socketPool = new HashMap<String, JedisPool>();
		Set<Entry<String, List<String>>> redisEntry = this.masterSlaveRef.entrySet();
		for(Entry<String, List<String>> entry : redisEntry) {
			//建立并保存master的连接信息
			String master = entry.getKey();
			String masterIpPort = this.masterNameServerRef.get(master);
			JedisPool jpm = new JedisPool(masterIpPort);
			this.socketPool.put(master, jpm);
			
			//建立并保存slave的连接信息
			for(String slave : entry.getValue()) {
				JedisPool jps = new JedisPool(slave);
				this.socketPool.put(slave, jps);
			}
		}
	}

	/**
	 * 采用一致性Hash收集buckets,只针对master进行收集
	 */
	private void populateConsistentBuckets() {
		consistentBuckets = new TreeMap<Long, String>();
		Set<String> masterSet = this.masterSlaveRef.keySet();
		
		for (String master : masterSet) {
			//每一个redis master实例暂定为32个对应的虚拟节点
			for (int j=0; j<8; j++) {
				//计算server的md5值,返回字节数据长度为16
				byte[] d = HashAlgorithm.computeMd5((master + "-" + j));
				for (int h=0; h<4; h++) {
					//每次提取出4个字节，组成Long类型
					Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
							| ((long) (d[2 + h * 4] & 0xFF) << 16)
							| ((long) (d[1 + h * 4] & 0xFF) << 8)
							| ((long) (d[0 + h * 4] & 0xFF));

					//一致性buckets中存放计算出的long值，注意：可能存在重复的情况
					//总数不超过master个数 * 8 * 4
					consistentBuckets.put(k, master);
				}
			}

			log.info("master [" + master + "] populate consistent buckets success");
		}
		log.info("+++consistentBuckets=" + consistentBuckets.toString());
		log.info("+++ consistentBuckets.size : " + consistentBuckets.size());
	}

	/**
	 * 根据传入的key值计算jedis连接池
	 * @param key
	 * @return
	 */
	public ShardedJedisPool getShardedJedisPool(String key) {
		if (!this.initialized) {
			log.error("attempting to get SockIO from uninitialized pool!");
			return null;
		}

		//通过Hash算法计算key所对应的bucket
		long bucket = getBucket(key);
		//获取根据bucket获取server信息
		String server = this.consistentBuckets.get(bucket);
		log.info("bucket=" + bucket + ", server=" + server);
		
		JedisPool jedisPool = socketPool.get(server);
		if (jedisPool != null) {
			return jedisPool.getShardedJedisPool();
		}
		return null;
	}

	/**
	 * 根据key获取buckets
	 * @param key
	 * @param hashCode
	 * @return
	 */
	public final long getBucket(String key) {
		long hc = getHash(key);
		//根据一致性Hash算法计算bucket位置
		return findPointFor(hc);
	}
	
	/**
	 * 获得当前key对应的bucket的下一个bucket
	 * @param key
	 * @param currentBucket
	 * @return
	 */
	public final long getNextBucket(long currentBucket) {
		return findNextPointFor(currentBucket);
	}

	private long findNextPointFor(long currentBucket) {
		//获取大于等于hv的所有元素的映射map
		SortedMap<Long, String> tmap = this.consistentBuckets.tailMap(currentBucket);
		//如果没有取到值，说明这个值最大，最后一个bucket不能存放，按照顺时针顺序，放到第一个。
		//如果取到值，获取返回的map的第一个key，
		long currentPoint = (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap.firstKey();
		
		//设置false表示不包含相等的节点
		tmap = this.consistentBuckets.tailMap(currentPoint, false);
		long nextPoint = (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap.firstKey();
		return nextPoint;
	}
	
	public static void main(String[] args) {
		TreeMap<Long, String> consistentBuckets = new TreeMap<Long, String>();
		consistentBuckets.put(1L, "1");
		consistentBuckets.put(11L, "11");
		consistentBuckets.put(111L, "111");
		consistentBuckets.put(1111L, "1111");
		consistentBuckets.put(11111L, "11111");
		consistentBuckets.put(111111L, "111111");
		long currentBucket = 10;
		//获取大于等于hv的所有元素的映射map
		SortedMap<Long, String> tmap = consistentBuckets.tailMap(currentBucket);
		//如果没有取到值，说明这个值最大，最后一个bucket不能存放，按照顺时针顺序，放到第一个。
		//如果取到值，获取返回的map的第一个key，
		long currentPoint = (tmap.isEmpty()) ? consistentBuckets.firstKey() : tmap.firstKey();
		System.out.println("curr=" + currentPoint);
		
		tmap = consistentBuckets.tailMap(currentPoint, false);
		long nextPoint = (tmap.isEmpty()) ? consistentBuckets.firstKey() : tmap.firstKey();
		System.out.println("next=" + nextPoint);
	}

	/**
	 * 获取指定key的hash值
	 * @param key
	 * @param hashCode
	 * @return
	 */
	private long getHash(String key) {
		return HashAlgorithm.KETAMA_HASH.hash(key);
	}

	/**
	 * 一致性Hash的情况，查找bucket下标（一致性Hash的查找算法实现）
	 * @param hv
	 * @return
	 */
	private Long findPointFor(Long hv) {
		//获取大于等于hv的所有元素的映射map
		SortedMap<Long, String> tmap = this.consistentBuckets.tailMap(hv);
		//如果没有取到值，说明这个值最大，最后一个bucket不能存放，按照顺时针顺序，放到第一个。
		//如果取到值，获取返回的map的第一个key，
		return (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap.firstKey();
	}

	/**
	 * 销毁连接信息
	 */
	public void destory() {
		if(this.masterSlaveRef != null) {
			this.masterSlaveRef.clear();
		}
		if (socketPool != null) {
			//释放连接池
			for (Entry<String, JedisPool> entryPool : socketPool.entrySet()) {
				JedisPool jedisPool = entryPool.getValue();
				if (jedisPool != null) {
					jedisPool.destory();
				}
			}
			socketPool.clear();
		}
		//释放bucket
		if (consistentBuckets != null) {
			consistentBuckets.clear();
		}
		initialized = false;
	}
	
}










