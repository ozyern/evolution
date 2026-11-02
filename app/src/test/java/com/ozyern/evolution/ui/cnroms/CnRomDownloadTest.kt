package com.ozyern.evolution.ui.cnroms

import com.ozyern.evolution.ui.cnroms.CnRomDownload.Companion.jobFromTags
import com.ozyern.evolution.ui.cnroms.CnRomDownload.Companion.toTags
import com.ozyern.evolution.workers.CnRomDownloadWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CnRomDownloadTest {

    private val job = CnRomDownload.Job(
        deviceKey = "OnePlus 13",
        index = 3,
        label = "PJZ110_16.0.3.500(CN01)",
        fileName = "PJZ110_16.0.3.500(CN01).zip",
        md5 = "d41d8cd98f00b204e9800998ecf8427e",
        title = "OnePlus 13 16.0.3.500",
    )

    @Test
    fun `job survives a round trip through work tags`() {
        assertEquals(job, jobFromTags(job.toTags().toSet()))
    }

    @Test
    fun `job without an MD5 survives a round trip`() {
        val noMd5 = job.copy(md5 = null)
        assertEquals(noMd5, jobFromTags(noMd5.toTags().toSet()))
    }

    @Test
    fun `values containing the separator aren't truncated`() {
        val tricky = job.copy(title = "a=b=c")
        assertEquals("a=b=c", jobFromTags(tricky.toTags().toSet())?.title)
    }

    @Test
    fun `unrelated tags are ignored`() {
        val tags = job.toTags().toSet() + "com.ozyern.evolution.workers.CnRomDownloadWorker"
        assertEquals(job, jobFromTags(tags))
    }

    @Test
    fun `tags without required fields give no job`() {
        assertNull(jobFromTags(emptySet()))
        assertNull(jobFromTags(setOf("cnrom:key=OnePlus 13")))
    }

    @Test
    fun `running download computes fraction and time left`() {
        val running = running(bytesDone = 250, totalBytes = 1000, bytesPerSecond = 50)
        assertEquals(0.25f, running.fraction)
        assertEquals(15L, running.secondsLeft)
    }

    @Test
    fun `unknown total size gives no fraction or time left`() {
        val running = running(bytesDone = 250, totalBytes = 0, bytesPerSecond = 50)
        assertEquals(0f, running.fraction)
        assertEquals(-1L, running.secondsLeft)
    }

    @Test
    fun `no speed yet gives no time left`() {
        assertEquals(-1L, running(bytesDone = 0, totalBytes = 1000, bytesPerSecond = 0).secondsLeft)
    }

    @Test
    fun `MD5 matches known digests, including leading zeros`() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", CnRomDownloadWorker.md5Of(ByteArray(0).inputStream()))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CnRomDownloadWorker.md5Of("abc".byteInputStream()))
        // Digest starts with 0x0c, whose leading zero must be kept
        assertEquals("0cc175b9c0f1b6a831c399e269772661", CnRomDownloadWorker.md5Of("a".byteInputStream()))
    }

    private fun running(bytesDone: Long, totalBytes: Long, bytesPerSecond: Long) = CnRomDownload.Running(
        job = job,
        phase = CnRomDownloadWorker.PhaseDownloading,
        bytesDone = bytesDone,
        totalBytes = totalBytes,
        bytesPerSecond = bytesPerSecond,
    )
}
