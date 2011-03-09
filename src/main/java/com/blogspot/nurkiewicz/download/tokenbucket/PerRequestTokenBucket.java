package com.blogspot.nurkiewicz.download.tokenbucket;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.servlet.ServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import static java.lang.Math.min;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 22:08
 */
@Service
@ManagedResource
public class PerRequestTokenBucket extends TokenBucketSupport {

	private static final Logger log = LoggerFactory.getLogger(PerRequestTokenBucket.class);

	private final ConcurrentMap<Long, Semaphore> bucketSizeByRequestNo = new ConcurrentHashMap<Long, Semaphore>();

	private volatile int eachBucketCapacity = 200 * 1024 / TOKEN_PERMIT_SIZE;

	public static final int BUCKET_FILLS_PER_SECOND = 10;

	@Scheduled(fixedRate = 1000 / BUCKET_FILLS_PER_SECOND)
	public void fillBucket() {
		final int maxToRelease = eachBucketCapacity / BUCKET_FILLS_PER_SECOND;
		log.debug("Releasing up to {} tokens, total stored counts: {}", maxToRelease, bucketSizeByRequestNo.size());
		for (Map.Entry<Long, Semaphore> count : bucketSizeByRequestNo.entrySet()) {
			final int releaseCount = min(maxToRelease, eachBucketCapacity - count.getValue().availablePermits());
			if (releaseCount > 0) {
				count.getValue().release(releaseCount);
				log.trace("Releasing {} tokens for request #{}", releaseCount, count.getKey());
			}
		}
	}

	@Override
	public void takeBlocking(ServletRequest req, int howMany) throws InterruptedException {
		getCount(req).acquire(howMany);
	}

	private Semaphore getCount(ServletRequest req) {
		final Semaphore semaphore = bucketSizeByRequestNo.get(getRequestNo(req));
		if (semaphore == null) {
			final Semaphore newSemaphore = new Semaphore(0, false);
			if (bucketSizeByRequestNo.putIfAbsent(getRequestNo(req), newSemaphore) == null) {
				log.trace("Created new bucket for #{}", getRequestNo(req));
			}
			return newSemaphore;
		} else {
			return semaphore;
		}
	}

	private Long getRequestNo(ServletRequest req) {
		final Long reqNo = (Long) req.getAttribute(REQUEST_NO);
		if (reqNo == null) {
			throw new IllegalAccessError("Request # not found in: " + req);
		}
		return reqNo;
	}

	@Override
	public boolean tryTake(ServletRequest req, int howMany) {
		return getCount(req).tryAcquire(howMany);
	}

	@Override
	public void completed(ServletRequest req) {
		bucketSizeByRequestNo.remove(getRequestNo(req));
		log.trace("Completed #{}, destroying bucket", getRequestNo(req));
	}

	@ManagedAttribute
	public int getEachBucketCapacity() {
		return eachBucketCapacity;
	}

	@ManagedAttribute
	public void setEachBucketCapacity(int eachBucketCapacity) {
		Validate.isTrue(eachBucketCapacity >= 0);
		this.eachBucketCapacity = eachBucketCapacity;
	}

	@ManagedAttribute
	public int getOngoingRequests() {
		return bucketSizeByRequestNo.size();
	}
}
