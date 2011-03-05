package com.blogspot.nurkiewicz.download.tokenbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

	@PostConstruct
	public void startBucketFillingThread() {
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.debug("Releasing {} tokens, current {}, queued connections: {}", new Object[] {maxTokens, count.availablePermits(), count.getQueueLength()});
				count.release(maxTokens - count.availablePermits());
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void stopBucketFillingThread() {
		executorService.shutdownNow();
	}

	@Override
	public void takeBlocking(HttpServletRequest req, int howMany) throws InterruptedException {
		count.acquire(howMany);
	}

	@Override
	public boolean tryTake(HttpServletRequest req, int howMany) {
		return count.tryAcquire(howMany);
	}

	@Override
	public void completed(HttpServletRequest req) {
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
}
