package com.blogspot.nurkiewicz.download;

import com.blogspot.nurkiewicz.download.tokenbucket.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestHandler;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:21
 */
@Service
public class DownloadServletHandler implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(DownloadServletHandler.class);

	@Resource
	private TokenBucket tokenBucket;

	private AtomicLong requestNo = new AtomicLong();

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		long curRequestNo = requestNo.incrementAndGet();
		request.setAttribute(TokenBucket.REQUEST_NO, curRequestNo);
		log.info("Serving: {} ({})", request.getRequestURI(), curRequestNo);
//		final File file = new File(req.getRequestURI());
		final File file = new File("/tmp/jsp-2.1-6.1.9.jar");
		final BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		try {
			response.setContentLength((int) file.length());
			sendFile(request, response, input);
			log.debug("Done");
		} catch (InterruptedException e) {
			log.error("Download interrupted", e);
		} finally {
			input.close();
			tokenBucket.completed(request);
		}
	}

	private void sendFile(HttpServletRequest request, HttpServletResponse response, BufferedInputStream input) throws IOException, InterruptedException {
		byte[] buffer = new byte[TokenBucket.TOKEN_PERMIT_SIZE];
		final ServletOutputStream outputStream = response.getOutputStream();
		for (int count = input.read(buffer); count > 0; count = input.read(buffer)) {
			tokenBucket.takeBlocking(request);
			outputStream.write(buffer, 0, count);
		}
	}
}
