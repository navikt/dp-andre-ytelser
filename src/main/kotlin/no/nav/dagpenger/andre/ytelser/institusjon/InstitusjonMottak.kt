package no.nav.dagpenger.andre.ytelser.institusjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndretSerializer
import no.nav.dagpenger.andre.ytelser.melding.InstitusjonDetaljer
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class InstitusjonMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "team-rocket.institusjon-opphold-hendelser"
        const val SYSTEM = "inst2"
        const val TEMA = "INST"
    }

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("norskident", "type", "oppholdId", "hendelseId", "kilde") }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        runCatching {
            val ident = packet["norskident"].textValue()
            val type = packet["type"].textValue()
            val oppholdId = packet["oppholdId"].longValue()
            val hendelseId = packet["hendelseId"].longValue()
            val kilde = packet["kilde"].textValue()
            val tidspunkt = LocalDateTime.now() // Meldingen har ikke eget tidspunkt

            log.info { "Mottok institusjonsopphold-hendelse: type=$type, oppholdId=$oppholdId, kilde=$kilde" }
            sikkerlogg.info {
                "Mottok institusjonsopphold-hendelse: ident=$ident, type=$type, " +
                    "oppholdId=$oppholdId, hendelseId=$hendelseId, kilde=$kilde"
            }

            val event =
                AnnenYtelseEndret(
                    ident = ident,
                    tema = TEMA,
                    tidspunkt = tidspunkt,
                    kilde = AnnenYtelseEndret.Kilde(system = SYSTEM, topic = TOPIC),
                    detaljer =
                        InstitusjonDetaljer(
                            oppholdId = oppholdId,
                            hendelseId = hendelseId,
                            type = type,
                            kilde = kilde,
                        ),
                )
            context.publish(ident, AnnenYtelseEndretSerializer.toJsonMessage(event).toJson())

            meterRegistry
                .counter("ytelse_vedtak_mottatt_total", "tema", TEMA, "kilde", SYSTEM)
                .increment()
        }.onFailure { e ->
            log.error(e) { "Feil ved behandling av institusjonsopphold-melding" }
            sikkerlogg.error(e) { "Feil ved behandling av institusjonsopphold-melding: ${packet.toJson()}" }
            throw e
        }
    }
}
