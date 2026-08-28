package com.dsu.extended.model

import android.os.Parcel
import android.os.Parcelable

data class DsuSystemMetadata(
    var sdkVersion: Int = 0,
    var androidVersion: String = "",
    var cpuAbi: String = "",
    var vndkVersion: String = "",
    var securityPatch: String = "",
    var buildFingerprint: String = "",
    var isTrebleCompliant: Boolean = false,
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        sdkVersion = parcel.readInt(),
        androidVersion = parcel.readString().orEmpty(),
        cpuAbi = parcel.readString().orEmpty(),
        vndkVersion = parcel.readString().orEmpty(),
        securityPatch = parcel.readString().orEmpty(),
        buildFingerprint = parcel.readString().orEmpty(),
        isTrebleCompliant = parcel.readByte().toInt() != 0,
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(sdkVersion)
        dest.writeString(androidVersion)
        dest.writeString(cpuAbi)
        dest.writeString(vndkVersion)
        dest.writeString(securityPatch)
        dest.writeString(buildFingerprint)
        dest.writeByte(if (isTrebleCompliant) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<DsuSystemMetadata> {
        override fun createFromParcel(parcel: Parcel): DsuSystemMetadata = DsuSystemMetadata(parcel)
        override fun newArray(size: Int): Array<DsuSystemMetadata?> = arrayOfNulls(size)
    }
}
