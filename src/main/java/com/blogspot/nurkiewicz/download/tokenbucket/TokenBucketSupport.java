package com.blogspot.nurkiewicz.download.tokenbucket;

import javax.servlet.ServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 19:41
 */
public abstract class TokenBucketSupport implements TokenBucket {

	@Override
	public void takeBlocking(ServletRequest req) throws InterruptedException {
		takeBlocking(req, 1);
	}

	@Override
	public boolean tryTake(ServletRequest req) {
		return tryTake(req, 1);
	}

}
