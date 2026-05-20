package no.nav.dagpenger.andre.ytelser.institusjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.EksternTopicMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.InstitusjonDetaljer
import java.time.LocalDateTime

internal class InstitusjonMottak(
    rapidsConnection: RapidsConnection,
) : EksternTopicMottak() {
    override val topics = setOf("team-rocket.institusjon-opphold-hendelser", "team-rocket.institusjon-opphold-hendelser-q2")
    override val system = "inst2"

    companion object {
        const val TEMA = "INST"
    }

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate { it.requireKey("norskident", "type", "oppholdId", "hendelseId", "kilde") }
            .register(this)
    }

    override fun JsonMessage.parseEvent(actualTopic: String): AnnenYtelseEndret {
        val ident = this["norskident"].stringValue()
        val type = this["type"].stringValue()
        val oppholdId = this["oppholdId"].longValue()
        val hendelseId = this["hendelseId"].longValue()
        val kilde = this["kilde"].stringValue()
        val tidspunkt = LocalDateTime.now()

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = actualTopic),
            detaljer =
                InstitusjonDetaljer(
                    oppholdId = oppholdId,
                    hendelseId = hendelseId,
                    type = type,
                    kilde = kilde,
                ),
        )
    }
}
