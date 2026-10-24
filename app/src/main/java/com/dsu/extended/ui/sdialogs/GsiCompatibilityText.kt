package com.dsu.extended.ui.sdialogs

import com.dsu.extended.R
import com.dsu.extended.compat.CompatibilityCode

data class CompatibilityText(
    val titleRes: Int,
    val detailRes: Int,
)

fun compatibilityText(code: CompatibilityCode): CompatibilityText {
    return when (code) {
        CompatibilityCode.DEVICE_ANDROID_TOO_OLD -> CompatibilityText(
            R.string.compat_issue_device_android_too_old_title,
            R.string.compat_issue_device_android_too_old_detail,
        )

        CompatibilityCode.DEVICE_LAUNCHED_BEFORE_TREBLE -> CompatibilityText(
            R.string.compat_issue_device_launched_before_treble_title,
            R.string.compat_issue_device_launched_before_treble_detail,
        )

        CompatibilityCode.DEVICE_NOT_TREBLE -> CompatibilityText(
            R.string.compat_issue_device_not_treble_title,
            R.string.compat_issue_device_not_treble_detail,
        )

        CompatibilityCode.DEVICE_TREBLE_UNKNOWN -> CompatibilityText(
            R.string.compat_issue_device_treble_unknown_title,
            R.string.compat_issue_device_treble_unknown_detail,
        )

        CompatibilityCode.DEVICE_NO_DYNAMIC_PARTITIONS -> CompatibilityText(
            R.string.compat_issue_device_no_dynamic_partitions_title,
            R.string.compat_issue_device_no_dynamic_partitions_detail,
        )

        CompatibilityCode.DEVICE_DSU_RUNNING -> CompatibilityText(
            R.string.compat_issue_device_dsu_running_title,
            R.string.compat_issue_device_dsu_running_detail,
        )

        CompatibilityCode.DEVICE_DSU_INSTALLED -> CompatibilityText(
            R.string.compat_issue_device_dsu_installed_title,
            R.string.compat_issue_device_dsu_installed_detail,
        )

        CompatibilityCode.DEVICE_BOOTLOADER_LOCKED -> CompatibilityText(
            R.string.compat_issue_device_bootloader_locked_title,
            R.string.compat_issue_device_bootloader_locked_detail,
        )

        CompatibilityCode.DEVICE_CROSS_VERSION_SUPPORTED -> CompatibilityText(
            R.string.compat_issue_device_cross_version_supported_title,
            R.string.compat_issue_device_cross_version_supported_detail,
        )

        CompatibilityCode.DEVICE_CROSS_VERSION_UNSUPPORTED -> CompatibilityText(
            R.string.compat_issue_device_cross_version_unsupported_title,
            R.string.compat_issue_device_cross_version_unsupported_detail,
        )

        CompatibilityCode.DEVICE_CROSS_VERSION_UNKNOWN -> CompatibilityText(
            R.string.compat_issue_device_cross_version_unknown_title,
            R.string.compat_issue_device_cross_version_unknown_detail,
        )

        CompatibilityCode.ABI_SUPPORTED -> CompatibilityText(
            R.string.compat_issue_abi_supported_title,
            R.string.compat_issue_abi_supported_detail,
        )

        CompatibilityCode.ABI_MISMATCH -> CompatibilityText(
            R.string.compat_issue_abi_mismatch_title,
            R.string.compat_issue_abi_mismatch_detail,
        )

        CompatibilityCode.ABI_UNKNOWN -> CompatibilityText(
            R.string.compat_issue_abi_unknown_title,
            R.string.compat_issue_abi_unknown_detail,
        )

        CompatibilityCode.SDK_SUPPORTED -> CompatibilityText(
            R.string.compat_issue_sdk_supported_title,
            R.string.compat_issue_sdk_supported_detail,
        )

        CompatibilityCode.SDK_NEWER -> CompatibilityText(
            R.string.compat_issue_sdk_newer_title,
            R.string.compat_issue_sdk_newer_detail,
        )

        CompatibilityCode.SDK_NEWER_UNVERIFIED -> CompatibilityText(
            R.string.compat_issue_sdk_newer_unverified_title,
            R.string.compat_issue_sdk_newer_unverified_detail,
        )

        CompatibilityCode.SDK_OLDER_THAN_DEVICE -> CompatibilityText(
            R.string.compat_issue_sdk_older_title,
            R.string.compat_issue_sdk_older_detail,
        )

        CompatibilityCode.SDK_VERSION_MISMATCH -> CompatibilityText(
            R.string.compat_issue_sdk_mismatch_title,
            R.string.compat_issue_sdk_mismatch_detail,
        )

        CompatibilityCode.SDK_TOO_OLD -> CompatibilityText(
            R.string.compat_issue_sdk_too_old_title,
            R.string.compat_issue_sdk_too_old_detail,
        )

        CompatibilityCode.SDK_UNKNOWN -> CompatibilityText(
            R.string.compat_issue_sdk_unknown_title,
            R.string.compat_issue_sdk_unknown_detail,
        )

        CompatibilityCode.VNDK_SUPPORTED -> CompatibilityText(
            R.string.compat_issue_vndk_supported_title,
            R.string.compat_issue_vndk_supported_detail,
        )

        CompatibilityCode.VNDK_MISMATCH -> CompatibilityText(
            R.string.compat_issue_vndk_mismatch_title,
            R.string.compat_issue_vndk_mismatch_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_EXT4 -> CompatibilityText(
            R.string.compat_issue_image_ext4_title,
            R.string.compat_issue_image_ext4_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_EROFS -> CompatibilityText(
            R.string.compat_issue_image_erofs_title,
            R.string.compat_issue_image_erofs_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_F2FS -> CompatibilityText(
            R.string.compat_issue_image_f2fs_title,
            R.string.compat_issue_image_f2fs_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_UNKNOWN -> CompatibilityText(
            R.string.compat_issue_image_unknown_title,
            R.string.compat_issue_image_unknown_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_SPARSE -> CompatibilityText(
            R.string.compat_issue_image_sparse_title,
            R.string.compat_issue_image_sparse_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_COMPRESSED -> CompatibilityText(
            R.string.compat_issue_image_compressed_title,
            R.string.compat_issue_image_compressed_detail,
        )

        CompatibilityCode.IMAGE_FORMAT_ZIP -> CompatibilityText(
            R.string.compat_issue_image_zip_title,
            R.string.compat_issue_image_zip_detail,
        )

        CompatibilityCode.IMAGE_FULL_DISK -> CompatibilityText(
            R.string.compat_issue_image_full_disk_title,
            R.string.compat_issue_image_full_disk_detail,
        )

        CompatibilityCode.IMAGE_SUPER -> CompatibilityText(
            R.string.compat_issue_image_super_title,
            R.string.compat_issue_image_super_detail,
        )

        CompatibilityCode.IMAGE_BOOT -> CompatibilityText(
            R.string.compat_issue_image_boot_title,
            R.string.compat_issue_image_boot_detail,
        )

        CompatibilityCode.IMAGE_EMPTY -> CompatibilityText(
            R.string.compat_issue_image_empty_title,
            R.string.compat_issue_image_empty_detail,
        )

        CompatibilityCode.IMAGE_SIZE_NOT_ALIGNED -> CompatibilityText(
            R.string.compat_issue_image_not_aligned_title,
            R.string.compat_issue_image_not_aligned_detail,
        )

        CompatibilityCode.IMAGE_SIZE_MISMATCH -> CompatibilityText(
            R.string.compat_issue_image_size_mismatch_title,
            R.string.compat_issue_image_size_mismatch_detail,
        )

        CompatibilityCode.IMAGE_PROPS_UNAVAILABLE -> CompatibilityText(
            R.string.compat_issue_props_unavailable_title,
            R.string.compat_issue_props_unavailable_detail,
        )

        CompatibilityCode.IMAGE_READ_ERROR -> CompatibilityText(
            R.string.compat_issue_read_error_title,
            R.string.compat_issue_read_error_detail,
        )

        CompatibilityCode.ZIP_NO_SYSTEM_IMAGE -> CompatibilityText(
            R.string.compat_issue_zip_no_system_title,
            R.string.compat_issue_zip_no_system_detail,
        )

        CompatibilityCode.ZIP_MULTIPLE_PARTITIONS -> CompatibilityText(
            R.string.compat_issue_zip_multiple_title,
            R.string.compat_issue_zip_multiple_detail,
        )

        CompatibilityCode.STORAGE_LOW -> CompatibilityText(
            R.string.compat_issue_storage_low_title,
            R.string.compat_issue_storage_low_detail,
        )

        CompatibilityCode.STORAGE_GSID_THRESHOLD -> CompatibilityText(
            R.string.compat_issue_storage_gsid_title,
            R.string.compat_issue_storage_gsid_detail,
        )

        CompatibilityCode.STORAGE_OK -> CompatibilityText(
            R.string.compat_issue_storage_ok_title,
            R.string.compat_issue_storage_ok_detail,
        )

        CompatibilityCode.DATA_SAFE -> CompatibilityText(
            R.string.compat_issue_data_safe_title,
            R.string.compat_issue_data_safe_detail,
        )

        CompatibilityCode.DATA_LIMITED -> CompatibilityText(
            R.string.compat_issue_data_limited_title,
            R.string.compat_issue_data_limited_detail,
        )

        CompatibilityCode.SECURITY_PATCH_OLDER -> CompatibilityText(
            R.string.compat_issue_security_patch_title,
            R.string.compat_issue_security_patch_detail,
        )

        CompatibilityCode.BUILD_TYPE_UNOFFICIAL -> CompatibilityText(
            R.string.compat_issue_build_type_title,
            R.string.compat_issue_build_type_detail,
        )
    }
}
