package com.bj58.xxzl.hydata.utility.cache.redis.sentinels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

import com.bj58.xxzl.hydata.utility.cache.redis.sentinels.SentinelProtocol.Command;

/**
 * Sentinel链接操作
 * @author jiangchunzhi
 *
 */
public class SentinelConnection {
	
	private String host;
    private int port = Protocol.DEFAULT_PORT;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    
    //管道中命令的数量
    private int pipelinedCommands = 0;
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    public Socket getSocket() {
        return socket;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public void setTimeoutInfinite() {
        try {
            socket.setKeepAlive(true);
            socket.setSoTimeout(0);
        } catch (SocketException ex) {
            throw new JedisException(ex);
        }
    }

    public void rollbackTimeout() {
        try {
            socket.setSoTimeout(timeout);
            socket.setKeepAlive(false);
        } catch (SocketException ex) {
            throw new JedisException(ex);
        }
    }

    public SentinelConnection(final String host) {
        super();
        this.host = host;
    }

    /**
     * flush output stream
     */
    protected void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }

    protected SentinelConnection sendCommand(final Command cmd, final String... args) {
        final byte[][] bargs = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            bargs[i] = SafeEncoder.encode(args[i]);
        }
        return sendCommand(cmd, bargs);
    }

    protected SentinelConnection sendCommand(final Command cmd, final byte[]... args) {
        connect();
        SentinelProtocol.sendCommand(outputStream, cmd, args);
        pipelinedCommands++;
        return this;
    }
    
    protected SentinelConnection sendCommand(final Command cmd) {
        connect();
        SentinelProtocol.sendCommand(outputStream, cmd, new byte[0][]);
        pipelinedCommands++;
        return this;
    }

    public SentinelConnection(final String host, final int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public SentinelConnection() {
    	
    }

    /**
     * 创建与redis的连接,每次都是重新连接,原因:目前sentinel不支持动态添加master,
     * 只能通过停掉sentinel,修改配置文件,启动sentinel的方式达到动态扩容,所以需要每次
     * 都重建连接.
     */
    public void connect() {
        try {
            socket = new Socket();
            
            socket.setReuseAddress(true);
            socket.setKeepAlive(true);  //Will monitor the TCP connection is valid
            socket.setTcpNoDelay(true);  //Socket buffer Whetherclosed, to ensure timely delivery of data
            socket.setSoLinger(true,0);  //Control calls close () method, the underlying socket is closed immediately

            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);
            outputStream = new RedisOutputStream(socket.getOutputStream());
            inputStream = new RedisInputStream(socket.getInputStream());
        } catch (IOException ex) {
            throw new JedisConnectionException(ex);
        }
    }

    /**
     * 断开与redis的连接
     */
    public void disconnect() {
        if (isConnected()) {
            try {
                inputStream.close();
                outputStream.close();
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                throw new JedisConnectionException(ex);
            }
        }
    }

    /**
     * 判断是否已经连接
     * @return
     */
    public boolean isConnected() {
        return socket != null && socket.isBound() && !socket.isClosed()
                && socket.isConnected() && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }

    protected String getStatusCodeReply() {
        flush();
        pipelinedCommands--;
        final byte[] resp = (byte[]) Protocol.read(inputStream);
        if (null == resp) {
            return null;
        } else {
            return SafeEncoder.encode(resp);
        }
    }

    public String getBulkReply() {
        final byte[] result = getBinaryBulkReply();
        if (null != result) {
            return SafeEncoder.encode(result);
        } else {
            return null;
        }
    }

    public byte[] getBinaryBulkReply() {
        flush();
        pipelinedCommands--;
        return (byte[]) Protocol.read(inputStream);
    }

    public Long getIntegerReply() {
        flush();
        pipelinedCommands--;
        return (Long) Protocol.read(inputStream);
    }

    public List<String> getMultiBulkReply() {
        return BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
    }

    @SuppressWarnings("unchecked")
    public List<byte[]> getBinaryMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return (List<byte[]>) Protocol.read(inputStream);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getObjectMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return (List<Object>) Protocol.read(inputStream);
    }
    
    @SuppressWarnings("unchecked")
    public List<Long> getIntegerMultiBulkReply() {
        flush();
        pipelinedCommands--;
        return (List<Long>) Protocol.read(inputStream);
    }

    public List<Object> getAll() {
        return getAll(0);
    }

    public List<Object> getAll(int except) {
        List<Object> all = new ArrayList<Object>();
        flush();
        while (pipelinedCommands > except) {
        	try{
                all.add(Protocol.read(inputStream));
        	}catch(JedisDataException e){
        		all.add(e);
        	}
            pipelinedCommands--;
        }
        return all;
    }

    public Object getOne() {
        flush();
        pipelinedCommands--;
        return Protocol.read(inputStream);
    }
    
}















