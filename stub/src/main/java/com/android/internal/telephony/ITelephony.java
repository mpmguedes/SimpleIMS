package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;

public interface ITelephony extends android.os.IInterface {
    int setImsProvisioningInt(int subId, int key, int value);

    int getImsProvisioningInt(int subId, int key);

    abstract class Stub extends Binder implements ITelephony {
        public native static ITelephony asInterface(IBinder binder);
    }
}
