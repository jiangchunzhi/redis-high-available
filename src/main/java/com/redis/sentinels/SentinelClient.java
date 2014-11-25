package com.redis.sentinels;

import static redis.clients.jedis.Protocol.toByteArray;
import redis.clients.util.SafeEncoder;

import com.redis.sentinels.SentinelProtocol.Command;

/**
 * Sentinel 客户端封装
 * @author jiangchunzhi
 *
 */
public class SentinelClient extends SentinelConnection {
	
	public enum LIST_POSITION {
		BEFORE, AFTER;
		public final byte[] raw;

		private LIST_POSITION() {
			raw = SafeEncoder.encode(name());
		}
	}

	private boolean isInMulti;

	private String password;

	private long db;

	public boolean isInMulti() {
		return isInMulti;
	}

	public SentinelClient(final String host, final int port) {
		super(host, port);
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public void connect() {
		if (!isConnected()) {
			super.connect();
			if (password != null) {
				auth(password);
				getStatusCodeReply();
			}
			if (db > 0) {
				select(Long.valueOf(db).intValue());
				getStatusCodeReply();
			}
		}
	}

	public void auth(final String password) {
		setPassword(password);
		sendCommand(Command.AUTH, password);
	}

	public void select(final int index) {
		db = index;
		sendCommand(Command.SELECT, toByteArray(index));
	}

	public void getMasterIp(String masterName) {
		sendCommand(Command.SENTINEL,
				SafeEncoder.encode("get-master-addr-by-name"),
				SafeEncoder.encode(masterName));
	}
	
	/**
	 * 获取指定master的所有slave
	 * @param masterName
	 */
	public void getSlaves(String masterName) {
		sendCommand(Command.SENTINEL, SafeEncoder.encode("slaves"), SafeEncoder.encode(masterName));
	}
	
	/**
	 * 通过sentinel获取所有监控的master
	 */
	public void getMasters() {
		this.sendCommand(Command.SENTINEL, SafeEncoder.encode("masters"));
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
	public void sentinelReset(String pattern) {
		sendCommand(Command.SENTINEL, SafeEncoder.encode("reset"),
				SafeEncoder.encode(pattern));
	}
	
}












