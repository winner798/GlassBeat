package io.github.glassbeat.fonts

/** Metadata only: font binaries are never bundled in the APK. Versions and digests are pinned. */
data class DownloadFont(val id: String, val name: String, val description: String, val fileName: String,
    val bytes: Long, val sha256: String, val licenseSha256: String) {
    private val base get() = "https://raw.githubusercontent.com/google/fonts/5bb28733cec313b63fbd415b574b5ae35c8aa8b8/ofl/$id/"
    val url get() = base + fileName.replace("[", "%5B").replace("]", "%5D")
    val licenseUrl get() = base + "OFL.txt"
    val sizeLabel get() = "%.1f MB".format(java.util.Locale.ROOT, bytes / 1_000_000.0)
}
object FontCatalog {
    val fonts = listOf(
        DownloadFont("notosanssc", "Noto Sans SC", "清晰黑体 · 日常阅读", "NotoSansSC[wght].ttf", 17772300L,
            "a3041811a78c361b1de50f953c805e0244951c21c5bd412f7232ef0d899af0da", "1c05c68c34f9708415aada51f17e1b0092d2cea709bf4a94cd38114f9e73d7d9"),
        DownloadFont("mashanzheng", "马善政楷书", "手写笔意 · 轻松自然", "MaShanZheng-Regular.ttf", 5857936L,
            "6d2546bb189c732a8ca29af9e22457b152387d158aa459e4ac2ce1e51788b7fb", "d7bdb1cee215b689e23c2f95672a6084c790542170648267a55114103d756a08"),
        DownloadFont("zcoolkuaile", "站酷快乐体", "趣味字形 · 活泼醒目", "ZCOOLKuaiLe-Regular.ttf", 1514968L,
            "812a6fc1fe54b6d73a419245c32dfeba8aa33104d5be90d1cf6af082007cb71d", "538078469839b4a2e7ad22bef4ebe41681a4e53749bb2a072144024f1d6d703d"),
        DownloadFont("notoserifsc", "Noto Serif SC", "经典宋体 · 细腻衬线", "NotoSerifSC[wght].ttf", 25125512L,
            "050080d9255a86808f2945bffac582b31ef32bc36411ce29563b4961670c66f9", "5e0da210fb04058a8c0087985d2d456b931c2579811a49655721d3cf0c36b6d6"),
    )
}
