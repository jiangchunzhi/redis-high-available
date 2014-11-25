package com.bj58.xxzl.hydata.utility.cache.redis.sentinels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

/**
 * 通信协议封装
 * @author jiangchunzhi
 *
 */
public class SentinelProtocol {
	public static final int DEFAULT_PORT = 6379;
	public static final int DEFAULT_TIMEOUT = 2000;
	public static final int DEFAULT_DATABASE = 0;

	public static final String CHARSET = "UTF-8";

	public static final byte DOLLAR_BYTE = '$';
	public static final byte ASTERISK_BYTE = '*';
	public static final byte PLUS_BYTE = '+';
	public static final byte MINUS_BYTE = '-';
	public static final byte COLON_BYTE = ':';

	private SentinelProtocol() {}

	public static void sendCommand(final RedisOutputStream os,
			final Command command, final byte[]... args) {
		sendCommand(os, command.raw, args);
	}

	/**
	 * 发送命令最底层实现，格式如下：
	 *  *< 参数数量> CR LF
		$< 参数1 的字节数量> CR LF
		< 参数1 的数据> CR LF
		...
		$< 参数N 的字节数量> CR LF
		< 参数N 的数据> CR LF
	 * @param os
	 * @param command
	 * @param args
	 */
	private static void sendCommand(final RedisOutputStream os,
			final byte[] command, final byte[]... args) {
		try {
			//星号
			os.write(ASTERISK_BYTE);
			//参数数量，args的长度加1（command）
			os.writeIntCrLf(args.length + 1);
			//命令的前缀字符，$
			os.write(DOLLAR_BYTE);
			//命令的长度
			os.writeIntCrLf(command.length);
			//命令内容
			os.write(command);
			os.writeCrLf();

			for (final byte[] arg : args) {
				//命令参数的前缀
				os.write(DOLLAR_BYTE);
				//命令参数的字节数
				os.writeIntCrLf(arg.length);
				//命令字节
				os.write(arg);
				os.writeCrLf();
			}
		} catch (IOException e) {
			throw new JedisConnectionException(e);
		}
	}

	/**
	 * 处理异常
	 * @param is
	 */
	private static void processError(final RedisInputStream is) {
		String message = is.readLine();
		throw new JedisDataException(message);
	}

	/**
	 * 处理命令回复
	 *  通过检查服务器发回数据的第一个字节，可以确定这个回复是什么类型：
		• 状态回复（status reply）的第一个字节是"+"
		• 错误回复（error reply）的第一个字节是"-"
		• 整数回复（integer reply）的第一个字节是":"
		• 批量回复（bulk reply）的第一个字节是"$"
		• 多条批量回复（multi bulk reply）的第一个字节是"*"
	 * @param is
	 * @return
	 */
	private static Object process(final RedisInputStream is) {
		try {
			byte b = is.readByte();
			if (b == MINUS_BYTE) {
				processError(is);
			} else if (b == ASTERISK_BYTE) {
				return processMultiBulkReply(is);
			} else if (b == COLON_BYTE) {
				return processInteger(is);
			} else if (b == DOLLAR_BYTE) {
				return processBulkReply(is);
			} else if (b == PLUS_BYTE) {
				return processStatusCodeReply(is);
			} else {
				throw new JedisConnectionException("Unknown reply: " + (char) b);
			}
		} catch (IOException e) {
			throw new JedisConnectionException(e);
		}
		return null;
	}

	/**
	 * 处理状态回复
	 * @param is
	 * @return
	 */
	private static byte[] processStatusCodeReply(final RedisInputStream is) {
		return SafeEncoder.encode(is.readLine());
	}

	/**
	 * 处理批量回复
	 * @param is
	 * @return
	 */
	private static byte[] processBulkReply(final RedisInputStream is) {
		int len = Integer.parseInt(is.readLine());
		if (len == -1) {
			return null;
		}
		byte[] read = new byte[len];
		int offset = 0;
		try {
			while (offset < len) {
				offset += is.read(read, offset, (len - offset));
			}
			// read 2 more bytes for the command delimiter
			is.readByte();
			is.readByte();
		} catch (IOException e) {
			throw new JedisConnectionException(e);
		}

		return read;
	}

	/**
	 * 处理整数回复
	 * @param is
	 * @return
	 */
	private static Long processInteger(final RedisInputStream is) {
		String num = is.readLine();
		return Long.valueOf(num);
	}

	/**
	 * 处理多条批量回复
	 * @param is
	 * @return
	 */
	private static List<Object> processMultiBulkReply(final RedisInputStream is) {
		int num = Integer.parseInt(is.readLine());
		if (num == -1) {
			return null;
		}
		List<Object> ret = new ArrayList<Object>(num);
		for (int i = 0; i < num; i++) {
			try {
				ret.add(process(is));
			} catch (JedisDataException e) {
				ret.add(e);
			}
		}
		return ret;
	}

	/**
	 * 读数据
	 * @param is
	 * @return
	 */
	public static Object read(final RedisInputStream is) {
		return process(is);
	}

	/**
	 * 获取指定数据的字节表示(int)
	 * @param value
	 * @return
	 */
	public static final byte[] toByteArray(final int value) {
		return SafeEncoder.encode(String.valueOf(value));
	}

	/**
	 * 获取指定数据的字节表示(long)
	 * @param value
	 * @return
	 */
	public static final byte[] toByteArray(final long value) {
		return SafeEncoder.encode(String.valueOf(value));
	}

	/**
	 * 获取指定数据的字节表示(double)
	 * @param value
	 * @return
	 */
	public static final byte[] toByteArray(final double value) {
		return SafeEncoder.encode(String.valueOf(value));
	}

	/**
	 * sentinel可以执行的命令
	 * @author jiangchunzhi
	 *
	 */
	public static enum Command {
		SENTINEL,AUTH,SELECT;

		public final byte[] raw;

		Command() {
			raw = SafeEncoder.encode(this.name());
		}
	}

}












