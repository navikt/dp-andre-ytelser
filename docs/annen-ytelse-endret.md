# Forslag: `annen_ytelse_endret` på dagpenger-rapiden

## TL;DR

`dp-andre-ytelser` lytter på Kafka-topics fra andre team og publiserer et minimalt
varselsevent på dagpenger-rapiden. Konsumenter slår opp detaljer selv. Først ute:
foreldrepenger og sykmelding.

```json
{
  "@event_name": "annen_ytelse_endret",
  "ident": "12345678901",
  "tema": "FOR",
  "tidspunkt": "2026-04-24T10:59:42",
  "kilde": {
    "system": "fp-abakus",
    "topic": "teamforeldrepenger.vedtak-ekstern"
  }
}
```

For sykmelding er eventet utvidet med en `sykmelding`-blokk:

```json
{
  "@event_name": "annen_ytelse_endret",
  "ident": "12345678901",
  "tema": "SYM",
  "tidspunkt": "2026-04-24T10:59:42",
  "kilde": {
    "system": "tsm",
    "topic": "tsm.sykmeldinger"
  },
  "sykmelding": {
    "id": "abc-123",
    "aktivitet": [
      { "type": "AKTIVITET_IKKE_MULIG", "fom": "2026-04-15", "tom": "2026-04-30" },
      { "type": "GRADERT",              "fom": "2026-05-01", "tom": "2026-05-15" }
    ]
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
| `tidspunkt` | `LocalDateTime` | Når kildesystemet publiserte — **ikke** vedtakstidspunkt. Normalisert til Europe/Oslo |
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

Trenger dere noe av dette → send et behov på rapiden og la
`dp-oppslag-ytelser` hente det fra fp-abakus (REST `/hent-ytelse-vedtak`).

---

## Tema-koder fra `tsm.sykmeldinger`

| Tema-kode | Dekker |
|-----------|--------|
| `SYM` | Sykmelding |

### Filter

Vi publiserer kun for sykmeldinger med `validation.status == "OK"` — altså
godkjente sykmeldinger. AVVIST og PENDING filtreres bort.

> Topicen er log-compacted med `sykmelding.id` som key. En sykmelding kan
> derfor passere flere ganger (PENDING → OK etter manuell behandling). Vi
> publiserer hver gang status er OK; konsumenter bør være idempotente på
> `sykmelding.id`.

### Felt i `sykmelding`-blokken

| Felt | Type | Beskrivelse |
|------|------|-------------|
| `sykmelding.id` | string | Stabil ID fra TSM — bruk for idempotens |
| `sykmelding.aktivitet[]` | array | Aktivitetsperioder fra sykmeldingen |
| `sykmelding.aktivitet[].type` | string | `AKTIVITET_IKKE_MULIG` \| `GRADERT` \| `AVVENTENDE` \| `BEHANDLINGSDAGER` \| `REISETILSKUDD` |
| `sykmelding.aktivitet[].fom` | `LocalDate` | Periode fra |
| `sykmelding.aktivitet[].tom` | `LocalDate` | Periode til |

### Hva sykmelding-eventet IKKE inneholder (bevisst)

- ❌ `grad` (gradert sykmelding-prosent)
- ❌ `antallBehandlingsdager`
- ❌ `medisinskArsak` / `arbeidsrelatertArsak` (helsedata, GDPR art. 9)
- ❌ `innspillTilArbeidsgiver` (fritekst)
- ❌ `behandler`, `sykmelder`, `arbeidsgiver`, `medisinskVurdering`

Dette er et trigger-signal, ikke faktagrunnlag. dp-saksbehandling skal trigge
utredning når en sykmelding er mottatt — ikke automatisk fatte beslutninger ut
fra detaljene.

> TSM tilbyr per i dag ingen oppslags-API for sykmeldinger. Trenger dere mer
> informasjon enn det som ligger i eventet, må dere ta det opp med team
> sykmelding.
