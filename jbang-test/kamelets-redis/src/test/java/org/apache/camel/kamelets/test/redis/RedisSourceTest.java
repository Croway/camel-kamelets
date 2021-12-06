package org.apache.camel.kamelets.test.redis;

import org.apache.camel.kamelets.test.JbangService;
import org.apache.camel.test.infra.redis.services.RedisService;
import org.apache.camel.test.infra.redis.services.RedisServiceFactory;
import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.awaitility.Awaitility;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;

public class RedisSourceTest {

	@RegisterExtension
	static RedisService redisService = RedisServiceFactory.createService();

	@RegisterExtension
	static JbangService jbangService = new JbangService("redis-source.yaml", () -> Collections.singletonMap("redis.port", redisService.port()));

	@Test
	public void test() {
		Config config = new Config();
		config.useSingleServer().setAddress("redis://" + redisService.host() + ":" + redisService.port());

		RedissonClient redisson = Redisson.create(config);

		RTopic topic = redisson.getTopic("one");
		topic.publish("message from redisson");

		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() ->
				FileUtils.readFileToString(
						new File(JbangService.LOG_FILE), Charset.defaultCharset()).contains("message from redisson")
		);
	}
}
