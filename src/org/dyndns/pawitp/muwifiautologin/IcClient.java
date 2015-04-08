package org.dyndns.pawitp.muwifiautologin;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Client for IC-WiFi system running on Cisco without SSL
public class IcClient implements LoginClient {

    private static final String TAG = "IcClient";

    // These are not regex
    private static final String LOGIN_SUCCESSFUL_PATTERN = "You can now use all our regular network services over the wireless network.";
    private static final String LOGOUT_SUCCESSFUL_PATTERN = "To complete the log off process and to prevent access";

    private static final String FORM_USERNAME = "username";
    private static final String FORM_PASSWORD = "password";
    private static final String FORM_URL = "https://icwifi.mahidol.ac.th/login.html";
    private static final String LOGOUT_URL = "https://icwifi.mahidol.ac.th/logout.html";

    private DefaultHttpClient mHttpClient;

    public IcClient() {
        mHttpClient = Utils.createHttpClient(false, null);
    }

    public void login(String username, String password) throws IOException, LoginException {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(FORM_USERNAME, username));
        formparams.add(new BasicNameValuePair(FORM_PASSWORD, password));

        // Magic values
        formparams.add(new BasicNameValuePair("buttonClicked", "4"));
        formparams.add(new BasicNameValuePair("err_flag", "0"));
        formparams.add(new BasicNameValuePair("redirect_url", ""));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        HttpPost httppost = new HttpPost(FORM_URL);
        httppost.setEntity(entity);
        HttpResponse response = mHttpClient.execute(httppost);
        String strRes = EntityUtils.toString(response.getEntity());

        Log.d(TAG, strRes);

        if (strRes.contains(LOGIN_SUCCESSFUL_PATTERN)) {
            // login successful
        } else {
            // login fail
            throw new LoginException("Incorrect username or password.");
        }
    }

    public void logout() throws IOException, LoginException {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();

        // Magic values
        formparams.add(new BasicNameValuePair("userStatus", "1"));
        formparams.add(new BasicNameValuePair("err_flag", "0"));
        formparams.add(new BasicNameValuePair("Logout", "Logout"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        HttpPost httppost = new HttpPost(LOGOUT_URL);
        httppost.setEntity(entity);
        HttpResponse response = mHttpClient.execute(httppost);
        String strRes = EntityUtils.toString(response.getEntity());

        Log.d(TAG, strRes);

        if (strRes.contains(LOGOUT_SUCCESSFUL_PATTERN)) {
            // logout successful
        } else {
            // logout fail
            throw new LoginException("Unexpected reply from server.");
        }
    }

    @Override
    public boolean allowAuto() {
        return true;
    }

}
