package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.AbstractMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val OSLO = ZoneId.of("Europe/Oslo")

internal class ForeldrepengerMottak(
    rapidsConnection: RapidsConnection,
) : AbstractMottak() {
    override val topic = "teamforeldrepenger.vedtak-ekstern"
    override val system = "fp-abakus"

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("personidentifikator", "tidspunkt", "tema") }
            .register(this)
    }

    override fun JsonMessage.parseEvent(): AnnenYtelseEndret {
        val ident = this["personidentifikator"].textValue()
        val tema = this["tema"].textValue()
        val raaTidspunkt = this["tidspunkt"].textValue()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)

        log.info { "Mottok vedtak fra foreldrepenger: tema=$tema, tidspunkt=$tidspunkt (rå=$raaTidspunkt)" }
        sikkerlogg.info { "Mottok vedtak fra foreldrepenger: ident=$ident, tema=$tema, tidspunkt=$tidspunkt" }

        return AnnenYtelseEndret(
            ident = ident,
            tema = tema,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = topic),
        )
    }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()
}
