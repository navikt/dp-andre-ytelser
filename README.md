# dp-andre-ytelser

Lytter på vedtak om andre ytelser (foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger) og videresender notifikasjoner på dagpenger-rapiden.

## Topic

Konsumerer `teamforeldrepenger.vedtak-ekstern` via Rapids & Rivers `KAFKA_EXTRA_TOPIC`.

Meldingsformat inn:
```json
{"personidentifikator": "12345678901", "tidspunkt": "2026-04-17T08:30:00+02:00", "tema": "FOR"}
```

Publiserer `annen_ytelse_vedtatt` på rapiden:
```json
{"@event_name": "annen_ytelse_vedtatt", "ident": "12345678901", "tidspunkt": "...", "tema": "FOR"}
```

| Tema | Ytelser |
|------|---------|
| `FOR` | Foreldrepenger, Svangerskapspenger |
| `OMS` | Pleiepenger sykt barn, Pleiepenger nærstående, Omsorgspenger, Opplæringspenger |

## Komme i gang

```
./gradlew build
```

## Henvendelser

Spørsmål kan rettes mot #team-dagpenger-dev på Slack.
