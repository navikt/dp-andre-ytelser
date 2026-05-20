package no.nav.dagpenger.andre.ytelser.uforetrygd

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.EksternTopicMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.UforetrygdDetaljer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class UforetrygdMottak(
    rapidsConnection: RapidsConnection,
) : EksternTopicMottak() {
    override val topics = setOf("pensjondeployer.uforevedtak-dagpenger", "pensjon-q2.uforevedtak-dagpenger-q2")
    override val system = "pensjon-pen"

    companion object {
        const val TEMA = "UFO"
    }

    init {
        River(rapidsConnection)
            .precondition { it.forbid("@event_name") }
            .validate {
                it.requireKey("personId", "virkningsdato", "resultat")
                it.interestedIn("uforegrad")
            }.register(this)
    }

    override fun JsonMessage.parseEvent(actualTopic: String): AnnenYtelseEndret {
        val ident = this["personId"].stringValue()
        val virkningsdato = LocalDate.parse(this["virkningsdato"].stringValue())
        val resultat = this["resultat"].stringValue()
        val uforegrad = this["uforegrad"].let { if (it.isMissingNode || it.isNull) null else it.intValue() }

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = LocalDateTime.of(virkningsdato, LocalTime.MIDNIGHT),
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = actualTopic),
            detaljer =
                UforetrygdDetaljer(
                    resultat = resultat,
                    uforegrad = uforegrad,
                ),
        )
    }
}
