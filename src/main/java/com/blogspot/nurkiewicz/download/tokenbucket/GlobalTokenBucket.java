package com.blogspot.nurkiewicz.download.tokenbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;
import static org.apache.commons.lang.Validate.isTrue;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:02
 */
@Service
@Primary
@ManagedResource
public class GlobalTokenBucket extends TokenBucketSupport {

	private static final Logger log = LoggerFactory.getLogger(GlobalTokenBucket.class);

	private Semaphore count = new Semaphore(0, false);
	private ScheduledExecutorService executorService;

	private volatile int maxTokens = 10000;

	private volatile int bucketFillPerSecond = 10;

	@PostConstruct
	public void startBucketFillingThread() {
		log.info("Creating startBucketFillingThread");
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(new FillBucketTask(), 0, 1000 / bucketFillPerSecond, TimeUnit.MILLISECONDS);
	}

	private class FillBucketTask implements Runnable {
		@Override
		public void run() {
			final int releaseCount = min(maxTokens / bucketFillPerSecond, maxTokens - count.availablePermits());
			if (releaseCount > 0) {
				count.release(releaseCount);
				log.debug("Released {} tokens, current {}, queued connections: {}", new Object[]{releaseCount, count.availablePermits(), count.getQueueLength()});
			}
		}
	}

	@PreDestroy
	public void stopBucketFillingThread() {
		executorService.shutdownNow();
	}

	@Override
	public void takeBlocking(int howMany) throws InterruptedException {
		count.acquire(howMany);
	}

	@Override
	public boolean tryTake(int howMany) {
		return count.tryAcquire(howMany);
	}

	@ManagedAttribute
	public int getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		isTrue(maxTokens >= 0);
		this.maxTokens = maxTokens;
	}

	@ManagedAttribute
	public int getOngoingRequests() {
		return count.getQueueLength();
	}

	@ManagedAttribute
	public int getBucketFillPerSecond() {
		return bucketFillPerSecond;
	}

	public void setBucketFillPerSecond(int bucketFillPerSecond) {
		isTrue(bucketFillPerSecond > 0);
		this.bucketFillPerSecond = bucketFillPerSecond;
	}
}
