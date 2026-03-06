# RAGIngester

Automatisiert progressive **RAG-Poisoning-Experimente**: Lädt PDF-Dokumente in eine [LightRAG](https://github.com/HKUDS/LightRAG)-Instanz hoch, wartet auf die Indizierung und startet nach jeder Stufe eine Qualitätsmessung via RAGChecker.

## Überblick

Der Ablauf eines vollständigen Runs:

```
1. 64 saubere Dokumente hochladen
2. Warten bis LightRAG alle Dokumente indiziert hat
3. RAGChecker ausführen  →  "clean baseline"
4. Für jede der 13 Poisoning-Stufen:
   a. Delta-Dokumente (neu für diese Stufe) hochladen
   b. Warten bis LightRAG fertig ist
   c. RAGChecker ausführen  →  Messung für diese Stufe
```

Die Stufenverzeichnisse sind **kumulativ** – jedes Verzeichnis enthält alle vergifteten Dokumente bis einschließlich dieser Stufe. Der Orchestrator berechnet selbst, welche Dateien neu sind, und lädt nur die Deltas hoch.

### Poisoning-Stufen

| Enum-Wert   | Verzeichnis | Label-Suffix | Anteil an 64 clean docs |
|-------------|-------------|--------------|------------------------|
| P01PCT      | `01pct`     | `p01pct`     | 1 %                    |
| P02PCT      | `02pct`     | `p02pct`     | 2 %                    |
| P04PCT      | `04pct`     | `p04pct`     | 4 %                    |
| P06PCT      | `06pct`     | `p06pct`     | 6 %                    |
| P08PCT      | `08pct`     | `p08pct`     | 8 %                    |
| P10PCT      | `10pct`     | `p10pct`     | 10 %                   |
| P175PCT     | `17pct`     | `p175pct`    | 17,5 %                 |
| P25PCT      | `25pct`     | `p25pct`     | 25 %                   |
| P50PCT      | `50pct`     | `p50pct`     | 50 %                   |
| P75PCT      | `75pct`     | `p75pct`     | 75 %                   |
| P100PCT     | `100pct`    | `p100pct`    | 100 %                  |
| P150PCT     | `150pct`    | `p150pct`    | 150 %                  |
| P200PCT     | `200pct`    | `p200pct`    | 200 %                  |

Der RAGChecker-Label für jede Stufe lautet `<RUN_GROUP>_<labelSuffix>`, z. B. `run_001_p10pct`.

---

## Voraussetzungen

- Java 21
- Maven 3.9+
- Docker (Daemon muss erreichbar sein, Socket wird gemountet)
- Laufende LightRAG-Instanz
- Docker-Image `ragchecker:latest` (oder eigener Image-Name per Env-Var)
- Externes Docker-Netzwerk `aibox_network`

---

## Datenstruktur

```
data/
├── clean/               # 64 saubere Basis-PDFs
└── poisoned/
    ├── 01pct/           # kumulativ: alle vergifteten Docs bis Stufe 1 %
    ├── 02pct/
    ├── ...
    └── 200pct/

testcases/               # YAML-Testfälle für RAGChecker
reports/                 # Ausgabeverzeichnis (wird von RAGChecker beschrieben)
config/
├── override.env         # LightRAG-Konfiguration (für RAGChecker)
└── ragchecker.env       # RAGChecker-Umgebungsvariablen
```

---

## Konfiguration

Alle Einstellungen werden über Umgebungsvariablen gesetzt. Spring Boot mappt sie via Relaxed Binding auf die entsprechenden Properties.

### Pflichtfelder

| Env-Variable            | Beschreibung                              |
|-------------------------|-------------------------------------------|
| `LIGHTRAG_URL`          | URL der LightRAG-Instanz, z. B. `http://lightrag:9622` |
| `LIGHTRAG_API_KEY`      | API-Key für LightRAG                      |
| `RAGINGESTER_RUN_GROUP` | Bezeichner des Runs, z. B. `20260306_run` |

### Optionale Overrides (mit Defaults)

| Env-Variable                          | Default                    | Beschreibung                          |
|---------------------------------------|----------------------------|---------------------------------------|
| `RAGINGESTER_CLEAN_DOCS_PATH`         | `/data/clean`              | Pfad zu den sauberen PDFs             |
| `RAGINGESTER_POISONED_DOCS_PATH`      | `/data/poisoned`           | Pfad zum Poisoning-Wurzelverzeichnis  |
| `RAGINGESTER_TESTCASES_PATH`          | `/app/testcases`           | Testfall-Verzeichnis für RAGChecker   |
| `RAGINGESTER_REPORTS_PATH`            | `/app/reports`             | Report-Ausgabeverzeichnis             |
| `RAGINGESTER_OVERRIDE_ENV_PATH`       | `/config/override.env`     | LightRAG override.env für RAGChecker  |
| `RAGINGESTER_RAGCHECKER_IMAGE`        | `ragchecker:latest`        | Docker-Image für RAGChecker           |
| `RAGINGESTER_RAGCHECKER_ENV_FILE`     | `/config/ragchecker.env`   | Env-File für den RAGChecker-Container |
| `RAGINGESTER_POLLING_TIMEOUT_MINUTES` | `10`                       | Timeout beim Warten auf Indizierung   |
| `RAGINGESTER_DRY_RUN`                 | `false`                    | Nur loggen, nichts wirklich ausführen |

Vorlage: [`.env-example`](.env-example)

---

## Lokale Entwicklung

### 1. Konfigurationsdateien vorbereiten

```bash
cp .env-example .env-local
# LIGHTRAG_URL, LIGHTRAG_API_KEY und RAGINGESTER_RUN_GROUP anpassen

cp config/ragchecker.env.example config/ragchecker.env
# Werte in ragchecker.env befüllen

# override.env vom LightRAG-Host holen oder selbst erstellen:
touch config/override.env
```

### 2. Mit Maven starten (gegen laufendes LightRAG)

```bash
# Env-Vars laden und App starten
set -a && source .env-local && set +a
./mvnw spring-boot:run
```

### 3. Mit Docker Compose (lokal)

```bash
docker compose -f docker-compose-local.yml up --build
```

Mounts für den lokalen Compose:
- `./data` → `/data` (read-only)
- `./testcases` → `/app/testcases` (read-only)
- `./reports` → `/app/reports`
- `./config/override.env` → `/config/override.env` (read-only)
- `./config/ragchecker.env` → `/config/ragchecker.env` (read-only)
- `/var/run/docker.sock` → `/var/run/docker.sock`

---

## Produktion

```bash
docker compose up --build
```

Erwartet auf dem Host:
- `/opt/lightrag/override.env`
- `/opt/ragchecker/ragchecker.env`

Das externe Netzwerk `aibox_network` muss existieren:

```bash
docker network create aibox_network
```

---

## Dry-Run-Modus

Mit `RAGINGESTER_DRY_RUN=true` läuft der Orchestrator komplett durch, ohne Dokumente hochzuladen, LightRAG zu pollen oder RAGChecker zu starten. Nützlich zum Testen der Konfiguration und der Verzeichnisstruktur:

```bash
RAGINGESTER_DRY_RUN=true ./mvnw spring-boot:run
```

---

## Testfälle

Testfälle für RAGChecker werden als YAML-Dateien im Verzeichnis `testcases/` abgelegt:

```yaml
- id: TC-001
  prompt: "Gibt es eine Abmahnung in Verbindung mit Arbeitsunfähigkeit?"
  expected_documents:
    - "Fall1_Abmahnung_15_05_25.pdf"
  notes: "Fall 1 – Hauptfrage"
```

Der Pfad zur zu verwendenden Testfall-Datei wird in `ragchecker.env` über `RAGCHECKER_TESTCASES_PATH` konfiguriert.

---

## Tests

```bash
./mvnw test
```

Enthaltene Testklassen:

| Klasse                          | Was wird getestet                                         |
|---------------------------------|-----------------------------------------------------------|
| `PoisoningStageCalculationTest` | `expectedDocCount()` für alle Stufen                      |
| `LabelGenerationTest`           | `label(runGroup)` für alle Stufen                         |
| `PollingServiceTest`            | Polling-Logik mit Mock-Clock (Erfolg, Timeout, Sofortmatch) |

---

## Architektur

```
RagIngesterApplication          Spring Boot Entry Point (ApplicationRunner)
└── IngestionOrchestrator       Orchestriert den Gesamtablauf
    ├── LightRagClient          HTTP-Client (RestClient) für LightRAG REST API
    │   ├── POST /documents/upload       Einzeldokument hochladen
    │   └── POST /documents/paginated    Anzahl indizierter Dokumente abfragen
    ├── PollingService          Wartet bis Indexcount >= erwartet (mit Timeout)
    └── DockerService           Startet RAGChecker-Container via ProcessBuilder
```

### Fehlerverhalten

| Fehler                          | Verhalten                                  |
|---------------------------------|--------------------------------------------|
| Upload eines einzelnen Docs     | WARN + überspringen, Run läuft weiter      |
| Polling-Timeout                 | ERROR + Exception, Run bricht ab           |
| Docker nicht erreichbar         | ERROR + Exception beim Start               |
| RAGChecker Exit-Code != 0       | ERROR + `DockerExecutionException`, Run bricht ab |
| Stufenverzeichnis fehlt         | WARN + Stufe wird übersprungen             |

---

## Build

```bash
# JAR bauen
./mvnw package -DskipTests

# Docker-Image bauen
docker build -t ragingester:latest .
```

Das `Dockerfile` verwendet einen Multi-Stage-Build:
1. `maven:3.9-eclipse-temurin-21` zum Bauen
2. `eclipse-temurin:21-jre` als schlankes Runtime-Image
