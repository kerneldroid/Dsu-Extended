package com.dsu.extended.util

/**
 * Keeps only log lines that belong to DSU/GSI. gsid and the dynamic system
 * services are always kept; noisy tags such as init, vold, kernel and
 * system_server are kept only when the message mentions a GSI/DSU subsystem.
 *
 * The kernel variant additionally keeps filesystem and block-layer lines,
 * because those carry the actual boot-failure cause after a DSU reboot.
 */
object GsiLogFilter {

    private val alwaysTags = setOf(
        "gsid",
        "dynamicsystemservice",
        "dynamicsysteminstallationservice",
        "dynsysteminstallationservice",
        "gsi_tool",
        "dsuextended",
        "dsuextwatermark",
        "logcatdiagnostic",
        "dsuinstallservice",
        "dsuinstaller",
        "dsuinstallationhandler",
        "homeviewmodel",
        "privilegedservice",
        "gsicompatibilitychecker",
        "gsicompatibilityevaluator",
        "gsiimageanalyzer",
    )

    private val keywords = listOf(
        "gsid",
        "gsi",
        "dsu",
        "dynamic system",
        "dynamic_system",
        "dynamic partition",
        "dynamic_partition",
        "dynsystem",
        "dm-verity",
        "dm_verity",
        "dm-linear",
        "dm-crypt",
        "device-mapper",
        "verity",
        "vbmeta",
        "avb",
        "verified boot",
        "super partition",
        "super_empty",
        "loop device",
        "loopback",
        "logical partition",
        "/data/gsi",
        "system partition",
        "gsi_",
        "pstore",
        "last_kmsg",
        "avc: denied",
        "sepolicy",
        "mount failed",
        "block device",
        "fstab",
    )

    private val kernelKeywords = keywords + listOf(
        "ext4",
        "f2fs",
        "jbd2",
        "kernel panic",
        "unable to handle kernel",
        "watchdog",
        "oops",
        "call trace",
        "mmc",
        "ufs",
        "scsi",
        "blk_update_request",
        "i/o error",
    )

    private val threadTimeTag = Regex(
        """^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3}\s+\d+\s+\d+\s+[VDIWEF]\s+(.+?):\s""",
    )
    private val tagFormatTag = Regex("""^[VDIWEF]/([^:]+):\s""")

    fun shouldInclude(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        if (isSectionMarker(trimmed)) return true
        val tag = extractTag(trimmed)?.lowercase()
        if (tag != null && tag in alwaysTags) return true
        return containsKeyword(trimmed, keywords)
    }

    fun filter(text: String): String = filterLines(text, ::shouldInclude)

    fun filterKernel(text: String): String {
        return filterLines(text) { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@filterLines false
            if (isSectionMarker(trimmed)) return@filterLines true
            val tag = extractTag(trimmed)?.lowercase()
            if (tag != null && tag in alwaysTags) return@filterLines true
            containsKeyword(trimmed, kernelKeywords)
        }
    }

    fun extractTag(line: String): String? {
        threadTimeTag.find(line)?.let { return it.groupValues[1].trim() }
        tagFormatTag.find(line)?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun filterLines(text: String, predicate: (String) -> Boolean): String {
        if (text.isBlank()) return ""
        var continuation = 0
        return text.lineSequence()
            .filter { line ->
                val started = predicate(line)
                val keep = started || (continuation > 0 && extractTag(line) == null)
                continuation = if (started) CONTINUATION_LINES else (continuation - 1).coerceAtLeast(0)
                keep
            }
            .joinToString(separator = "\n")
    }

    private fun isSectionMarker(line: String): Boolean {
        return line.startsWith("---") || line.startsWith("===")
    }

    private fun containsKeyword(line: String, words: List<String>): Boolean {
        val lower = line.lowercase()
        return words.any { lower.contains(it) }
    }

    private const val CONTINUATION_LINES = 50
}
