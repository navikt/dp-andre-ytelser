# dp-andre-ytelser — AI-kontekst

Dette dokumentet beskriver applikasjonens formål, arkitektur og viktige
beslutninger for AI-assistenter som arbeider i dette repoet.

## Formål

`dp-andre-ytelser` er en Rapids & Rivers-app som lytter på Kafka-topics fra
andre NAV-team og normaliserer varslene til et felles format på
Dagpenger-rapiden (`teamdagpenger.rapid.v1`).

**Nåværende status:** Kun logging — publisering til rapiden er ikke aktivert.

## Arkitektur

```
teamforeldrepenger.vedtak-ekstern  ──┐
                                     ├──▶ dp-andre-ytelser ──▶ teamdagpenger.rapid.v1
tsm.sykmeldinger ────────────────────┘         (logging only, foreløpig)
```

- Ekstra Kafka-topics konfigureres via `KAFKA_EXTRA_TOPIC` (kommaseparert)
- R&R-meldinger fra rapiden filtreres bort med `forbid("@event_name")`
- `Configuration.kt` inneholder alle defaults — ingen env-vars nødvendig i nais.yaml

## Kildetopics

Se `docs/ytelseskilder.md` for full analyse. Oppsummert:

| Topic | Eier | Format | Felt vi bruker |
|-------|------|--------|----------------|
| `teamforeldrepenger.vedtak-ekstern` | fp-abakus | JSON | `personidentifikator`, `tidspunkt`, `tema` |
| `tsm.sykmeldinger` | syk-inn-api (tsm) | JSON via Jackson | `sykmelding.pasient.fnr`, `sykmelding.metadata.mottattDato` |

## Tema-enum

`Tema.kt` mapper kildesystemets råverdier til norske navn:

| Kilde-verdi | Tema-enum | Fra topic |
|-------------|-----------|-----------|
| `FOR` | `FORELDREPENGER` | vedtak-ekstern |
| `OMS` | `OMSORGSPENGER` | vedtak-ekstern |
| `FRI` | `FRISINN` | vedtak-ekstern |
| *(topic)* | `SYKMELDING` | tsm.sykmeldinger |

## Planlagt event-format

```json
{
  "@event_name": "andre_ytelse_mottatt",
  "ident": "12345678901",
  "tema": "FORELDREPENGER",
  "tidspunkt": "2026-04-20T10:00:00Z",
  "kilde": { "system": "fp-abakus", "topic": "teamforeldrepenger.vedtak-ekstern" }
}
```

## Viktige konvensjoner

- **Logging av PII:** Aldri i vanlig logg. I sikkerlogg maskeres fnr: `ident.take(6) + "*****"`
- **Mottakere heter `*Mottak`** ikke `*River` (team Dagpenger-konvensjon)
- **Publisering ikke aktivert** — ikke legg til `context.publish()` uten beslutning
- **Ukjente tema-verdier** logges som `warn` og hoppes over — ikke kast exception

## Fil-struktur

```
src/main/kotlin/.../
├── App.kt                          # Entry point, App-objekt
├── Configuration.kt                # Kafka/R&R defaults
├── Tema.kt                         # Enum for normaliserte tema-verdier
├── foreldrepenger/
│   └── ForeldrepengerMottak.kt     # Lytter på vedtak-ekstern
└── sykmelding/
    └── SykmeldingMottak.kt         # Lytter på tsm.sykmeldinger
```

## ACL-status

| Topic | IAC-repo | Status |
|-------|----------|--------|
| `teamforeldrepenger.vedtak-ekstern` | navikt/fp-iac | ✅ Merget (PR#37) |
| `tsm.sykmeldinger` | navikt/tsm-? | ❌ Ikke påbegynt |
| `teamdagpenger.rapid.v1` | navikt/dagpenger-iac | ✅ readwrite |
