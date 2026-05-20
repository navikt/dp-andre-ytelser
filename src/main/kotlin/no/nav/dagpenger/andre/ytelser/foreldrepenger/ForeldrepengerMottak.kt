package no.nav.dagpenger.andre.ytelser.foreldrepenger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.EksternTopicMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val OSLO = ZoneId.of("Europe/Oslo")

internal class ForeldrepengerMottak(
    rapidsConnection: RapidsConnection,
) : EksternTopicMottak() {
    override val topics = setOf("teamforeldrepenger.vedtak-ekstern")
    override val system = "fp-abakus"

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("personidentifikator", "tidspunkt", "tema") }
            .register(this)
    }

    override fun JsonMessage.parseEvent(actualTopic: String): AnnenYtelseEndret {
        val ident = this["personidentifikator"].stringValue()
        val tema = this["tema"].stringValue()
        val raaTidspunkt = this["tidspunkt"].stringValue()
        val tidspunkt = normaliserTilOsloTid(raaTidspunkt)

        return AnnenYtelseEndret(
            ident = ident,
            tema = tema,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = actualTopic),
        )
    }

    private fun normaliserTilOsloTid(raa: String): LocalDateTime = OffsetDateTime.parse(raa).atZoneSameInstant(OSLO).toLocalDateTime()
}
