package no.nav.dagpenger.andre.ytelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndretSerializer

internal abstract class EksternTopicMottak : River.PacketListener {
    protected val log: KLogger = KotlinLogging.logger(this::class.java.name)
    protected val sikkerlogg: KLogger = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")

    abstract val topic: String
    abstract val system: String

    abstract fun JsonMessage.parseEvent(): AnnenYtelseEndret

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        when (metadata.topic) {
            topic -> {
                runCatching {
                    val event = packet.parseEvent()
                    log.info { "Mottok vedtak fra $system: $event" }
                    sikkerlogg.info { "Mottok vedtak fra $system: ${event.toSikkerLoggString()}" }
                    context.publish(event.ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())
                    meterRegistry
                        .counter("ytelse_vedtak_mottatt_total", "tema", event.tema, "kilde", system)
                        .increment()
                }.onFailure { e ->
                    log.error(e) { "Feil ved behandling av melding fra $system" }
                    sikkerlogg.error(e) { "Feil ved behandling av melding fra $system: ${packet.toJson()}" }
                    throw e
                }
            }

            else -> {
                log.warn { "Mottok melding fra uventet topic: ${metadata.topic}, forventet $topic" }
            }
        }
    }
}
