# Copilot Instructions for dp-andre-ytelser

Lytter på vedtak om andre ytelser (foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger) og videresender notifikasjoner på dagpenger-rapiden.

## Build, Test, and Lint

```bash
./gradlew build
./gradlew test
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## Architecture

### Formål

Appen konsumerer eksterne Kafka-topics via Rapids & Rivers `KAFKA_EXTRA_TOPIC` og publiserer `annen_ytelse_vedtatt`-hendelser på dagpenger-rapiden.

### Pakkestruktur

Hver ytelseskilde har sin egen pakke med en Mottak-klasse:

```
src/main/kotlin/no/nav/dagpenger/andre/ytelser/
├── App.kt                              # Registrerer alle mottak
├── foreldrepenger/
│   └── ForeldrepengerMottak.kt         # teamforeldrepenger.vedtak-ekstern
├── sykepenger/                         # (fremtidig)
│   └── SykepengerMottak.kt
└── aap/                                # (fremtidig)
    └── AapMottak.kt
```

### Navnekonvensjon: Mottak, ikke River

Klasser som implementerer `River.PacketListener` skal hete **Mottak**, ikke River.
- ✅ `ForeldrepengerMottak`
- ❌ `ForeldrepengerRiver`

Dette er konsistent med dp-saksbehandling og resten av dagpenger-teamets konvensjoner.

### Legge til ny ytelseskilde

1. Opprett ny pakke under `no.nav.dagpenger.andre.ytelser.<kilde>/`
2. Lag en `<Kilde>Mottak`-klasse som implementerer `River.PacketListener`
3. Filtrer på `metadata.topic` for å kun behandle meldinger fra riktig topic
4. Publiser `annen_ytelse_vedtatt` med `kilde`-felt som identifiserer kilden
5. Registrer mottak i `App.kt`
6. Legg til topic i `KAFKA_EXTRA_TOPIC` i `.nais/nais.yaml` (kommaseparert)

### Hendelsesformat på rapiden

```json
{
  "@event_name": "annen_ytelse_vedtatt",
  "ident": "12345678901",
  "tidspunkt": "2026-04-17T08:30:00+02:00",
  "tema": "FOR",
  "kilde": "foreldrepenger"
}
```

| Felt | Beskrivelse |
|------|-------------|
| `ident` | Fødselsnummer |
| `tidspunkt` | Tidspunkt for vedtak |
| `tema` | Kodeverk-tema (FOR, OMS, etc.) |
| `kilde` | Hvilken ytelseskilde meldingen kom fra |

## Conventions

### Testing

- JUnit 5 med Kotest assertions
- `TestRapid` for å teste mottak med `sendTestMessage(melding, metadata)`
- Bruk `MessageMetadata(topic = ...)` for å simulere meldinger fra eksterne topics
- Tester ligger i samme pakkestruktur som prodkoden

### Kotlin & Build

- Kotlin JVM 21
- ktlint via `common` convention plugin i `buildSrc/`
- Gradle configuration cache aktivert

## Architecture

### Module Structure

- **modell**: Pure domain model with no external dependencies. Contains domain entities, state machines, and events.
- **mediator**: Application layer with API, database, and messaging. Orchestrates domain logic via mediator classes.
- **openapi**: API contract definitions. Models are generated from `saksbehandling-api.yaml` using Fabrikt plugin.
- **streams-consumer**: Kafka Streams consumers for external data.

### Oppdatering av AI-instruksjoner

Når en ny ytelseskilde legges til, oppdater denne filen slik at AI-assistenten kjenner til:
- Nye mottak-klasser og hvilke topics de lytter på
- Nye felter i `annen_ytelse_vedtatt`-hendelsen
- Eventuelle nye konvensjoner eller mønstre som ble etablert
