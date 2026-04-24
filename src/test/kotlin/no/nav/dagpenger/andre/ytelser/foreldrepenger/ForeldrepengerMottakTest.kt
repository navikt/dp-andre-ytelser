package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ForeldrepengerMottakTest {
    private val testRapid = TestRapid()

    init {
        ForeldrepengerMottak(testRapid)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal publisere annen_ytelse_endret for tema FOR`() {
        testRapid.sendTestMessage(vedtakEkstern(ident = "12345678901", tema = "FOR"))

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].asText() shouldBe "annen_ytelse_endret"
        event["ident"].asText() shouldBe "12345678901"
        event["tema"].asText() shouldBe "FOR"
        event["tidspunkt"].asText() shouldBe "2026-04-17T08:30:00+02:00"
        event["kilde"]["system"].asText() shouldBe "fp-abakus"
        event["kilde"]["topic"].asText() shouldBe "teamforeldrepenger.vedtak-ekstern"
    }

    @Test
    fun `skal videresende rå tema OMS uten konvertering`() {
        testRapid.sendTestMessage(vedtakEkstern(ident = "98765432100", tema = "OMS"))

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["tema"].asText() shouldBe "OMS"
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding())

        testRapid.inspektør.size shouldBe 0
    }

    private fun vedtakEkstern(
        ident: String = "12345678901",
        tema: String = "FOR",
    ) = //language=JSON
        """
        {
            "personidentifikator": "$ident",
            "tidspunkt": "2026-04-17T08:30:00+02:00",
            "tema": "$tema"
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "personidentifikator": "12345678901",
            "tidspunkt": "2026-04-17T08:30:00+02:00",
            "tema": "FOR"
        }
        """.trimIndent()
}
