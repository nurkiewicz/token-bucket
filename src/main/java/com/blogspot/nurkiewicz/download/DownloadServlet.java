package com.blogspot.nurkiewicz.download;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Tomasz Nurkiewicz
 * @since 26.02.11, 22:03
 */
@WebServlet(urlPatterns = "/*")
public class DownloadServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(DownloadServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.info("Serving: {}", req.getRequestURI());
//		final File file = new File(req.getRequestURI());
		final File file = new File("/tmp/jsp-2.1-6.1.9.jar");
		final BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		try {
			resp.setContentLength((int) file.length());
			IOUtils.copy(input, resp.getOutputStream());
			log.debug("Done");
		} finally {
			input.close();
		}
	}

}
