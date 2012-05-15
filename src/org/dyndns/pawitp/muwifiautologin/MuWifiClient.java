package org.dyndns.pawitp.muwifiautologin;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

public class MuWifiClient {

    // These are not regex
    static final String LOGIN_SUCCESSFUL_PATTERN = "External Welcome Page";
    static final String LOGOUT_SUCCESSFUL_PATTERN = "Logout Successful";

    static final String FORM_USERNAME = "user";
    static final String FORM_PASSWORD = "password";
    static final String FORM_URL = "https://securelogin.arubanetworks.com/auth/index.html/u";
    static final String LOGOUT_URL = "https://securelogin.arubanetworks.com/auth/logout.html";
    static final int CONNECTION_TIMEOUT = 2000;
    static final int SOCKET_TIMEOUT = 2000;
    static final int RETRY_COUNT = 2;

    private String mUsername;
    private String mPassword;
    private DefaultHttpClient mHttpClient;

    public MuWifiClient(String username, String password) {
        mUsername = username;
        mPassword = password;

        mHttpClient = new DefaultHttpClient();
        HttpParams params = mHttpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

        // Also retry POST requests (normally not retried because it is not regarded idempotent)
        mHttpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount,
                    HttpContext context) {
                if (executionCount >= RETRY_COUNT) {
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    // Unknown host
                    return false;
                }
                if (exception instanceof ConnectException) {
                    // Connection refused 
                    return false;
                }
                if (exception instanceof SSLHandshakeException) {
                    // SSL handshake exception
                    return false;
                }

                return true;
            }
        });
    }

    public boolean loginRequired() throws IOException {
        try {
            HttpGet httpget = new HttpGet("https://www.google.com/");
            mHttpClient.execute(httpget);
        }
        catch (SSLException e) {
            return true; // If login is required, the certificate sent will be securelogin.arubanetworks.com
        }
        return false;
    }

    public void login() throws IOException, LoginException {
        try {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair(FORM_USERNAME, mUsername));
            formparams.add(new BasicNameValuePair(FORM_PASSWORD, mPassword));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            HttpPost httppost = new HttpPost(FORM_URL);
            httppost.setEntity(entity);
            HttpResponse response = mHttpClient.execute(httppost);
            String strRes = EntityUtils.toString(response.getEntity());

            if (strRes.contains(LOGIN_SUCCESSFUL_PATTERN)) {
                // login successful
            } else {
                throw new LoginException("Unexpected reply from server.");
            }
        } catch (ClientProtocolException e) {
            // If there is an error, the server will send an illegal reply (containing space)
            if (e.getCause() == null || e.getCause().getCause() == null ||
                    !(e.getCause().getCause() instanceof URISyntaxException)) {
                throw new LoginException("Unknown error.");
            }
            URISyntaxException orig = (URISyntaxException) e.getCause().getCause();
            String url = orig.getInput();
            String message = url.substring(url.indexOf("errmsg=") + "errmsg=".length());
            throw new LoginException(message);
        }
    }

    public void logout() throws IOException, LoginException {
        HttpGet httpget = new HttpGet(LOGOUT_URL);
        HttpResponse response = mHttpClient.execute(httpget);
        String strRes = EntityUtils.toString(response.getEntity());

        if (strRes.contains(LOGOUT_SUCCESSFUL_PATTERN)) {
            // logout successful
        } else {
            // TODO
            throw new LoginException("Unexpected reply from server.");
        }
    }

}
