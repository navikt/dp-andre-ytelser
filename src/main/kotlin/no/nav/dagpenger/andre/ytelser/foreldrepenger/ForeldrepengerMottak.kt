package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndretSerializer
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

private val OSLO = ZoneId.of("Europe/Oslo")

internal class ForeldrepengerMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "teamforeldrepenger.vedtak-ekstern"
        const val SYSTEM = "fp-abakus"
    }

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("personidentifikator", "tidspunkt", "tema") }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        runCatching {
            val ident = packet["personidentifikator"].textValue()
            val tema = packet["tema"].textValue()
            val raaTidspunkt = packet["tidspunkt"].textValue()
            val tidspunkt = normaliserTilOsloTid(raaTidspunkt)

            log.info { "Mottok vedtak fra foreldrepenger: tema=$tema, tidspunkt=$tidspunkt (rå=$raaTidspunkt)" }
            sikkerlogg.info { "Mottok vedtak fra foreldrepenger: ident=$ident, tema=$tema, tidspunkt=$tidspunkt" }

            val event =
                AnnenYtelseEndret(
                    ident = ident,
                    tema = tema,
                    tidspunkt = tidspunkt,
                    kilde = AnnenYtelseEndret.Kilde(system = SYSTEM, topic = TOPIC),
                )
            context.publish(ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())

            meterRegistry
                .counter("ytelse_vedtak_mottatt_total", "tema", tema, "kilde", SYSTEM)
                .increment()
        }.onFailure { e ->
            log.error(e) { "Feil ved behandling av foreldrepenger-melding" }
            sikkerlogg.error(e) { "Feil ved behandling av foreldrepenger-melding: ${packet.toJson()}" }
            throw e
        }
    }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()
}
