package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Region code the ROM archive uses for mainland China (ColorOS China) builds */
const val CnRegion = "CN"

/**
 * A device listed in the ROM archive, with its China builds (newest first).
 *
 * @param key the archive's internal device ID, e.g. "OP 13"
 * @param name human-readable name, e.g. "OnePlus 13"
 * @param brand e.g. "OnePlus", "OPPO", "Realme"
 * @param builds raw version labels, e.g. "PJZ110_16.0.3.500(CN01)"
 */
@Immutable
data class CnRomDevice(
    val key: String,
    override val name: String,
    val brand: String,
    val builds: List<String>,
) : SelectableModel {
    /** Model numbers this device ships as, e.g. "PJZ110" */
    val models = builds.mapNotNullTo(LinkedHashSet()) { it.substringBefore('_', "").ifEmpty { null } }

    override val id = key.hashCode().toLong()
    override val subtitle = models.joinToString(", ").ifEmpty { null }
}

/** A single resolved China build, as shown on the archive's build page */
@Immutable
data class CnRomBuild(
    val deviceName: String,
    val region: String,
    /** e.g. "PJZ110_16.0.3.500(CN01)" */
    val label: String,
    val securityPatch: String?,
    val sizeBytes: Long?,
    /** OPPO's internal OTA version, e.g. "PJZ110_11.F.07_2070_202610080010" */
    val otaVersion: String?,
    val md5: String?,
    val published: String?,
    /** Anti-rollback index, if the archive detected one */
    val arbIndex: Int?,
    val changelogUrl: String?,
    /** Session-bound key & token needed to fetch the actual (signed, short-lived) download URL */
    val otaKey: String,
    val csrf: String,
) {
    val version get() = CnRomParser.versionOf(label)
    val fileName get() = "$label.zip".replace(Regex("[^A-Za-z0-9._()-]"), "_")
}

/**
 * Parses pages of the community ROM archive at [CnRomParser.BaseUrl]. It has no public API, so we read
 * the same data its own OTA tool page uses: a JSON map embedded in the device `<select>`, and the result box
 * shown after resolving a build.
 */
object CnRomParser {

    const val BaseUrl = "https://roms.danielspringer.at"
    const val OtaPageUrl = "$BaseUrl/index.php?view=ota"
    const val PackageUrl = "$BaseUrl/index.php?view=ota&ota_action=package"

    private val json = Json { ignoreUnknownKeys = true }

    private val DevicesAttr = Regex("""data-devices='([^']*)'""")
    private val DeviceOption = Regex(
        """data-device="([^"]+)"\s+data-search="[^"]*">\s*<span>([^<]+)</span>"""
    )
    private val BrandSection = Regex("""<details class="ota-device-brand"[\s\S]*?class="ota-device-group-title">[\s\S]*?</span>([^<]+)</span>([\s\S]*?)</details>""")

    /**
     * @return devices that have at least one China build, in the archive's order
     */
    fun parseDevices(html: String): List<CnRomDevice> {
        val raw = DevicesAttr.find(html)?.groupValues?.get(1) ?: return listOf()
        val root = try {
            json.parseToJsonElement(unescapeHtml(raw)) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return listOf()

        // Display names & brands come from the custom picker markup; fall back to the key
        val names = HashMap<String, String>()
        val brands = HashMap<String, String>()
        BrandSection.findAll(html).forEach { section ->
            val brand = unescapeHtml(section.groupValues[1]).trim()
            DeviceOption.findAll(section.groupValues[2]).forEach {
                val key = unescapeHtml(it.groupValues[1])
                names[key] = unescapeHtml(it.groupValues[2]).trim()
                brands[key] = brand
            }
        }

        return root.mapNotNull { (key, regions) ->
            val builds = ((regions as? JsonObject)?.get(CnRegion) as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            CnRomDevice(
                key = key,
                name = names[key] ?: key,
                brand = brands[key] ?: key.substringBefore(' ').let { if (it == "OP") "OnePlus" else it },
                builds = builds,
            )
        }
    }

    /**
     * Finds the archive device for this phone: first by model number (most reliable, since every
     * build label is prefixed with it), then by market name.
     */
    fun match(devices: List<CnRomDevice>, model: String, marketName: String?): CnRomDevice? {
        devices.firstOrNull { model.isNotEmpty() && model in it.models }?.let { return it }
        if (marketName.isNullOrBlank()) return null
        val wanted = normalise(marketName)
        return devices.firstOrNull { normalise(it.name) == wanted }
    }

    /** Parses the build page shown after resolving a build; `null` if no result is on the page */
    fun parseBuild(html: String): CnRomBuild? {
        val box = html.substringAfter("id=\"resultBox\"", "").ifEmpty { return null }
        val otaKey = attr(box, "data-ota-key") ?: return null
        val csrf = attr(box, "data-csrf") ?: return null

        val title = Regex("""class="ota-package-title"[^>]*>\s*<span>([^<]+)</span>\s*(?:<span class="ota-package-region">([^<]+)</span>)?""")
            .find(box)
        val label = Regex("""class="ota-version-name">([\s\S]*?)</p>""").find(box)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")?.let(::unescapeHtml)?.trim()
            ?: return null

        return CnRomBuild(
            deviceName = title?.groupValues?.get(1)?.let(::unescapeHtml)?.trim() ?: "",
            region = title?.groupValues?.get(2)?.let(::unescapeHtml)?.trim()?.ifEmpty { null } ?: CnRegion,
            label = label,
            securityPatch = Regex("""ota-chip">Patch:\s*([^<]+)<""").find(box)?.groupValues?.get(1)?.trim(),
            sizeBytes = Regex("""title="([\d,.]+) bytes"""").find(box)?.groupValues?.get(1)
                ?.filter(Char::isDigit)?.toLongOrNull(),
            otaVersion = detail(box, "OTA build"),
            md5 = detail(box, "MD5"),
            published = detail(box, "Published"),
            arbIndex = Regex("""data-arb-index>(\d+)<""").find(box)?.groupValues?.get(1)?.toIntOrNull(),
            changelogUrl = Regex("""id="changelogBtn"\s+href="([^"]+)"""").find(box)?.groupValues?.get(1)
                ?.let(::unescapeHtml)?.let { if (it.startsWith("/")) BaseUrl + it else it },
            otaKey = otaKey,
            csrf = csrf,
        )
    }

    /** "PJZ110_16.0.3.500(CN01)" → "16.0.3.500"; older labels like "NE2210_11_C.26" → "11_C.26" */
    fun versionOf(label: String) = label.substringAfter('_').substringBefore('(').trim()

    /** "PJZ110_16.0.3.500(CN01)" → "PJZ110 · CN01" */
    fun detailsOf(label: String): String {
        val model = label.substringBefore('_', "")
        val variant = label.substringAfter('(', "").substringBefore(')')
        return listOf(model, variant).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    private fun attr(html: String, name: String) = Regex("""$name="([^"]*)"""").find(html)
        ?.groupValues?.get(1)?.ifEmpty { null }

    private fun detail(html: String, term: String) = Regex("""<dt>${Regex.escape(term)}</dt>\s*<dd>(?:<code>)?([^<]+)""")
        .find(html)?.groupValues?.get(1)?.let(::unescapeHtml)?.trim()

    private fun normalise(name: String) = name.lowercase().replace("oneplus", "op").filter(Char::isLetterOrDigit)

    internal fun unescapeHtml(text: String) = text
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&") // must be last
}
