package com.blogspot.nurkiewicz.download.tokenbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 22:08
 */
@Service
public class PerRequestTokenBucket extends TokenBucketSupport {

	private static final Logger log = LoggerFactory.getLogger(PerRequestTokenBucket.class);

	private final ConcurrentMap<HttpServletRequest, Semaphore> counts = new ConcurrentHashMap<HttpServletRequest, Semaphore>();

	private ScheduledExecutorService executorService;

	private int maxTokens = 1000;

	@PostConstruct
	public void startBucketFillingThread() {
		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				log.debug("Releasing {} tokens, total stored counts: {}", maxTokens, counts.size());
				for (Semaphore count : counts.values()) {
					count.release(maxTokens - count.availablePermits());
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void stopBucketFillingThread() {
		executorService.shutdownNow();
	}


	@Override
	public void takeBlocking(HttpServletRequest req, int howMany) throws InterruptedException {
		getCount(req).acquire(howMany);
	}

	private Semaphore getCount(HttpServletRequest req) {
		final Semaphore semaphore = counts.get(req);
		if (semaphore == null) {
			final Semaphore newSemaphore = new Semaphore(0, false);
			counts.putIfAbsent(req, newSemaphore);
			return newSemaphore;
		} else
			return semaphore;
	}

	@Override
	public boolean tryTake(HttpServletRequest req, int howMany) {
		return getCount(req).tryAcquire(howMany);
	}

	@Override
	public void completed(HttpServletRequest req) {
		counts.remove(req);
	}
}
