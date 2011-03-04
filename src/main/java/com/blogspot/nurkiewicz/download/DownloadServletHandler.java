package com.blogspot.nurkiewicz.download;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:21
 */
@Service
public class DownloadServletHandler implements HttpRequestHandler {

	private static final Logger log = LoggerFactory.getLogger(DownloadServletHandler.class);

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Serving: {}", request.getRequestURI());
//		final File file = new File(req.getRequestURI());
		final File file = new File("/tmp/jsp-2.1-6.1.9.jar");
		final BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		try {
			response.setContentLength((int) file.length());
			IOUtils.copy(input, response.getOutputStream());
			log.debug("Done");
		} finally {
			input.close();
		}

	}
}
