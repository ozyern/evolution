package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Fixtures are trimmed copies of the ROM archive's real markup */
class CnRomParserTest {

    private val otaPage = """
        <select id="device" name="device" class="ota-select ota-device-native" aria-label="Device"
              data-devices='{&quot;OP 12&quot;:{&quot;GLO&quot;:[&quot;CPH2581_16.0.2.400(EX01)&quot;],&quot;CN&quot;:[&quot;PJD110_16.0.3.500(CN01)&quot;,&quot;PJD110_15.0.0.862(CN01)&quot;]},&quot;OP 12R&quot;:{&quot;IN&quot;:[&quot;CPH2585_16.0.0.100(EX01)&quot;]},&quot;OPPO FIND X8&quot;:{&quot;CN&quot;:[&quot;PKB110_16.0.1.300(CN01)&quot;]}}'
              data-version-groups='{}'>
        </select>
        <details class="ota-device-brand" data-ota-device-brand>
          <summary>
            <span class="ota-device-group-title"><span class="ota-device-group-chevron" aria-hidden="true">›</span>OnePlus</span>
            <span class="ota-device-count">2</span>
          </summary>
          <div class="ota-device-brand-body">
            <button type="button" id="ota-device-option-4" role="option" class="ota-device-option" data-device="OP 12" data-search="OP 12 OnePlus 12">
              <span>OnePlus 12</span><span class="ota-device-check" aria-hidden="true">✓</span>
            </button>
            <button type="button" id="ota-device-option-5" role="option" class="ota-device-option" data-device="OP 12R" data-search="OP 12R OnePlus 12R">
              <span>OnePlus 12R</span><span class="ota-device-check" aria-hidden="true">✓</span>
            </button>
          </div>
        </details>
        <details class="ota-device-brand" data-ota-device-brand>
          <summary>
            <span class="ota-device-group-title"><span class="ota-device-group-chevron" aria-hidden="true">›</span>OPPO</span>
          </summary>
          <div class="ota-device-brand-body">
            <button type="button" id="ota-device-option-45" role="option" class="ota-device-option" data-device="OPPO FIND X8" data-search="OPPO FIND X8 OPPO Find X8">
              <span>OPPO Find X8</span><span class="ota-device-check" aria-hidden="true">✓</span>
            </button>
          </div>
        </details>
    """.trimIndent()

    private val buildPage = """
        <div class="ota-result adsense-action-zone google-anno-skip" id="resultBox"
             data-url=""
             data-provider="oplus"
             data-ota-key="d3f309b94d6fb03e"
             data-csrf="dc741b55998cc4722419b086">
          <h3 class="ota-package-title" id="ota-package-title">
            <span>OnePlus 13</span>
            <span class="ota-package-region">China</span>              </h3>
          <p class="ota-version-name"><span class="ota-version-context">PJZ110_</span><strong class="ota-version-highlight">17.0.0.103</strong><span class="ota-version-context">(CN01)</span></p>
          <span class="ota-chip">Patch: 2026-09-01</span>            <span class="ota-chip" title="9,271,674,344 bytes">Size: 8.63 GiB</span>
          <dl class="ota-package-details">
            <div><dt>OTA build</dt><dd><code>PJZ110_11.F.07_2070_202610080010</code></dd></div>              <div><dt>MD5</dt><dd><code>ef71905c53243c0f2c75c2b64ac6edde</code></dd></div>              <div><dt>Published</dt><dd>12 Oct 2026, 04:55 UTC</dd></div>
          </dl>
          <strong>Anti-Rollback detected: ARB <span data-arb-index>1</span></strong>
          <a class="ota-btn ghost" id="changelogBtn" href="/index.php?view=changelog&amp;id=93c7&amp;lang=en#changelog-view" target="_blank">Changelog</a>
        </div>
    """.trimIndent()

    @Test
    fun `devices without China builds are skipped, names & brands are read`() {
        val devices = CnRomParser.parseDevices(otaPage)
        assertEquals(listOf("OP 12", "OPPO FIND X8"), devices.map { it.key })

        devices[0].run {
            assertEquals("OnePlus 12", name)
            assertEquals("OnePlus", brand)
            assertEquals(listOf("PJD110_16.0.3.500(CN01)", "PJD110_15.0.0.862(CN01)"), builds)
            assertEquals(setOf("PJD110"), models)
        }
        assertEquals("OPPO", devices[1].brand)
    }

    @Test
    fun `missing or broken device data yields an empty list`() {
        assertEquals(listOf(), CnRomParser.parseDevices("<html></html>"))
        assertEquals(listOf(), CnRomParser.parseDevices("data-devices='{not json'"))
    }

    @Test
    fun `match prefers model number, then market name`() {
        val devices = CnRomParser.parseDevices(otaPage)
        assertEquals("OP 12", CnRomParser.match(devices, "PJD110", null)?.key)
        assertEquals("OPPO FIND X8", CnRomParser.match(devices, "UNKNOWN", "OPPO Find X8")?.key)
        assertEquals("OP 12", CnRomParser.match(devices, "UNKNOWN", "OnePlus 12")?.key)
        assertNull(CnRomParser.match(devices, "UNKNOWN", "OnePlus 99"))
    }

    @Test
    fun `build page is parsed`() {
        val build = assertNotNull(CnRomParser.parseBuild(buildPage))
        assertEquals("OnePlus 13", build.deviceName)
        assertEquals("China", build.region)
        assertEquals("PJZ110_17.0.0.103(CN01)", build.label)
        assertEquals("17.0.0.103", build.version)
        assertEquals("2026-09-01", build.securityPatch)
        assertEquals(9_271_674_344L, build.sizeBytes)
        assertEquals("PJZ110_11.F.07_2070_202610080010", build.otaVersion)
        assertEquals("ef71905c53243c0f2c75c2b64ac6edde", build.md5)
        assertEquals("12 Oct 2026, 04:55 UTC", build.published)
        assertEquals(1, build.arbIndex)
        assertEquals("${CnRomParser.BaseUrl}/index.php?view=changelog&id=93c7&lang=en#changelog-view", build.changelogUrl)
        assertEquals("d3f309b94d6fb03e", build.otaKey)
        assertEquals("dc741b55998cc4722419b086", build.csrf)
        assertEquals("PJZ110_17.0.0.103(CN01).zip", build.fileName)
    }

    @Test
    fun `page without a result has no build`() {
        assertNull(CnRomParser.parseBuild(otaPage))
    }

    @Test
    fun `version labels are split`() {
        assertEquals("16.0.3.500", CnRomParser.versionOf("PJZ110_16.0.3.500(CN01)"))
        assertEquals("11_C.26", CnRomParser.versionOf("NE2210_11_C.26"))
        assertEquals("PJZ110 · CN01", CnRomParser.detailsOf("PJZ110_16.0.3.500(CN01)"))
        assertEquals("NE2210", CnRomParser.detailsOf("NE2210_11_C.26"))
    }
}
