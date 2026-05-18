package no.nav.dagpenger.andre.ytelser.sykmelding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.EksternTopicMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.SykmeldingDetaljer
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val OSLO = ZoneId.of("Europe/Oslo")

internal class SykmeldingMottak(
    rapidsConnection: RapidsConnection,
) : EksternTopicMottak() {
    override val topic = "tsm.sykmeldinger"
    override val system = "tsm"

    companion object {
        const val TEMA = "SYM"
    }

    init {
        River(rapidsConnection)
            .precondition {
                it.forbid("@event_name")
                it.requireValue("validation.status", "OK")
            }.validate { it.requireKey("sykmelding", "validation") }
            .register(this)
    }

    override fun JsonMessage.parseEvent(): AnnenYtelseEndret {
        val sykmelding = this["sykmelding"]
        val ident = sykmelding["pasient"]["fnr"].stringValue()
        val sykmeldingId = sykmelding["id"].stringValue()
        val raaTidspunkt = sykmelding["metadata"]["mottattDato"].stringValue()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)
        val aktivitet = mapAktivitet(sykmelding["aktivitet"])

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = topic),
            detaljer =
                SykmeldingDetaljer(
                    id = sykmeldingId,
                    aktivitet = aktivitet,
                ),
        )
    }

    private fun mapAktivitet(node: JsonNode): List<SykmeldingDetaljer.Aktivitet> =
        if (node.isMissingNode || node.isNull) {
            emptyList()
        } else {
            node.values().map { aktivitet ->
                SykmeldingDetaljer.Aktivitet(
                    type = aktivitet["type"].stringValue(),
                    fom = LocalDate.parse(aktivitet["fom"].stringValue()),
                    tom = LocalDate.parse(aktivitet["tom"].stringValue()),
                )
            }
        }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()
}
