package no.nav.dagpenger.andre.ytelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.andre.ytelser.aap.AapMottak
import no.nav.dagpenger.andre.ytelser.barnepensjon.BarnepensjonMottak
import no.nav.dagpenger.andre.ytelser.foreldrepenger.ForeldrepengerMottak
import no.nav.dagpenger.andre.ytelser.institusjon.InstitusjonMottak
import no.nav.dagpenger.andre.ytelser.sykmelding.SykmeldingMottak
import no.nav.dagpenger.andre.ytelser.uforetrygd.UforetrygdMottak
import no.nav.helse.rapids_rivers.RapidApplication

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }
    App.start()
}

internal object App : RapidsConnection.StatusListener {
    private val rapidsConnection =
        RapidApplication.create(Configuration.config).also {
            AapMottak(it)
            ForeldrepengerMottak(it)
            SykmeldingMottak(it)
            InstitusjonMottak(it, Configuration.config.getValue("INSTITUSJON_TOPIC"))
            BarnepensjonMottak(it)
            UforetrygdMottak(it)
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        val extraTopics = Configuration.config["KAFKA_EXTRA_TOPIC"] ?: "(ingen)"
        log.info { "Starter dp-andre-ytelser — lytter på ekstra topics: $extraTopics" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        log.info { "Stopper dp-andre-ytelser" }
    }
}
