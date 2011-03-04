package com.blogspot.nurkiewicz.download.tokenbucket;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:30
 */
@Service
public class AlwaysFullTokenBucket extends TokenBucketSupport {
	@Override
	public void takeBlocking(HttpServletRequest req, int howMany) throws InterruptedException {
	}

	@Override
	public boolean tryTake(HttpServletRequest req, int howMany) {
		return true;
	}

	@Override
	public void completed(HttpServletRequest req) {
	}
}
