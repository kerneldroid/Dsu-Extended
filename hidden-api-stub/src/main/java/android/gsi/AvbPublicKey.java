package android.gsi;

import android.os.Parcel;
import android.os.Parcelable;

public class AvbPublicKey implements Parcelable {
    public byte[] bytes;
    public byte[] sha1;

    public AvbPublicKey() {}

    private AvbPublicKey(Parcel in) {
        bytes = in.createByteArray();
        sha1 = in.createByteArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(bytes);
        dest.writeByteArray(sha1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AvbPublicKey> CREATOR =
        new Creator<AvbPublicKey>() {
            @Override
            public AvbPublicKey createFromParcel(Parcel in) {
                return new AvbPublicKey(in);
            }

            @Override
            public AvbPublicKey[] newArray(int size) {
                return new AvbPublicKey[size];
            }
        };
}
