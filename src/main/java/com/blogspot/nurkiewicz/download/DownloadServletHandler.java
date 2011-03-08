package com.blogspot.nurkiewicz.download;

import com.blogspot.nurkiewicz.download.tokenbucket.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:21
 */
@Service
@ManagedResource
public class DownloadServletHandler implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(DownloadServletHandler.class);

	private static final int WORKER_THREADS = 1;

	@Resource
	private TokenBucket tokenBucket;

	private AtomicLong requestNo = new AtomicLong();

	private ExecutorService downloadWorkers;
	private final LinkedBlockingQueue<Runnable> downloadTasksQueue = new LinkedBlockingQueue<Runnable>();

	@PostConstruct
	public void startDownloadWorkerThreadPool() {
		downloadWorkers = new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS, 0L, TimeUnit.MILLISECONDS, downloadTasksQueue);
	}

	@PreDestroy
	public void shutDownDownloadWorkerThreadPool() {
		downloadWorkers.shutdownNow();
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		long curRequestNo = requestNo.incrementAndGet();
		request.setAttribute(TokenBucket.REQUEST_NO, curRequestNo);
		log.info("Serving: {} ({})", request.getRequestURI(), curRequestNo);
//		final File file = new File(req.getRequestURI());
		final File file = new File("/home/dev/tmp/ehcache-1.6.2.jar");
		final BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		response.setContentLength((int) file.length());
		final AsyncContext asyncContext = request.startAsync(request, response);
		downloadWorkers.submit(new DownloadChunkTask(asyncContext, input, curRequestNo));
	}

	private class DownloadChunkTask implements Callable<Void> {

		private final BufferedInputStream fileInputStream;
		private final byte[] buffer = new byte[TokenBucket.TOKEN_PERMIT_SIZE];
		private final AsyncContext asyncContext;
		private final long requestNo;
		private final ServletRequest request;
		private final ServletOutputStream responseOutputStream;

		public DownloadChunkTask(AsyncContext asyncContext, BufferedInputStream fileInputStream, long requestNo) throws IOException {
			this.asyncContext = asyncContext;
			this.requestNo = requestNo;
			this.request = asyncContext.getRequest();
			this.responseOutputStream = asyncContext.getResponse().getOutputStream();
			this.fileInputStream = fileInputStream;
		}

		@Override
		public Void call() throws Exception {
			try {
				if (tokenBucket.tryTake(request)) {
					sendChunkWorthOneToken();
				} else
					downloadWorkers.submit(this);
			} catch (Exception e) {
				log.error("Error while sending data chunk, aborting ("  + requestNo + ")", e);
				done();
			}
			return null;
		}

		private void sendChunkWorthOneToken() throws IOException {
			final int bytesCount = fileInputStream.read(buffer);
			responseOutputStream.write(buffer, 0, bytesCount);
			if (bytesCount < buffer.length) {
				done();
			} else {
				downloadWorkers.submit(this);
			}
		}

		private void done() throws IOException {
			fileInputStream.close();
			tokenBucket.completed(request);
			asyncContext.complete();
			log.debug("Done: ({})", requestNo);
		}
	}

	@ManagedAttribute
	public int getAwaitingChunks() {
		return downloadTasksQueue.size();
	}

}
