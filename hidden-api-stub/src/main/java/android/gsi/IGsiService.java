package android.gsi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.util.List;

public interface IGsiService extends IInterface {
    int STATUS_NO_OPERATION = 0;
    int STATUS_WORKING = 1;
    int STATUS_COMPLETE = 2;
    int INSTALL_OK = 0;
    int INSTALL_ERROR_GENERIC = 1;
    int INSTALL_ERROR_NO_SPACE = 2;
    int INSTALL_ERROR_FILE_SYSTEM_CLUTTERED = 3;

    boolean commitGsiChunkFromStream(ParcelFileDescriptor stream, long bytes) throws RemoteException;
    GsiProgress getInstallProgress() throws RemoteException;
    boolean setGsiAshmem(ParcelFileDescriptor stream, long size) throws RemoteException;
    boolean commitGsiChunkFromAshmem(long bytes) throws RemoteException;
    int enableGsi(boolean oneShot, String dsuSlot) throws RemoteException;
    void enableGsiAsync(boolean oneShot, String dsuSlot, IGsiServiceCallback result) throws RemoteException;
    boolean isGsiEnabled() throws RemoteException;
    boolean cancelGsiInstall() throws RemoteException;
    boolean isGsiInstallInProgress() throws RemoteException;
    boolean removeGsi() throws RemoteException;
    void removeGsiAsync(IGsiServiceCallback result) throws RemoteException;
    boolean disableGsi() throws RemoteException;
    boolean isGsiInstalled() throws RemoteException;
    boolean isGsiRunning() throws RemoteException;
    String getActiveDsuSlot() throws RemoteException;
    String getInstalledGsiImageDir() throws RemoteException;
    List<String> getInstalledDsuSlots() throws RemoteException;
    int openInstall(String installDir) throws RemoteException;
    int closeInstall() throws RemoteException;
    int createPartition(String name, long size, boolean readOnly) throws RemoteException;
    int closePartition() throws RemoteException;
    int zeroPartition(String name) throws RemoteException;
    IImageService openImageService(String prefix) throws RemoteException;

    abstract class Stub extends Binder implements IGsiService {
        public static IGsiService asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
