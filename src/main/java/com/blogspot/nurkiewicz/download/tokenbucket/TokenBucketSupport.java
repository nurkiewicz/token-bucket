package com.blogspot.nurkiewicz.download.tokenbucket;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 19:41
 */
public abstract class TokenBucketSupport implements TokenBucket {

	@Override
	public void takeBlocking() throws InterruptedException {
		takeBlocking(1);
	}

	@Override
	public boolean tryTake() {
		return tryTake(1);
	}

}
