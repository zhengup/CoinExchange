package org.zheng.redis;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zheng.util.ClassPathUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

@Component
public class RedisService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final RedisClient redisClient;

    //连接池
    final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;

    public RedisService(@Autowired RedisConfiguration redisConfig) {
        RedisURI uri = RedisURI.Builder.redis(redisConfig.getHost(), redisConfig.getPort())
                .withPassword(redisConfig.getPassword().toCharArray()).withDatabase(redisConfig.getDatabase()).build();
        this.redisClient = RedisClient.create(uri);

        //连接池参数
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.redisConnectionPool = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect,
                poolConfig);
    }

    //Bean销毁前执行
    @PreDestroy
    public void shutdown() {
        this.redisConnectionPool.close();
        this.redisClient.shutdown();
    }

    public String loadScriptFromClassPath(String classpathFile) {
        String sha = executeSync(commands -> {
            try {
                return commands.scriptLoad(ClassPathUtil.readFile(classpathFile));
            } catch (IOException e) {
                throw new UncheckedIOException("load file from classpath failed: " + classpathFile, e);
            }
        });
        if (logger.isInfoEnabled()) {
            logger.info("loaded script {} from {}.", sha, classpathFile);
        }
        return sha;
    }

    String loadScript(String scriptContent) {
        return executeSync(commands -> {
            return commands.scriptLoad(scriptContent);
        });
    }

    //订阅指定频道 并监听
    public void subscribe(String channel, Consumer<String> listener) {
        StatefulRedisPubSubConnection<String, String> conn = this.redisClient.connectPubSub();
        conn.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                listener.accept(message);
            }
        });
        conn.sync().subscribe(channel);
    }

    public Boolean executeScriptReturnBoolean(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.BOOLEAN, keys, values);
        });
    }

    public String executeScriptReturnString(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.VALUE, keys, values);
        });
    }

    public String get(String key) {
        return executeSync((commands) -> {
            return commands.get(key);
        });
    }

    //发布到指定频道
    public void publish(String topic, String data) {
        executeSync((commands) -> {
            return commands.publish(topic, data);
        });
    }

    public List<String> lrange(String key, long start, long end) {
        return executeSync((commands) -> {
            return commands.lrange(key, start, end);
        });
    }

    public List<String> zrangebyscore(String key, long start, long end) {
        return executeSync((commands) -> {
            return commands.zrangebyscore(key, Range.create(start, end));
        });
    }

    public <T> T executeSync(SyncCommandCallback<T> callback) {
        try (StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {
            connection.setAutoFlushCommands(true);
            RedisCommands<String, String> commands = connection.sync();
            return callback.doInConnection(commands);
        } catch (Exception e) {
            logger.warn("executeSync redis failed.", e);
            throw new RuntimeException(e);
        }
    }
}
