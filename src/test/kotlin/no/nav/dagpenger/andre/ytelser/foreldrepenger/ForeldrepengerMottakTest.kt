package no.nav.dagpenger.andre.ytelser.foreldrepenger

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
    fun `skal publisere annen_ytelse_vedtatt ved mottak av vedtak med tema FOR`() {
        testRapid.sendTestMessage(vedtakEkstern(tema = "FOR"))

        val inspektør = testRapid.inspektør
        inspektør.size shouldBe 1
        inspektør.message(0).let { msg ->
            msg["@event_name"].asText() shouldBe "annen_ytelse_vedtatt"
            msg["ident"].asText() shouldBe "12345678901"
            msg["tidspunkt"].asText() shouldBe "2026-04-17T08:30:00+02:00"
            msg["tema"].asText() shouldBe "FOR"
            msg["kilde"].asText() shouldBe "foreldrepenger"
        }
    }

    @Test
    fun `skal publisere annen_ytelse_vedtatt ved mottak av vedtak med tema OMS`() {
        testRapid.sendTestMessage(vedtakEkstern(ident = "98765432100", tema = "OMS"))

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).let { msg ->
            msg["tema"].asText() shouldBe "OMS"
            msg["kilde"].asText() shouldBe "foreldrepenger"
        }
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
}
