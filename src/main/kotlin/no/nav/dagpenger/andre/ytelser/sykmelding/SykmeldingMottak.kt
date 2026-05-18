package no.nav.dagpenger.andre.ytelser.sykmelding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.AbstractMottak
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
) : AbstractMottak() {
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
        val ident = sykmelding["pasient"]["fnr"].textValue()
        val sykmeldingId = sykmelding["id"].textValue()
        val raaTidspunkt = sykmelding["metadata"]["mottattDato"].textValue()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)
        val aktivitet = mapAktivitet(sykmelding["aktivitet"])

        log.info { "Mottok OK sykmelding: tidspunkt=$tidspunkt" }
        sikkerlogg.info {
            "Mottok OK sykmelding fra $system: ident=$ident, sykmeldingId=$sykmeldingId, " +
                "tidspunkt=$tidspunkt, antallAktivitet=${aktivitet.size}"
        }

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
                    type = aktivitet["type"].textValue(),
                    fom = LocalDate.parse(aktivitet["fom"].textValue()),
                    tom = LocalDate.parse(aktivitet["tom"].textValue()),
                )
            }
        }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()
}
