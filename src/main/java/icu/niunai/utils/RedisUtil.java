package icu.niunai.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * <p>
 * 声明: 此工具只简单包装了redisTemplate的大部分常用的api, 没有包装redisTemplate所有的api。
 * 如果对此工具类中的功能不太满意, 或对StringRedisTemplate提供的api不太满意，
 * 那么可自行实现相应的{@link StringRedisTemplate}类中的对应execute方法, 以达
 * 到自己想要的效果; 至于如何实现,则可参考源码或@link LockOps中的方法。
 * <p>
 * 注: 此工具类依赖spring-boot-starter-data-redis类库、以及可选的lombok、fastjson
 * 注: 更多javadoc细节，可详见{@link RedisOperations}
 * <p>
 * 统一说明一: 方法中的key、 value都不能为null。
 * 统一说明二: 不能跨数据类型进行操作， 否者会操作失败/操作报错。
 * 如: 向一个String类型的做Hash操作，会失败/报错......等等
 *
 * @author JustryDeng
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class RedisUtil implements ApplicationContextAware {

    /**
     * 使用StringRedisTemplate(，其是RedisTemplate的定制化升级)
     */
    private static StringRedisTemplate redisTemplate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RedisUtil.redisTemplate = applicationContext.getBean(StringRedisTemplate.class);
    }

    /**
     * 根据key，获取到对应的value值
     *
     * @param key key-value对应的key
     * @return 该key对应的值。
     * 注: 若key不存在， 则返回null。
     */
    public static String get(String key) {
        log.info("get(...) => key -> {}", key);
        String result = redisTemplate.opsForValue().get(key);
        log.info("get(...) => result -> {} ", result);
        return result;
    }

    /**
     * 设置key-value
     * <p>
     * 注: 若已存在相同的key, 那么原来的key-value会被丢弃
     *
     * @param key     key
     * @param value   key对应的value
     * @param timeout 过时时长
     * @param unit    timeout的单位
     */
    public static void setEx(String key, String value, long timeout, TimeUnit unit) {
        log.info("setEx(...) => key -> {}, value -> {}, timeout -> {}, unit -> {}",
                key, value, timeout, unit);
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }


    /**
     * redis中是否存在,指定key的key-value
     *
     * @param key 指定的key
     * @return 是否存在对应的key-value
     */
    public static boolean hasKey(String key) {
        log.info("hasKey(...) => key -> {}", key);
        Boolean result = redisTemplate.hasKey(key);
        log.info("hasKey(...) => result -> {}", result);
        if (result == null) {
            throw new RedisOpsResultIsNullException();
        }
        return result;
    }


    /**
     * 给指定的key对应的key-value设置: 多久过时
     * <p>
     * 注:过时后，redis会自动删除对应的key-value。
     * 注:若key不存在，那么也会返回false。
     *
     * @param key     指定的key
     * @param timeout 过时时间
     * @param unit    timeout的单位
     */
    public static void expire(String key, long timeout, TimeUnit unit) {
        log.info("expire(...) => key -> {}, timeout -> {}, unit -> {}", key, timeout, unit);
        Boolean result = redisTemplate.expire(key, timeout, unit);
        log.info("expire(...) => result is -> {}", result);
        if (result == null) {
            throw new RedisOpsResultIsNullException();
        }
    }


    /**
     * 当使用Pipeline 或 Transaction操作redis时, (不论redis中实际操作是否成功, 这里)结果(都)会返回null。
     * 此时，如果试着将null转换为基本类型的数据时，会抛出此异常。
     * <p>
     * 即: 此工具类中的某些方法, 希望不要使用Pipeline或Transaction操作redis。
     * <p>
     * 注: Pipeline 或 Transaction默认是不启用的， 可详见源码:
     *
     * @author JustryDeng
     */
    public static class RedisOpsResultIsNullException extends NullPointerException {

        public RedisOpsResultIsNullException() {
            super();
        }

        public RedisOpsResultIsNullException(String message) {
            super(message);
        }
    }

}