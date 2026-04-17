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
    fun `skal motta vedtak med tema FOR uten å publisere på rapiden`() {
        testRapid.sendTestMessage(vedtakEkstern(tema = "FOR"))

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal motta vedtak med tema OMS uten å publisere på rapiden`() {
        testRapid.sendTestMessage(vedtakEkstern(ident = "98765432100", tema = "OMS"))

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
}
