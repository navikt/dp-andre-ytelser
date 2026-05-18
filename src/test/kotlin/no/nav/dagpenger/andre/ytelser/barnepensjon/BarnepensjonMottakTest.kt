package no.nav.dagpenger.andre.ytelser.barnepensjon

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.JsonNode

class BarnepensjonMottakTest {
    private val testRapid = TestRapid()

    init {
        BarnepensjonMottak(testRapid)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal publisere annen_ytelse_endret for barnepensjon-vedtak`() {
        testRapid.sendTestMessage(vedtakshendelse(), "test-key", "etterlatte.vedtakshendelser")

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].stringValue() shouldBe "annen_ytelse_endret"
        event["ident"].stringValue() shouldBe "12345678901"
        event["tema"].stringValue() shouldBe "EYB"
        event["tidspunkt"].stringValue() shouldBe "2026-05-12T00:00:00"
        event["kilde"]["system"].stringValue() shouldBe "etterlatte-behandling"
        event["kilde"]["topic"].stringValue() shouldBe "etterlatte.vedtakshendelser"
    }

    @Test
    fun `skal videresende vedtaksdetaljer`() {
        testRapid.sendTestMessage(
            vedtakshendelse(vedtakId = 99, type = "INNVILGELSE", vedtaksdato = "2026-05-12", virkningFom = "2026-06-01"),
            "test-key",
            "etterlatte.vedtakshendelser",
        )

        val bp = testRapid.inspektør.message(0)["barnepensjon"]
        bp["vedtakId"].longValue() shouldBe 99
        bp["type"].stringValue() shouldBe "INNVILGELSE"
        bp["vedtaksdato"].stringValue() shouldBe "2026-05-12"
        bp["virkningFom"].stringValue() shouldBe "2026-06-01"
    }

    @ParameterizedTest
    @ValueSource(strings = ["AVSLAG", "INNVILGELSE", "ENDRING", "REGULERING", "OPPHOER"])
    fun `skal håndtere alle vedtakstyper`(type: String) {
        testRapid.sendTestMessage(vedtakshendelse(type = type), "test-key", "etterlatte.vedtakshendelser")

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["barnepensjon"]["type"].stringValue() shouldBe type
    }

    @Test
    fun `skal filtrere bort OMS-hendelser`() {
        testRapid.sendTestMessage(vedtakshendelse(sakstype = "OMS"), "test-key", "etterlatte.vedtakshendelser")

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding(), "test-key", "etterlatte.vedtakshendelser")

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal håndtere vedtak uten virkningFom`() {
        testRapid.sendTestMessage(vedtakshendelse(virkningFom = null), "test-key", "etterlatte.vedtakshendelser")

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["barnepensjon"].has("virkningFom") shouldBe false
    }

    private fun vedtakshendelse(
        ident: String = "12345678901",
        sakstype: String = "BP",
        type: String = "INNVILGELSE",
        vedtakId: Long = 12345,
        vedtaksdato: String = "2026-05-12",
        virkningFom: String? = "2026-06-01",
    ): String {
        val virkningFomJson = if (virkningFom != null) ""","virkningFom": "$virkningFom"""" else ""
        //language=JSON
        return """
            {
                "ident": "$ident",
                "sakstype": "$sakstype",
                "type": "$type",
                "vedtakId": $vedtakId,
                "vedtaksdato": "$vedtaksdato"$virkningFomJson
            }
            """.trimIndent()
    }

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "ident": "12345678901",
            "sakstype": "BP",
            "type": "INNVILGELSE",
            "vedtakId": 12345,
            "vedtaksdato": "2026-05-12"
        }
        """.trimIndent()
}
