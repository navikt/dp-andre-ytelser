package no.nav.dagpenger.andre.ytelser.aap

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

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class AapMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "aap.api-intern-hendelse-v1"
        const val SYSTEM = "aap-api-intern"
        const val TEMA = "AAP"
    }

    init {
        River(rapidsConnection)
            .precondition {
                it.forbid("@event_name")
                it.requireValue("hendelse", "VEDTAK")
            }.validate { it.requireKey("ident") }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        when (metadata.topic) {
            TOPIC -> {
                runCatching {
                    val ident = packet["ident"].asText()
                    val tidspunkt = LocalDateTime.now()

                    log.info { "Mottok AAP-vedtak: tidspunkt=$tidspunkt" }
                    sikkerlogg.info { "Mottok AAP-vedtak fra $SYSTEM: ident=$ident, tidspunkt=$tidspunkt" }

                    val event =
                        AnnenYtelseEndret(
                            ident = ident,
                            tema = TEMA,
                            tidspunkt = tidspunkt,
                            kilde = AnnenYtelseEndret.Kilde(system = SYSTEM, topic = TOPIC),
                        )
                    context.publish(ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())

                    meterRegistry
                        .counter("ytelse_vedtak_mottatt_total", "tema", TEMA, "kilde", SYSTEM)
                        .increment()
                }.onFailure { e ->
                    log.error(e) { "Feil ved behandling av AAP-melding" }
                    sikkerlogg.error(e) { "Feil ved behandling av AAP-melding: ${packet.toJson()}" }
                    throw e
                }

            }

            else -> {
                log.warn { "Mottok melding fra uventet topic: ${metadata.topic}, ignorerer" }
            }
        }
    }
}
