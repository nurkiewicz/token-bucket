package com.blogspot.nurkiewicz.download;

import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.annotation.WebServlet;

/**
 * @author Tomasz Nurkiewicz
 * @since 26.02.11, 22:03
 */
@WebServlet(urlPatterns = "/d/*", name="downloadServletHandler", asyncSupported = true)
public class DownloadServlet extends HttpRequestHandlerServlet {

}
