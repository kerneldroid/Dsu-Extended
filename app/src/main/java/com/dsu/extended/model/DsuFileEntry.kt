package com.dsu.extended.model

import android.os.Parcel
import android.os.Parcelable

data class DsuFileEntry(
    var name: String = "",
    var relativePath: String = "",
    var isDirectory: Boolean = false,
    var sizeBytes: Long = 0L,
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        name = parcel.readString().orEmpty(),
        relativePath = parcel.readString().orEmpty(),
        isDirectory = parcel.readByte().toInt() != 0,
        sizeBytes = parcel.readLong(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(relativePath)
        dest.writeByte(if (isDirectory) 1 else 0)
        dest.writeLong(sizeBytes)
    }

    companion object CREATOR : Parcelable.Creator<DsuFileEntry> {
        override fun createFromParcel(parcel: Parcel): DsuFileEntry = DsuFileEntry(parcel)
        override fun newArray(size: Int): Array<DsuFileEntry?> = arrayOfNulls(size)
    }
}
