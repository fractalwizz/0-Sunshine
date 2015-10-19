package com.fract.nano.williamyoung.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * The Service which allows sync adapter framework to access authenticator
 */

public class SunshineAuthenticatorService extends Service {

    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new SunshineAuthenticator(this);
    }

    // When system binds to this service to make RPC call
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}