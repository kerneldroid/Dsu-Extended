package android.gsi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.List;

public interface IImageService extends IInterface {
    int CREATE_IMAGE_DEFAULT = 0;
    int CREATE_IMAGE_READONLY = 1;
    int CREATE_IMAGE_ZERO_FILL = 2;
    int IMAGE_OK = 0;
    int IMAGE_ERROR = 1;

    void createBackingImage(String name, long size, int flags, IProgressCallback onProgress) throws RemoteException;
    void deleteBackingImage(String name) throws RemoteException;
    void mapImageDevice(String name, int timeoutMs, MappedImage mapping) throws RemoteException;
    void unmapImageDevice(String name) throws RemoteException;
    boolean backingImageExists(String name) throws RemoteException;
    boolean isImageMapped(String name) throws RemoteException;
    int getAvbPublicKey(String name, AvbPublicKey dst) throws RemoteException;
    List<String> getAllBackingImages() throws RemoteException;

    abstract class Stub extends Binder implements IImageService {
        public static IImageService asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
