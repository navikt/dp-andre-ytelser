package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class ForeldrepengerMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "teamforeldrepenger.vedtak-ekstern"
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
        val tema = packet["tema"].asText()
        val tidspunkt = packet["tidspunkt"].asText()

        log.info { "Mottok vedtak fra foreldrepenger med tema=$tema, tidspunkt=$tidspunkt" }

        val melding =
            JsonMessage
                .newMessage(
                    "annen_ytelse_vedtatt",
                    mapOf(
                        "ident" to packet["personidentifikator"].asText(),
                        "tidspunkt" to tidspunkt,
                        "tema" to tema,
                        "kilde" to "foreldrepenger",
                    ),
                )

        context.publish(melding.toJson())

        meterRegistry
            .counter("ytelse_vedtak_mottatt_total", "tema", tema, "kilde", "foreldrepenger")
            .increment()

        log.info { "Publiserte annen_ytelse_vedtatt (kilde=foreldrepenger, tema=$tema) på rapiden" }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error { "Feil ved parsing av foreldrepenger vedtak-ekstern melding: $problems" }
        sikkerlogg.error { "Feil ved parsing av foreldrepenger vedtak-ekstern melding: $problems" }
    }
}
