package no.nav.dagpenger.andre.ytelser.uforetrygd

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class UforetrygdMottakTest {
    private val testRapid = TestRapid()

    init {
        UforetrygdMottak(testRapid, "pensjondeployer.uforevedtak-dagpenger")
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal publisere annen_ytelse_endret ved uførevedtak`() {
        testRapid.sendTestMessage(uforevedtak(), "test-key", "pensjondeployer.uforevedtak-dagpenger")

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].stringValue() shouldBe "annen_ytelse_endret"
        event["ident"].stringValue() shouldBe "12345678901"
        event["tema"].stringValue() shouldBe "UFO"
        event["tidspunkt"].stringValue() shouldBe "2026-06-01T00:00:00"
        event["kilde"]["system"].stringValue() shouldBe "pensjon-pen"
        event["kilde"]["topic"].stringValue() shouldBe "pensjondeployer.uforevedtak-dagpenger"
        event["resultat"].stringValue() shouldBe "INNV"
        event["uforegrad"].intValue() shouldBe 100
    }

    @Test
    fun `skal håndtere vedtak uten uføregrad`() {
        testRapid.sendTestMessage(uforevedtak(uforegrad = null), "test-key", "pensjondeployer.uforevedtak-dagpenger")

        testRapid.inspektør.size shouldBe 1
        val event = testRapid.inspektør.message(0)
        event["resultat"].stringValue() shouldBe "INNV"
        event.has("uforegrad") shouldBe false
    }

    @Test
    fun `skal håndtere opphør-vedtak`() {
        testRapid.sendTestMessage(uforevedtak(resultat = "OPPH"), "test-key", "pensjondeployer.uforevedtak-dagpenger")

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["resultat"].stringValue() shouldBe "OPPH"
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding(), "test-key", "pensjondeployer.uforevedtak-dagpenger")

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal ikke prosessere meldinger fra andre topics`() {
        testRapid.sendTestMessage(uforevedtak(), "test-key", "annet.topic")

        testRapid.inspektør.size shouldBe 0
    }

    private fun uforevedtak(
        personId: String = "12345678901",
        virkningsdato: String = "2026-06-01",
        resultat: String = "INNV",
        uforegrad: Int? = 100,
    ) = //language=JSON
        """
        {
            "personId": "$personId",
            "virkningsdato": "$virkningsdato",
            "resultat": "$resultat"${uforegrad?.let { ""","uforegrad": $it""" } ?: ""}
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "personId": "12345678901",
            "virkningsdato": "2026-06-01",
            "resultat": "INNV"
        }
        """.trimIndent()
}
