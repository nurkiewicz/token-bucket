package com.blogspot.nurkiewicz.download.tokenbucket;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:01
 */
public interface TokenBucket {

	int TOKEN_PERMIT_SIZE = 1024;
	String REQUEST_NO = "REQUEST_NO";

	void takeBlocking(HttpServletRequest req) throws InterruptedException;
	void takeBlocking(HttpServletRequest req, int howMany) throws InterruptedException;

	boolean tryTake(HttpServletRequest req);
	boolean tryTake(HttpServletRequest req, int howMany);

	void completed(HttpServletRequest req);
}
