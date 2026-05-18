package no.nav.dagpenger.andre.ytelser.barnepensjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.andre.ytelser.AbstractMottak
import no.nav.dagpenger.andre.ytelser.melding.AnnenYtelseEndret
import no.nav.dagpenger.andre.ytelser.melding.BarnepensjonDetaljer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class BarnepensjonMottak(
    rapidsConnection: RapidsConnection,
) : AbstractMottak() {
    override val topic = "etterlatte.vedtakshendelser"
    override val system = "etterlatte-behandling"

    companion object {
        const val TEMA = "EYB"
    }

    init {
        River(rapidsConnection)
            .precondition {
                it.forbid("@event_name")
                it.requireValue("sakstype", "BP")
            }.validate {
                it.requireKey("ident", "sakstype", "type", "vedtakId", "vedtaksdato")
                it.interestedIn("virkningFom")
            }.register(this)
    }

    override fun JsonMessage.parseEvent(): AnnenYtelseEndret {
        val ident = this["ident"].textValue()
        val type = this["type"].textValue()
        val vedtakId = this["vedtakId"].longValue()
        val vedtaksdato = LocalDate.parse(this["vedtaksdato"].textValue())
        val virkningFom = this["virkningFom"].takeUnless { it.isMissingNode }?.textValue()?.let { LocalDate.parse(it) }
        val tidspunkt = LocalDateTime.of(vedtaksdato, LocalTime.MIDNIGHT)

        return AnnenYtelseEndret(
            ident = ident,
            tema = TEMA,
            tidspunkt = tidspunkt,
            kilde = AnnenYtelseEndret.Kilde(system = system, topic = topic),
            detaljer =
                BarnepensjonDetaljer(
                    vedtakId = vedtakId,
                    type = type,
                    vedtaksdato = vedtaksdato,
                    virkningFom = virkningFom,
                ),
        )
    }
}
