package no.nav.dagpenger.andre.ytelser.sykmelding

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

internal class SykmeldingMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        const val TOPIC = "tsm.sykmeldinger"
        const val SYSTEM = "tsm"
    }

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("sykmelding") }
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
        val tidspunkt = sykmelding["metadata"]["mottattDato"].asText()
        val maskertIdent = ident.take(6) + "*****"

        log.info { "Mottok sykmelding fra $SYSTEM: tidspunkt=$tidspunkt" }
        sikkerlogg.info { "Mottok sykmelding fra $SYSTEM: ident=$maskertIdent, tidspunkt=$tidspunkt" }

        meterRegistry
            .counter("ytelse_vedtak_mottatt_total", "tema", "SYKMELDING", "kilde", SYSTEM)
            .increment()
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error { "Feil ved parsing av sykmelding-melding: $problems" }
        sikkerlogg.error { "Feil ved parsing av sykmelding-melding: $problems" }
    }
}
