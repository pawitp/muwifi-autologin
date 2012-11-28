package org.dyndns.pawitp.muwifiautologin;

import android.util.Log;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MySSLSocketFactory extends SSLSocketFactory {

    private static final String TAG = "MySSLSocketFactory";

    /**
     * Mode of operation in which an error is returned if the
     * user is in a captive portal.
     */
    public static final int MODE_CHECK_CAPTIVE = 0;

    /**
     * Mode of operation in which all certificates are trusted.
     */
    public static final int MODE_TRUST_ALL = 1;

    private SSLContext mSslContext = SSLContext.getInstance("TLS");

    public MySSLSocketFactory(KeyStore truststore, final int mode) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(truststore);

        // Basically a "trust-all" trust manager
        // Cisco-based system uses an invalid certificate
        // Aruba-based system uses a global certificate for securelogin.arubanetworks.com
        // which any client has access to anyway, so additional measure is useless.
        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                for (X509Certificate cer : chain) {
                    String name = cer.getSubjectDN().getName();
                    if (mode == MODE_CHECK_CAPTIVE) {
                        if (name.contains("CN=securelogin.arubanetworks.com")) {
                            throw new CertificateException("Aruba");
                        }
                        Log.d(TAG, name);
                    }
                }
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        mSslContext.init(null, new TrustManager[] { tm }, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return mSslContext.getSocketFactory().createSocket();
    }
}