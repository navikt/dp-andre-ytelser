package no.nav.dagpenger.andre.ytelser.melding

internal data class UforetrygdDetaljer(
    val resultat: String,
    val uforegrad: Int?,
) : AnnenYtelseEndret.Detaljer {
    override fun toMap(): Map<String, Any> =
        buildMap {
            put("resultat", resultat)
            if (uforegrad != null) put("uforegrad", uforegrad)
        }
}
