package no.nav.dagpenger.andre.ytelser.melding

import java.time.LocalDateTime

internal data class AnnenYtelseEndret(
    val ident: String,
    val tema: String,
    val tidspunkt: LocalDateTime,
    val kilde: Kilde,
    val detaljer: Detaljer = Detaljer.Tom,
) {
    fun toLogString(): String =
        "tema=$tema, tidspunkt=$tidspunkt, kilde=${kilde.system}" +
            detaljer.toMap().let { if (it.isNotEmpty()) ", detaljer=$it" else "" }

    fun toSikkerLoggString(): String = "ident=${ident.take(6)}*****, ${toLogString()}"

    data class Kilde(
        val system: String,
        val topic: String,
    )

    sealed interface Detaljer {
        fun toMap(): Map<String, Any>

        data object Tom : Detaljer {
            override fun toMap(): Map<String, Any> = emptyMap()
        }
    }
}
