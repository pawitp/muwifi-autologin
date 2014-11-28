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

// Client for MU-WiFi system running on Aruba Networks
public class CiscoClient implements LoginClient {

    private static final String TAG = "CiscoClient";

    // These are not regex
    private static final String LOGIN_FAIL_PATTERN = "<INPUT TYPE=\"hidden\" NAME=\"err_flag\" SIZE=\"16\" MAXLENGTH=\"15\" VALUE=\"1\">";
    private static final String LOGOUT_SUCCESSFUL_PATTERN = "To complete the log off process and to prevent access";

    private static final String FORM_USERNAME = "username";
    private static final String FORM_PASSWORD = "password";
    private static final String FORM_URL = "https://1.1.1.1/login.html";
    private static final String LOGOUT_URL = "https://1.1.1.1/logout.html";

    private DefaultHttpClient mHttpClient;

    public CiscoClient() {
        mHttpClient = Utils.createHttpClient(true, null);
        // TODO: Add certificate pinning
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

        if (strRes.contains(LOGIN_FAIL_PATTERN)) {
            // login fail (extracted message from server)
            throw new LoginException("The User Name and Password combination you have entered is invalid. Please try again.");
        } else {
            // login successful
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
        return false;
    }

}
