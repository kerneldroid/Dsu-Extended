package android.gsi;

import android.os.IInterface;
import android.os.RemoteException;

public interface IProgressCallback extends IInterface {
    void onProgress(long current, long total) throws RemoteException;
}
