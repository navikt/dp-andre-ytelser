package no.nav.dagpenger.andre.ytelser.barnepensjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndretSerializer
import no.nav.dagpenger.andre.ytelser.melding.BarnepensjonDetaljer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class BarnepensjonMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "etterlatte.vedtakshendelser"
        const val SYSTEM = "etterlatte-behandling"
        const val TEMA = "EYB"
    }

    init {
        River(rapidsConnection)
            .precondition {
                it.forbid("@event_name")
                it.requireValue("sakstype", "BP")
            }.validate {
                it.requireKey("ident", "sakstype", "type", "vedtakId", "vedtaksdato")
                it.interestedIn("virkningFom")
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        runCatching {
            val ident = packet["ident"].textValue()
            val type = packet["type"].textValue()
            val vedtakId = packet["vedtakId"].longValue()
            val vedtaksdato = LocalDate.parse(packet["vedtaksdato"].textValue())
            val virkningFom = packet["virkningFom"].takeUnless { it.isMissingNode }?.textValue()?.let { LocalDate.parse(it) }
            val tidspunkt = LocalDateTime.of(vedtaksdato, LocalTime.MIDNIGHT)

            log.info { "Mottok barnepensjon-vedtak: type=$type, vedtakId=$vedtakId" }
            sikkerlogg.info {
                "Mottok barnepensjon-vedtak: ident=$ident, type=$type, " +
                    "vedtakId=$vedtakId, vedtaksdato=$vedtaksdato, virkningFom=$virkningFom"
            }

            val event =
                AnnenYtelseEndret(
                    ident = ident,
                    tema = TEMA,
                    tidspunkt = tidspunkt,
                    kilde = AnnenYtelseEndret.Kilde(system = SYSTEM, topic = TOPIC),
                    detaljer =
                        BarnepensjonDetaljer(
                            vedtakId = vedtakId,
                            type = type,
                            vedtaksdato = vedtaksdato,
                            virkningFom = virkningFom,
                        ),
                )
            context.publish(ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())

            meterRegistry
                .counter("ytelse_vedtak_mottatt_total", "tema", TEMA, "kilde", SYSTEM)
                .increment()
        }.onFailure { e ->
            log.error(e) { "Feil ved behandling av barnepensjon-melding" }
            sikkerlogg.error(e) { "Feil ved behandling av barnepensjon-melding: ${packet.toJson()}" }
            throw e
        }
    }
}
