package no.nav.dagpenger.andre.ytelser

enum class Tema(
    val kildeTema: String,
) {
    FORELDREPENGER("FOR"),
    OMSORGSPENGER("OMS"),
    FRISINN("FRI"),
    SYKMELDING("SYKMELDING"),
    ;

    companion object {
        fun fraKildeTema(kildeTema: String): Tema? = entries.find { it.kildeTema == kildeTema }
    }
}
