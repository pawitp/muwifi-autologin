package org.dyndns.pawitp.muwifiautologin;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Base64;
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
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MySSLSocketFactory extends SSLSocketFactory {

    private static final String TAG = "MySSLSocketFactory";
    private static final String ADD_CIPHER_SUITE = "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA";

    private SSLContext mSslContext = SSLContext.getInstance("TLS");

    public MySSLSocketFactory(KeyStore truststore, final byte[][] trustedDers) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super(truststore);

        // Basically a "trust-all" trust manager
        // (except if "trustedDer" is set, it will only trust that certificate)
        // Cisco-based system uses an invalid certificate
        // Aruba-based system uses *.mahidol.ac.th wildcard certificate and so this class should not be used.
        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                if (trustedDers != null) {
                    // If trustedDer is set, we pin to only trust this certificate
                    if (chain.length == 0) {
                        throw new CertificateException("No certificate chain provided");
                    } else if (!checkTrustedDers(chain[0].getEncoded(), trustedDers)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            dumpInfo(chain[0]);
                        }
                        throw new CertificateException("Certificate does not match pinned certificate " + chain[0]);
                    }
                }
            }

            private boolean checkTrustedDers(byte[] encoded, byte[][] trustedDers) {
                for (byte[] der : trustedDers) {
                    if (Arrays.equals(encoded, der)) {
                        return true;
                    }
                }
                return false;
            }

            @TargetApi(Build.VERSION_CODES.FROYO)
            private void dumpInfo(X509Certificate cer) throws CertificateException {
                Log.e(TAG, "Certificate: " + Base64.encodeToString(cer.getEncoded(), Base64.DEFAULT));
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        mSslContext.init(null, new TrustManager[] { tm }, null);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        SSLSocket ss = (SSLSocket) mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);

        // Lollipop disables TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA used in
        // Aruba networks, so we must re-enable it
        String[] suites = ss.getEnabledCipherSuites();
        boolean foundSuite = false;
        for (int i = 0; i < suites.length; i++) {
            if (ADD_CIPHER_SUITE.equals(suites[i])) {
                foundSuite = true;
                break;
            }
        }

        if (!foundSuite) {
            Log.v(TAG, "Adding cipher suite " + ADD_CIPHER_SUITE);
            String[] newSuites = new String[suites.length + 1];
            for (int i = 0; i < suites.length; i++) {
                newSuites[i] = suites[i];
            }
            newSuites[suites.length] = ADD_CIPHER_SUITE;
            ss.setEnabledCipherSuites(newSuites);
        }

        return ss;
    }

    @Override
    public Socket createSocket() throws IOException {
        return mSslContext.getSocketFactory().createSocket();
    }
}