# Forslag: `annen_ytelse_endret` på dagpenger-rapiden

## TL;DR

`dp-andre-ytelser` lytter på Kafka-topics fra andre team og publiserer et minimalt
varselsevent på dagpenger-rapiden. Konsumenter slår opp detaljer selv. Først ute:
foreldrepenger.

```json
{
  "@event_name": "annen_ytelse_endret",
  "ident": "12345678901",
  "tema": "FOR",
  "tidspunkt": "2026-04-24T10:59:42+02:00",
  "kilde": {
    "system": "fp-abakus",
    "topic": "teamforeldrepenger.vedtak-ekstern"
  }
}
```

---

## Felt

| Felt | Type | Beskrivelse |
|------|------|-------------|
| `@event_name` | string | Alltid `"annen_ytelse_endret"` |
| `ident` | string | Fnr |
| `tema` | string | Rå tema-kode fra kilden (se tabell under) |
| `tidspunkt` | ISO-8601 | Når kildesystemet publiserte — **ikke** vedtakstidspunkt |
| `kilde.system` | string | Kildesystem |
| `kilde.topic` | string | Kafka-topic |

## Tema-koder fra `teamforeldrepenger.vedtak-ekstern`

| Tema-kode | Dekker ytelser |
|-----------|----------------|
| `FOR` | Foreldrepenger, svangerskapspenger |
| `OMS` | Pleiepenger, omsorgspenger, opplæringspenger |
| `FRI` | Frisinn |

> Engangsstønad publiseres **ikke** (ikke trekkpliktig).

## Hva fp-eventet IKKE inneholder

- ❌ Vedtaksreferanse / saksnummer
- ❌ Vedtaksperiode (fom/tom)
- ❌ Faktisk vedtakstidspunkt
- ❌ Beløp eller grad

Trenger dere noe av dette → kall Abakus REST `/hent-ytelse-vedtak` (slik
`dp-oppslag-ytelser` allerede gjør).
