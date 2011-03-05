package com.blogspot.nurkiewicz.download.tokenbucket;

import org.springframework.stereotype.Service;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:30
 */
@Service
public class AlwaysFullTokenBucket extends TokenBucketSupport {
	@Override
	public void takeBlocking(int howMany) throws InterruptedException {
	}

	@Override
	public boolean tryTake(int howMany) {
		return true;
	}
}
