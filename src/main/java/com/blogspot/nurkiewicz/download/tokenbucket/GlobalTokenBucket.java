package com.blogspot.nurkiewicz.download.tokenbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

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

	private Semaphore bucketSize = new Semaphore(0, false);

	private volatile int bucketCapacity = 1000;

	public static final int BUCKET_FILLS_PER_SECOND = 10;

	@Scheduled(fixedRate = 1000 / BUCKET_FILLS_PER_SECOND)
	public void fillBucket() {
		final int releaseCount = min(bucketCapacity / BUCKET_FILLS_PER_SECOND, bucketCapacity - bucketSize.availablePermits());
		if (releaseCount > 0) {
			bucketSize.release(releaseCount);
			log.debug("Released {} tokens, current {}, queued connections: {}", new Object[]{releaseCount, bucketSize.availablePermits(), bucketSize.getQueueLength()});
		}
	}

	@Override
	public void takeBlocking(int howMany) throws InterruptedException {
		bucketSize.acquire(howMany);
	}

	@Override
	public boolean tryTake(int howMany) {
		return bucketSize.tryAcquire(howMany);
	}

	@ManagedAttribute
	public int getBucketCapacity() {
		return bucketCapacity;
	}

	@ManagedAttribute
	public void setBucketCapacity(int bucketCapacity) {
		isTrue(bucketCapacity >= 0);
		this.bucketCapacity = bucketCapacity;
	}

	@ManagedAttribute
	public int getAwaitingRequests() {
		return bucketSize.getQueueLength();
	}

	@ManagedAttribute
	public int getBucketSize() {
		return bucketSize.availablePermits();
	}
}
