package org.dyndns.pawitp.muwifiautologin;

import java.io.IOException;

public interface LoginClient {

    public void login(String username, String password) throws IOException, LoginException;
    public void logout() throws IOException, LoginException;
    public boolean allowAuto();

}
