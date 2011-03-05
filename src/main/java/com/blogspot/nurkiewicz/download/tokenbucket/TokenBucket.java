package com.blogspot.nurkiewicz.download.tokenbucket;

/**
 * @author Tomasz Nurkiewicz
 * @since 03.03.11, 00:01
 */
public interface TokenBucket {

	int TOKEN_PERMIT_SIZE = 1024;

	void takeBlocking() throws InterruptedException;
	void takeBlocking(int howMany) throws InterruptedException;

	boolean tryTake();
	boolean tryTake(int howMany);

}
