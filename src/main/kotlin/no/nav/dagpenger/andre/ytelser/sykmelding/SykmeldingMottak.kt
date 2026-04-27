package no.nav.dagpenger.andre.ytelser.sykmelding

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndretSerializer
import no.nav.dagpenger.andre.ytelser.melding.SykmeldingDetaljer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

private val OSLO = ZoneId.of("Europe/Oslo")

internal class SykmeldingMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "tsm.sykmeldinger"
        const val SYSTEM = "tsm"
        const val TEMA = "SYM"
    }

    init {
        River(rapidsConnection)
            .precondition {
                it.forbid("@event_name")
                // Vi reagerer kun på godkjente sykmeldinger.
                // AVVIST/PENDING-sykmeldinger filtreres bort — dp-saksbehandling
                // skal kun trigge utredning når sykmeldingen er gyldig.
                it.requireValue("validation.status", "OK")
            }.validate { it.requireKey("sykmelding", "validation") }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val sykmelding = packet["sykmelding"]
        val ident = sykmelding["pasient"]["fnr"].asText()
        val sykmeldingId = sykmelding["id"].asText()
        val raaTidspunkt = sykmelding["metadata"]["mottattDato"].asText()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)
        val aktivitet = mapAktivitet(sykmelding["aktivitet"])
        val maskertIdent = ident.take(6) + "*****"

        log.info { "Mottok OK sykmelding: tidspunkt=$tidspunkt" }
        sikkerlogg.info {
            "Mottok OK sykmelding fra $SYSTEM: ident=$maskertIdent, sykmeldingId=$sykmeldingId, " +
                "tidspunkt=$tidspunkt, antallAktivitet=${aktivitet.size}"
        }

        val event =
            AnnenYtelseEndret(
                ident = ident,
                tema = TEMA,
                tidspunkt = tidspunkt,
                kilde = AnnenYtelseEndret.Kilde(system = SYSTEM, topic = TOPIC),
                detaljer =
                    SykmeldingDetaljer(
                        id = sykmeldingId,
                        aktivitet = aktivitet,
                    ),
            )
        context.publish(ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())

        meterRegistry
            .counter("ytelse_vedtak_mottatt_total", "tema", TEMA, "kilde", SYSTEM)
            .increment()
    }

    // Pass-through av aktivitetsperioder: kun type, fom, tom.
    // Detaljer som grad, antall behandlingsdager, medisinsk årsak og fritekst
    // utelates bevisst — dp-konsumenter trenger kun trigger-signalet.
    private fun mapAktivitet(node: JsonNode): List<SykmeldingDetaljer.Aktivitet> =
        if (node.isMissingNode || node.isNull) {
            emptyList()
        } else {
            node.map { aktivitet ->
                SykmeldingDetaljer.Aktivitet(
                    type = aktivitet["type"].asText(),
                    fom = LocalDate.parse(aktivitet["fom"].asText()),
                    tom = LocalDate.parse(aktivitet["tom"].asText()),
                )
            }
        }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error { "Feil ved parsing av sykmelding-melding: $problems" }
        sikkerlogg.error { "Feil ved parsing av sykmelding-melding: $problems" }
    }
}
