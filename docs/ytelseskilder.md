# Ytelseskilder — analyse og kontrakter

`dp-andre-ytelser` lytter på Kafka-topics fra andre NAV-team og videresender
normaliserte varsler på Dagpenger-rapiden.

---

## 1. Foreldrepenger, svangerskapspenger og omsorgspenger

**Topic:** `teamforeldrepenger.vedtak-ekstern`
**Eier:** Team Foreldrepenger (`fp-abakus`)
**Kafka-pool:** `nav-prod` / `nav-dev`
**Format:** JSON (Jackson, ingen Avro)

### Kontraktstruktur

```json
{
  "personidentifikator": "12345678901",
  "tidspunkt": "2026-04-17T08:30:00+02:00",
  "tema": "FOR"
}
```

| Felt | Type | Beskrivelse |
|------|------|-------------|
| `personidentifikator` | `String` | Fødselsnummer eller D-nummer |
| `tidspunkt` | `OffsetDateTime` | Tidspunkt vedtaket ble fattet |
| `tema` | `String` | Se tabell under |

### Tema-verdier

| Råverdi | Vår `Tema`-enum | Innhold |
|---------|-----------------|---------|
| `FOR` | `FORELDREPENGER` | Foreldrepenger og svangerskapspenger |
| `OMS` | `OMSORGSPENGER` | Pleiepenger, omsorgspenger, opplæringspenger |
| `FRI` | `FRISINN` | Frisinn (utfaset ordning, publiseres fremdeles) |

> **NB:** Engangsstønad (`ENGANGSTØNAD`) publiseres også til dette topicet,
> men mappes til tema `FOR`. Engangsstønad er ikke en løpende ytelse og er
> derfor ikke relevant for dagpenger-samordning.

### Tilgang

PR mot `navikt/fp-iac` — legg til `dp-andre-ytelser: read` under topicet
`teamforeldrepenger/vedtak-ekstern/topic.yaml`.

**Referanse-PR:** navikt/fp-iac#37 (merged)

---

## 2. Sykmeldinger

**Topic:** `tsm.sykmeldinger`
**Eier:** Team Sykmelding (`tsm`) — appen `syk-inn-api`
**Kafka-pool:** `nav-prod` / `nav-dev`
**Format:** JSON (Jackson) via `tsm-sykmelding-input`-biblioteket
**Repo:** [navikt/tsm-sykmelding-input](https://github.com/navikt/tsm-sykmelding-input)

### Kontraktstruktur (forenklet)

```json
{
  "metadata": { "msgId": "...", "type": "SYKMELDING" },
  "sykmelding": {
    "id": "uuid",
    "type": "DIGITAL",
    "pasient": {
      "fnr": "12345678901",
      "navn": null,
      "kontaktinfo": []
    },
    "metadata": {
      "mottattDato": "2026-04-17T08:30:00+02:00",
      "genDate": "2026-04-17T08:00:00+02:00",
      "avsenderSystem": { "navn": "EPJ", "versjon": "1.0" }
    },
    "aktivitet": [
      { "type": "GRADERT", "fom": "2026-04-01", "tom": "2026-04-30", "grad": 50 },
      { "type": "AKTIVITET_IKKE_MULIG", "fom": "2026-05-01", "tom": "2026-05-15" }
    ]
  },
  "validation": { "status": "OK", "rules": [] }
}
```

Vi bruker kun:

| Felt (JSON-sti) | Vår bruk |
|-----------------|----------|
| `sykmelding.pasient.fnr` | Ident for kobling mot dagpengemottaker |
| `sykmelding.metadata.mottattDato` | Tidspunkt sykmeldingen ble mottatt av NAV |

Vi henter **ikke** ut medisinsk informasjon (`medisinskVurdering`, diagnose,
årsak). Den som trenger detaljer slår opp direkte hos TSM.

### Sykmelding er ikke en ytelse

En sykmelding er et legeerklæringsdokument. Personen kan velge å **ikke** bruke
sykmeldingen. Det betyr at `sykmelding_mottatt` ikke nødvendigvis betyr at
personen mottar sykepenger — kun at legen har erklært arbeidsudyktighet.

### Relevans for dagpenger

En dagpengemottaker som blir sykmeldt skal ikke motta dagpenger i
sykmeldingsperioden. Vi ønsker å varsle saksbehandlere slik at de kan
vurdere videre behandling og forhindre feilutbetaling.

### Tilgang

PR mot `navikt/tsm-sykmelding-input` eller via `#tsm`-kanalen på Slack.
Legg til `dp-andre-ytelser: read` for topicet `tsm.sykmeldinger`.

> **Status:** Ikke påbegynt — se [Neste steg](#neste-steg)

---

## Event-format på rapiden (`annen_ytelse_endret`)

Foreldrepenger publiseres som:

```json
{
  "@event_name": "annen_ytelse_endret",
  "@id": "uuid-v4",
  "@opprettet": "2026-04-23T14:00:00Z",

  "ident": "12345678901",
  "tema": "FOR",
  "tidspunkt": "2026-04-20T10:00:00",

  "kilde": {
    "system": "fp-abakus",
    "topic": "teamforeldrepenger.vedtak-ekstern"
  }
}
```

**Designprinsipp:** dp-andre-ytelser er en ren signalformidler — vi videresender at
noe har skjedd. Konsumenter slår opp detaljer selv (Abakus, Gosys, m.m.).

- `tema` er **rå kildekode** fra fp-abakus (`FOR`/`OMS`/`FRI`) — ikke konvertert.
- `tidspunkt` er publiseringstid fra kilden (ikke vedtakstidspunkt),
  normalisert til `LocalDateTime` i Europe/Oslo for å gjøre konsumering enklere.
- Sykmelding er ennå ikke aktivert — venter på ACL og format-bekreftelse fra TSM.

Se [`andre-ytelse-mottatt.md`](andre-ytelse-mottatt.md) for full kontrakt og rationale.

---

## Neste steg

- [ ] Få tilgang til `tsm.sykmeldinger` (PR mot tsm-iac)
- [ ] Verifiser at SykmeldingMottak mottar meldinger i dev
- [ ] Aktiver publisering for sykmelding når format er bekreftet av TSM
- [ ] Avklar med `dp-saksbehandling-frontend` hva de trenger fra eventet
