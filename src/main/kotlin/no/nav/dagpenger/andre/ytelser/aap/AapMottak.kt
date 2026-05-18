package no.nav.dagpenger.andre.ytelser.aap

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.EksternTopicMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import java.time.LocalDateTime

internal class AapMottak(
    rapidsConnection: RapidsConnection,
) : EksternTopicMottak() {
    override val topic = "aap.api-intern-hendelse-v1"
    override val system = "aap-api-intern"

    companion object {
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

    override fun JsonMessage.parseEvent(): AnnenYtelseEndret {
        val ident = this["ident"].textValue()
        val tidspunkt = LocalDateTime.now()

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = topic),
        )
    }
}
