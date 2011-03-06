package com.blogspot.nurkiewicz.download.tokenbucket;

import javax.servlet.ServletRequest;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:01
 */
public interface TokenBucket {

	int TOKEN_PERMIT_SIZE = 1024;
	String REQUEST_NO = "REQUEST_NO";

	void takeBlocking(ServletRequest req) throws InterruptedException;
	void takeBlocking(ServletRequest req, int howMany) throws InterruptedException;

	boolean tryTake(ServletRequest req);
	boolean tryTake(ServletRequest req, int howMany);

	void completed(ServletRequest req);
}
