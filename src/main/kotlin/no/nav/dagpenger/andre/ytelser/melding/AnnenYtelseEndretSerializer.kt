package no.nav.dagpenger.andre.ytelser.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

internal object AnnenYtelseEndretSerializer {
    const val EVENT_NAME = "annen_ytelse_endret"

    fun toJsonMessage(event: AnnenYtelseEndret): JsonMessage {
        val felter =
            mutableMapOf<String, Any>(
                "ident" to event.ident,
                "tema" to event.tema,
                "tidspunkt" to event.tidspunkt,
                "kilde" to mapOf("system" to event.kilde.system, "topic" to event.kilde.topic),
            )
        felter += event.detaljer.toMap()
        return JsonMessage.newMessage(EVENT_NAME, felter)
    }
}
