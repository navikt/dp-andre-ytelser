package no.nav.dagpenger.andre.ytelser.institusjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.AbstractMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.InstitusjonDetaljer
import java.time.LocalDateTime

internal class InstitusjonMottak(
    rapidsConnection: RapidsConnection,
) : AbstractMottak() {
    override val topic = "team-rocket.institusjon-opphold-hendelser"
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

    override fun JsonMessage.parseEvent(): AnnenYtelseEndret {
        val ident = this["norskident"].textValue()
        val type = this["type"].textValue()
        val oppholdId = this["oppholdId"].longValue()
        val hendelseId = this["hendelseId"].longValue()
        val kilde = this["kilde"].textValue()
        val tidspunkt = LocalDateTime.now()

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = topic),
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
