package no.nav.dagpenger.andre.ytelser.aap

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class AapMottakTest {
    private val testRapid = TestRapid()

    init {
        AapMottak(testRapid)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal publisere annen_ytelse_endret for AAP-vedtak`() {
        testRapid.sendTestMessage(aapHendelse(ident = "12345678901", hendelse = "VEDTAK"), "12345678901", "aap.api-intern-hendelse-v1")

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].textValue() shouldBe "annen_ytelse_endret"
        event["ident"].textValue() shouldBe "12345678901"
        event["tema"].textValue() shouldBe "AAP"
        event["tidspunkt"].textValue().shouldNotBeEmpty()
        event["kilde"]["system"].textValue() shouldBe "aap-api-intern"
        event["kilde"]["topic"].textValue() shouldBe "aap.api-intern-hendelse-v1"
    }

    @Test
    fun `skal ignorere SOKNAD-hendelser`() {
        testRapid.sendTestMessage(aapHendelse(hendelse = "SOKNAD"), "12345678901", "aap.api-intern-hendelse-v1")

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding(), "12345678901", "aap.api-intern-hendelse-v1")

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal ignorere meldinger fra feil topic`() {
        testRapid.sendTestMessage(aapHendelse(hendelse = "VEDTAK"), "12345678901", "annet.topic")

        testRapid.inspektør.size shouldBe 0
    }

    private fun aapHendelse(
        ident: String = "12345678901",
        hendelse: String = "VEDTAK",
    ) = //language=JSON
        """
        {
            "ident": "$ident",
            "hendelse": "$hendelse"
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "ident": "12345678901",
            "hendelse": "VEDTAK"
        }
        """.trimIndent()
}
