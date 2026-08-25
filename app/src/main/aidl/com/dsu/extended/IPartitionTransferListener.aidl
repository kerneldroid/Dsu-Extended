package com.dsu.extended;

oneway interface IPartitionTransferListener {
    void onProgress(long copiedBytes, long totalBytes);
    void onCompleted(int resultCode);
}
