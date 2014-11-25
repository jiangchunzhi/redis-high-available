package com.bj58.xxzl.hydata.utility.cache.redis.util;

/**
 * 缓存的key相关常量
 * @author jiangchunzhi
 *
 */
public class CacheKeyUtil {
	
	//抓取数据id保存队列的key
	public static final String ZHUAQU_IDS_KEY= "ZHUAQU_IDS_LIST";
	
	//当前最大的抓取ID key
	public static final String MAX_ZHUAQU_ID = "MAX_ZHUAQU_ID";
	
	//企业库数据id保存集合
	public static final String ENTERPRISE_IDS_KEY = "ENTERPRISE_IDS_LIST";
	
	//手动添加数据id集合
	public static final String HANDWORK_IDS_KEY = "HANDWORK_IDS_KEY";
	
	//初始化行业数据id集合
	public static final String HANGYE_INIT_LIST_KEY = "HANGYE_INIT_LIST_KEY";
	
	//自去重后的行业初始化数据id
	public static final String HANGYE_INIT_LIST_AFTER_SELFDELDUP = "HANGYE_INIT_LIST_AFTER_SELFDELDUP";

	//由于所以建立并不是即时,所以需要在一段时间内,后进入的key不能执行任何操作,避免重复添加.
	public static final String REPAIR_INDEX_PREFIX = "REPAIR_INDEX_PREFIX";
	
	//三方访问量统计key(成功)
	public static final String PARTNER_STATISTIC_SUCCESS_PREFIX = "PARTNER_STATISTIC_SUCCESS_PREFIX";
	
	//三方访问量统计key(失败)
	public static final String PARTNER_STATISTIC_FAILURE_PREFIX = "PARTNER_STATISTIC_FAILURE_PREFIX";
	
	//电话黑名单前缀
	public static final String BLACKLIST_TELEPHONE_KEY = "BLACKLIST_TELEPHONE_KEY";
	
	//名称黑名单前缀
	public static final String BLACKLIST_NAME_KEY = "BLACKLIST_NAME_KEY";
	
	//合作方跳转url集合前缀
	public static final String PARTNER_REDIRECT_URL_SET_PREFIX = "PARTNER_REDIRECT_URL_SET_PREFIX";
	
	public static final String INTERFACE_DATA_COUNT_KEY_PREFIX = "OC_DATA_";
	
}










