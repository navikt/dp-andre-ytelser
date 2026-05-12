package no.nav.dagpenger.andre.ytelser.melding

internal data class InstitusjonDetaljer(
    val oppholdId: Long,
    val hendelseId: Long,
    val type: String,
    val kilde: String,
) : AnnenYtelseEndret.Detaljer {
    override fun toMap(): Map<String, Any> =
        mapOf(
            "institusjon" to
                mapOf(
                    "oppholdId" to oppholdId,
                    "hendelseId" to hendelseId,
                    "type" to type,
                    "kilde" to kilde,
                ),
        )
}
