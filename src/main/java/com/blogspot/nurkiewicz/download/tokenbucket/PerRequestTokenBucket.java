package com.blogspot.nurkiewicz.download.tokenbucket;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Math.min;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 22:08
 */
@Service
@ManagedResource
public class PerRequestTokenBucket extends TokenBucketSupport {

	private static final Logger log = LoggerFactory.getLogger(PerRequestTokenBucket.class);

	private final ConcurrentMap<Long, Semaphore> countByRequestNo = new ConcurrentHashMap<Long, Semaphore>();

	private ScheduledExecutorService executorService;

	private volatile int maxTokens = 1000;

	private volatile int bucketFillPerSecond = 10;

	@PostConstruct
	public void startBucketFillingThread() {
		log.info("Creating startBucketFillingThread");
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(new FillBucketTask(), 0, 1000 / bucketFillPerSecond, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void stopBucketFillingThread() {
		log.info("Stopping startBucketFillingThread");
		executorService.shutdownNow();
	}

	private class FillBucketTask implements Runnable {
		@Override
		public void run() {
			final int maxToRelease = maxTokens / bucketFillPerSecond;
			log.debug("Releasing up to {} tokens, total stored counts: {}", maxToRelease, countByRequestNo.size());
			for (Map.Entry<Long, Semaphore> count : countByRequestNo.entrySet()) {
				final int releaseCount = min(maxToRelease, maxTokens - count.getValue().availablePermits());
				if (releaseCount > 0) {
					count.getValue().release(releaseCount);
					log.trace("Releasing {} tokens for request #{}", releaseCount, count.getKey());
				}
			}
		}
	}

	@Override
	public void takeBlocking(HttpServletRequest req, int howMany) throws InterruptedException {
		getCount(req).acquire(howMany);
	}

	private Semaphore getCount(HttpServletRequest req) {
		final Semaphore semaphore = countByRequestNo.get(getRequestNo(req));
		if (semaphore == null) {
			final Semaphore newSemaphore = new Semaphore(0, false);
			if (countByRequestNo.putIfAbsent(getRequestNo(req), newSemaphore) == null) {
				log.trace("Created new bucket for #{}", getRequestNo(req));
			}
			return newSemaphore;
		} else {
			return semaphore;
		}
	}

	private Long getRequestNo(HttpServletRequest req) {
		return (Long)req.getAttribute(REQUEST_NO);
	}

	@Override
	public boolean tryTake(HttpServletRequest req, int howMany) {
		return getCount(req).tryAcquire(howMany);
	}

	@Override
	public void completed(HttpServletRequest req) {
		countByRequestNo.remove(getRequestNo(req));
		log.trace("Completed #{}, destroying bucket", getRequestNo(req));
	}

	@ManagedAttribute
	public int getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		Validate.isTrue(maxTokens >= 0);
		this.maxTokens = maxTokens;
	}

	@ManagedAttribute
	public int getBucketFillPerSecond() {
		return bucketFillPerSecond;
	}

	public void setBucketFillPerSecond(int bucketFillPerSecond) {
		Validate.isTrue(maxTokens > 0);
		this.bucketFillPerSecond = bucketFillPerSecond;
	}

	@ManagedAttribute
	public int getOngoingRequests() {
		return countByRequestNo.size();
	}
}
