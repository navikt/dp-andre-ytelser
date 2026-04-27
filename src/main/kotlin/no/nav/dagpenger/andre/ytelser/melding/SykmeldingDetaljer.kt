package no.nav.dagpenger.andre.ytelser.melding

import java.time.LocalDate

internal data class SykmeldingDetaljer(
    val id: String,
    val aktivitet: List<Aktivitet>,
) : AnnenYtelseEndret.Detaljer {
    override fun toMap(): Map<String, Any> =
        mapOf(
            "sykmelding" to
                mapOf(
                    "id" to id,
                    "aktivitet" to aktivitet.map { mapOf("type" to it.type, "fom" to it.fom, "tom" to it.tom) },
                ),
        )

    data class Aktivitet(
        val type: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
