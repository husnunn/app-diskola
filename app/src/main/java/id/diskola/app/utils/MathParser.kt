package id.diskola.app.utils

/**
 * Satu rumus LaTeX yang ditemukan di dalam teks. [start] inklusif dan [end] eksklusif, keduanya
 * mencakup delimiter (`\(`…`\)`, `\[`…`\]`, `$$`…`$$`). [display] true untuk `\[…\]` dan `$$…$$`.
 */
data class MathRange(val start: Int, val end: Int, val latex: String, val display: Boolean)

private val patternBlock = Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)
private val patternDisplay = Regex("""\\\[(.*?)\\\]""", RegexOption.DOT_MATCHES_ALL)
private val patternInline = Regex("""\\\((.*?)\\\)""", RegexOption.DOT_MATCHES_ALL)

/** Cek cepat sebelum parsing HTML: ada kemungkinan rumus atau tidak. `$` tunggal bukan delimiter. */
fun containsMath(raw: String): Boolean =
    raw.contains("\\(") || raw.contains("\\[") || raw.contains("$$")

/**
 * Cari semua rumus pada [text] (teks polos hasil parsing HTML, sehingga `&lt;` sudah menjadi `<`).
 *
 * Hasil berurutan menurut posisi dan **tidak saling tumpang-tindih**: bila dua delimiter saling
 * bertumpuk (mis. `\[ \( x \) \]`), yang mulai lebih awal dipertahankan. Rumus yang isinya kosong
 * atau hanya spasi diabaikan.
 */
fun findMathRanges(text: String): List<MathRange> {
    val all = mutableListOf<MathRange>()
    fun collect(pattern: Regex, display: Boolean) {
        pattern.findAll(text).forEach { m ->
            val latex = m.groupValues[1].trim()
            if (latex.isNotEmpty()) {
                all.add(MathRange(m.range.first, m.range.last + 1, latex, display))
            }
        }
    }
    collect(patternBlock, display = true)
    collect(patternDisplay, display = true)
    collect(patternInline, display = false)

    all.sortBy { it.start }
    val result = mutableListOf<MathRange>()
    var lastEnd = 0
    for (range in all) {
        if (range.start >= lastEnd) {
            result.add(range)
            lastEnd = range.end
        }
    }
    return result
}
