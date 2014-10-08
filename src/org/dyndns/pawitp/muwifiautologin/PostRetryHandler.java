package org.dyndns.pawitp.muwifiautologin;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

// Retry handler allowing the retry of POST requests
public class PostRetryHandler implements HttpRequestRetryHandler {

    static final int RETRY_COUNT = 2;

    @Override
    public boolean retryRequest(IOException exception, int executionCount,
            HttpContext context) {
        if (executionCount >= RETRY_COUNT) {
            // Do not retry if over max retry count
            return false;
        }

        return true;
    }

}
