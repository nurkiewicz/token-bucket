package com.blogspot.nurkiewicz.download.tokenbucket;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 19:41
 */
public abstract class TokenBucketSupport implements TokenBucket {

	@Override
	public void takeBlocking(HttpServletRequest req) throws InterruptedException {
		takeBlocking(req, 1);
	}

	@Override
	public boolean tryTake(HttpServletRequest req) {
		return tryTake(req, 1);
	}

}
