package no.nav.dagpenger.andre.ytelser.melding

import java.time.LocalDateTime

internal data class AnnenYtelseEndret(
    val ident: String,
    val tema: String,
    val tidspunkt: LocalDateTime,
    val kilde: Kilde,
    val detaljer: Detaljer = Detaljer.Tom,
) {
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
