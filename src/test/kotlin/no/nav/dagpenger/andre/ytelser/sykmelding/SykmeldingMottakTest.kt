package no.nav.dagpenger.andre.ytelser.sykmelding

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SykmeldingMottakTest {
    private val testRapid = TestRapid()

    init {
        SykmeldingMottak(testRapid)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal motta sykmelding uten å publisere på rapiden`() {
        testRapid.sendTestMessage(sykmeldingRecord())

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding())

        testRapid.inspektør.size shouldBe 0
    }

    private fun sykmeldingRecord(fnr: String = "12345678901") =
        //language=JSON
        """
        {
            "metadata": {
                "msgId": "uuid-123",
                "type": "SYKMELDING"
            },
            "sykmelding": {
                "id": "sykmelding-uuid-456",
                "type": "DIGITAL",
                "pasient": {
                    "fnr": "$fnr",
                    "navn": null,
                    "navKontor": null,
                    "navnFastlege": null,
                    "kontaktinfo": []
                },
                "metadata": {
                    "mottattDato": "2026-04-17T08:30:00+02:00",
                    "genDate": "2026-04-17T08:00:00+02:00",
                    "avsenderSystem": { "navn": "EPJ", "versjon": "1.0" }
                },
                "aktivitet": [],
                "medisinskVurdering": {}
            },
            "validation": { "status": "OK", "rules": [] }
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "sykmelding": { "pasient": { "fnr": "12345678901" } }
        }
        """.trimIndent()
}
