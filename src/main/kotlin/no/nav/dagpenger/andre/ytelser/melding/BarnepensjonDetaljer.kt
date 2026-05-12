package no.nav.dagpenger.andre.ytelser.melding

import java.time.LocalDate

internal data class BarnepensjonDetaljer(
    val vedtakId: Long,
    val type: String,
    val vedtaksdato: LocalDate,
    val virkningFom: LocalDate?,
) : AnnenYtelseEndret.Detaljer {
    override fun toMap(): Map<String, Any> =
        mapOf(
            "barnepensjon" to
                buildMap {
                    put("vedtakId", vedtakId)
                    put("type", type)
                    put("vedtaksdato", vedtaksdato)
                    if (virkningFom != null) put("virkningFom", virkningFom)
                },
        )
}
