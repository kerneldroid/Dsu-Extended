package android.gsi;

import android.os.IInterface;
import android.os.RemoteException;

public interface IGsiServiceCallback extends IInterface {
    void onFinished(int status) throws RemoteException;
}
