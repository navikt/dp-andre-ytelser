package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
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
        const val EVENT_NAME = "annen_ytelse_endret"
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
        val ident = packet["personidentifikator"].asText()
        val tema = packet["tema"].asText()
        val raaTidspunkt = packet["tidspunkt"].asText()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)
        val maskertIdent = ident.take(6) + "*****"

        log.info { "Mottok vedtak fra foreldrepenger: tema=$tema, tidspunkt=$tidspunkt (rå=$raaTidspunkt)" }
        sikkerlogg.info { "Mottok vedtak fra foreldrepenger: ident=$maskertIdent, tema=$tema, tidspunkt=$tidspunkt" }

        val event =
            JsonMessage.newMessage(
                EVENT_NAME,
                mapOf(
                    "ident" to ident,
                    "tema" to tema,
                    "tidspunkt" to tidspunkt,
                    "kilde" to
                        mapOf(
                            "system" to SYSTEM,
                            "topic" to TOPIC,
                        ),
                ),
            )
        context.publish(ident, event.toJson())

        meterRegistry
            .counter("ytelse_vedtak_mottatt_total", "tema", tema, "kilde", SYSTEM)
            .increment()
    }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error { "Feil ved parsing av foreldrepenger vedtak-ekstern melding: $problems" }
        sikkerlogg.error { "Feil ved parsing av foreldrepenger vedtak-ekstern melding: $problems" }
    }
}
