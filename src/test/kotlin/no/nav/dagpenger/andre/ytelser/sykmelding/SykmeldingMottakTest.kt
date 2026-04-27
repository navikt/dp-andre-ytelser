package no.nav.dagpenger.andre.ytelser.sykmelding

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
    fun `skal publisere annen_ytelse_endret for OK sykmelding med tema SYM`() {
        testRapid.sendTestMessage(sykmeldingRecord(fnr = "12345678901", sykmeldingId = "syk-1"))

        testRapid.inspektør.size shouldBe 1
        val event: JsonNode = testRapid.inspektør.message(0)
        event["@event_name"].asText() shouldBe "annen_ytelse_endret"
        event["ident"].asText() shouldBe "12345678901"
        event["tema"].asText() shouldBe "SYM"
        event["tidspunkt"].asText() shouldBe "2026-04-17T08:30:00"
        event["kilde"]["system"].asText() shouldBe "tsm"
        event["kilde"]["topic"].asText() shouldBe "tsm.sykmeldinger"
        event["sykmelding"]["id"].asText() shouldBe "syk-1"
    }

    @Test
    fun `skal videresende aktivitet med kun type fom og tom`() {
        testRapid.sendTestMessage(
            sykmeldingRecord(
                aktivitet =
                    """
                    [
                        {
                            "type": "AKTIVITET_IKKE_MULIG",
                            "fom": "2026-04-15",
                            "tom": "2026-04-30",
                            "medisinskArsak": { "beskrivelse": "skal ikke videresendes" }
                        },
                        {
                            "type": "GRADERT",
                            "fom": "2026-05-01",
                            "tom": "2026-05-15",
                            "grad": 50,
                            "reisetilskudd": false
                        }
                    ]
                    """.trimIndent(),
            ),
        )

        val aktivitet = testRapid.inspektør.message(0)["sykmelding"]["aktivitet"]
        aktivitet.size() shouldBe 2

        aktivitet[0]["type"].asText() shouldBe "AKTIVITET_IKKE_MULIG"
        aktivitet[0]["fom"].asText() shouldBe "2026-04-15"
        aktivitet[0]["tom"].asText() shouldBe "2026-04-30"
        aktivitet[0].has("medisinskArsak") shouldBe false

        aktivitet[1]["type"].asText() shouldBe "GRADERT"
        aktivitet[1]["fom"].asText() shouldBe "2026-05-01"
        aktivitet[1]["tom"].asText() shouldBe "2026-05-15"
        aktivitet[1].has("grad") shouldBe false
        aktivitet[1].has("reisetilskudd") shouldBe false
    }

    @Test
    fun `skal håndtere tom aktivitetsliste`() {
        testRapid.sendTestMessage(sykmeldingRecord(aktivitet = "[]"))

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["sykmelding"]["aktivitet"].size() shouldBe 0
    }

    @ParameterizedTest
    @ValueSource(strings = ["INVALID", "PENDING", "MANUAL_PROCESSING", "OK_WITH_INFOTRYGD"])
    fun `skal filtrere bort sykmelding når validation status ikke er OK`(status: String) {
        testRapid.sendTestMessage(sykmeldingRecord(validationStatus = status))

        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `skal normalisere tidspunkt til Europe-Oslo LocalDateTime`() {
        testRapid.sendTestMessage(sykmeldingRecord(mottattDato = "2026-04-17T06:30:00Z"))

        // 06:30 UTC = 08:30 Oslo (CEST)
        testRapid.inspektør.message(0)["tidspunkt"].asText() shouldBe "2026-04-17T08:30:00"
    }

    @Test
    fun `skal ikke prosessere meldinger fra rapiden`() {
        testRapid.sendTestMessage(rapidMelding())

        testRapid.inspektør.size shouldBe 0
    }

    private fun sykmeldingRecord(
        fnr: String = "12345678901",
        sykmeldingId: String = "sykmelding-uuid-456",
        mottattDato: String = "2026-04-17T08:30:00+02:00",
        validationStatus: String = "OK",
        aktivitet: String = "[]",
    ) = //language=JSON
        """
        {
            "metadata": { "type": "DIGITAL", "orgnummer": "999999999" },
            "sykmelding": {
                "id": "$sykmeldingId",
                "type": "DIGITAL",
                "pasient": {
                    "fnr": "$fnr",
                    "navn": null,
                    "navKontor": null,
                    "navnFastlege": null,
                    "kontaktinfo": []
                },
                "metadata": {
                    "mottattDato": "$mottattDato",
                    "genDate": "2026-04-17T08:00:00+02:00",
                    "avsenderSystem": { "navn": "EPJ", "versjon": "1.0" }
                },
                "aktivitet": $aktivitet,
                "medisinskVurdering": {}
            },
            "validation": {
                "status": "$validationStatus",
                "timestamp": "2026-04-17T08:31:00+02:00",
                "rules": []
            }
        }
        """.trimIndent()

    private fun rapidMelding() =
        //language=JSON
        """
        {
            "@event_name": "noe_fra_rapiden",
            "sykmelding": { "pasient": { "fnr": "12345678901" } },
            "validation": { "status": "OK" }
        }
        """.trimIndent()
}
