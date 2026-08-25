package android.gsi;

import android.os.Parcel;
import android.os.Parcelable;

public class MappedImage implements Parcelable {
    public String path;

    public MappedImage() {}

    private MappedImage(Parcel in) {
        path = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MappedImage> CREATOR =
        new Creator<MappedImage>() {
            @Override
            public MappedImage createFromParcel(Parcel in) {
                return new MappedImage(in);
            }

            @Override
            public MappedImage[] newArray(int size) {
                return new MappedImage[size];
            }
        };
}
