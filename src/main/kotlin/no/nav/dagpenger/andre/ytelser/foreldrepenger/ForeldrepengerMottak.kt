package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.Tema

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

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
        val ident = packet["personidentifikator"].asText()
        val kildeTema = packet["tema"].asText()
        val tidspunkt = packet["tidspunkt"].asText()
        val maskertIdent = ident.take(6) + "*****"

        val tema = Tema.fraKildeTema(kildeTema)
        if (tema == null) {
            log.warn { "Ukjent tema=$kildeTema fra $TOPIC — hopper over" }
            return
        }

        log.info { "Mottok vedtak fra foreldrepenger: tema=${tema.name}, tidspunkt=$tidspunkt" }
        sikkerlogg.info { "Mottok vedtak fra foreldrepenger: ident=$maskertIdent, tema=${tema.name}, tidspunkt=$tidspunkt" }

        meterRegistry
            .counter("ytelse_vedtak_mottatt_total", "tema", tema.name, "kilde", SYSTEM)
            .increment()

        context.publish(
            ident,
            JsonMessage
                .newMessage(
                    "andre_ytelse_mottatt",
                    mapOf(
                        "ident" to ident,
                        "tema" to tema.name,
                        "tidspunkt" to tidspunkt,
                        "kilde" to
                            mapOf(
                                "system" to SYSTEM,
                                "topic" to TOPIC,
                            ),
                    ),
                ).toJson(),
        )
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
