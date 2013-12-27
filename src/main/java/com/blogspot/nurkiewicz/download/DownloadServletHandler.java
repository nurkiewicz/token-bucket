package com.blogspot.nurkiewicz.download;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.HttpRequestHandler;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import static org.apache.commons.lang.time.DateUtils.MILLIS_PER_HOUR;

/**
 * @author Tomasz Nurkiewicz
 * @since 04.03.11, 20:21
 */
@Service
@ManagedResource
public class DownloadServletHandler implements HttpRequestHandler {



    private static final Logger log = LoggerFactory.getLogger(DownloadServletHandler.class);
    private static final int CHUNK_SIZE = 1024*10;
    private static final double GLOBAL_RATE = 10 * 1024;  // in kilobytes/s

    @Resource
    private ThreadPoolTaskExecutor downloadWorkersPool;

    private final RateLimiter rateLimiter = RateLimiter.create(GLOBAL_RATE);

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final File file = new File("/home/dev/tmp/ehcache-1.6.2.jar");
        final InputStream input = new FileInputStream(file);
        response.setContentLength((int) file.length());
        final AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout(MILLIS_PER_HOUR);
        final DownloadChunkTask task = new DownloadChunkTask(asyncContext, input);
        asyncContext.addListener(task);
        downloadWorkersPool.submit(task);

    }

    private class DownloadChunkTask implements Callable<Void>, AsyncListener {

        private final InputStream fileInputStream;
        private final AsyncContext ctx;
        private final byte[] buffer = new byte[CHUNK_SIZE];
        int chunkNo;

        public DownloadChunkTask(AsyncContext ctx, InputStream fileInputStream) throws IOException {
            this.ctx = ctx;
            this.fileInputStream = fileInputStream;
        }

        @Override
        public Void call() throws Exception {

            try {
                rateLimiter.acquire(CHUNK_SIZE / 1024);
                sendChunk();
            } catch (Exception e) {
                log.error("Error while sending data chunk, aborting", e);
                ctx.complete();
            }
            return null;
        }

        private void sendChunk() throws IOException {
            log.trace("Sending chunk {} ", chunkNo++);
            final int bytesCount = fileInputStream.read(buffer);
            if (bytesCount == -1){
                ctx.complete();
            } else{
                ctx.getResponse().getOutputStream().write(buffer, 0, bytesCount);
                downloadWorkersPool.submit(this);

            }
        }

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException {
            fileInputStream.close();
        }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            log.warn("Asynchronous request timeout");
            onComplete(asyncEvent);
        }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {
            log.warn("Asynchronous request error", asyncEvent.getThrowable());
            onComplete(asyncEvent);
        }

        @Override
        public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
        }
    }

    @ManagedAttribute
    public int getAwaitingChunks() {
        return downloadWorkersPool.getThreadPoolExecutor().getQueue().size();
    }

}
