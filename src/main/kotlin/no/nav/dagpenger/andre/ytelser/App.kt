package no.nav.dagpenger.andre.ytelser

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.andre.ytelser.foreldrepenger.ForeldrepengerMottak
import no.nav.helse.rapids_rivers.RapidApplication

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }

    RapidApplication
        .create(System.getenv())
        .apply {
            ForeldrepengerMottak(this)

            register(
                object : RapidsConnection.StatusListener {
                    override fun onStartup(rapidsConnection: RapidsConnection) {
                        log.info { "Starter dp-andre-ytelser" }
                    }

                    override fun onShutdown(rapidsConnection: RapidsConnection) {
                        log.info { "Stopper dp-andre-ytelser" }
                    }
                },
            )
        }.start()
}
