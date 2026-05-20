package no.nav.dagpenger.andre.ytelser.institusjon

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.JsonNode

class InstitusjonMottakTest {
    private val testRapid = TestRapid()

    init {
        InstitusjonMottak(testRapid, "team-rocket.institusjon-opphold-hendelser")
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal publisere annen_ytelse_endret for institusjonsopphold`() {
        testRapid.sendTestMessage(oppholdHendelse(), "test-key", "team-rocket.institusjon-opphold-hendelser")

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].stringValue() shouldBe "annen_ytelse_endret"
        event["ident"].stringValue() shouldBe "12345678901"
        event["tema"].stringValue() shouldBe "INST"
        event["kilde"]["system"].stringValue() shouldBe "inst2"
        event["kilde"]["topic"].stringValue() shouldBe "team-rocket.institusjon-opphold-hendelser"
    }

    @Test
    fun `skal videresende oppholddetaljer`() {
        testRapid.sendTestMessage(
            oppholdHendelse(oppholdId = 456, hendelseId = 123, type = "INNMELDING", kilde = "KDI"),
            "test-key",
            "team-rocket.institusjon-opphold-hendelser",
        )

        val institusjon = testRapid.inspektør.message(0)["institusjon"]
        institusjon["oppholdId"].longValue() shouldBe 456
        institusjon["hendelseId"].longValue() shouldBe 123
        institusjon["type"].stringValue() shouldBe "INNMELDING"
        institusjon["kilde"].stringValue() shouldBe "KDI"
    }

    @ParameterizedTest
    @ValueSource(strings = ["INNMELDING", "OPPDATERING", "UTMELDING", "ANNULERING"])
    fun `skal håndtere alle hendelsestyper`(type: String) {
        testRapid.sendTestMessage(oppholdHendelse(type = type), "test-key", "team-rocket.institusjon-opphold-hendelser")

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["institusjon"]["type"].stringValue() shouldBe type
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding(), "test-key", "team-rocket.institusjon-opphold-hendelser")

        testRapid.inspektør.size shouldBe 0
    }

    private fun oppholdHendelse(
        ident: String = "12345678901",
        oppholdId: Long = 456,
        hendelseId: Long = 123,
        type: String = "INNMELDING",
        kilde: String = "KDI",
    ) = //language=JSON
        """
        {
            "hendelseId": $hendelseId,
            "oppholdId": $oppholdId,
            "norskident": "$ident",
            "type": "$type",
            "kilde": "$kilde"
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "norskident": "12345678901",
            "type": "INNMELDING",
            "oppholdId": 456
        }
        """.trimIndent()
}
