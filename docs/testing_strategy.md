# Strategia di Testing e Qualità del Codice - NEPE

Questo documento definisce la strategia di testing, la copertura della qualità e i criteri di validazione software per **NEPE**.

L'obiettivo è garantire che il motore matematico di previsione, la logica di calcolo dell'Expected Value (EV) e le componenti di persistenza/importazione siano totalmente immuni da regressioni e pronti per l'analisi statica tramite **SonarQube**.

---

## 1. Piramide dei Test e Livelli di Copertura

Il sistema adotta la classica piramide dei test, sfruttando le peculiarità dell'**Architettura Esagonale** per eseguire la stragrande maggioranza dei test a livello unitario super-veloce.

```text
        / \
       /   \        Test E2E / GUI (JavaFX TestFX) - 10%
      /-----\
     /       \      Test di Integrazione (Spring Boot + MariaDB/Testcontainers) - 30%
    /---------\
   /           \    Test Unitari Puri (Domain Core + JUnit 5 + AssertJ) - 60%
  /-------------\
```

---

## 2. Dettaglio dei Livelli di Testing

### 2.1 Test Unitari Puri del Domain Core (Obiettivo Copertura > 95%)
Il **Domain Core** (`org.nepe.inference.domain`, `org.nepe.match.domain`, ecc.) è scritto in Java puro ed è completamente disaccoppiato da Spring, JPA o MariaDB.

* **Strumenti:** JUnit 5, AssertJ, Fake/Stub creati a mano in Java. **Nessun uso di Mockito o librerie di mocking dinamico**, garantendo test reali, leggibili e manutenibili.
* **Velocità:** Esecuzione in pochi millisecondi.
* **Componenti sotto Test:**
  1. **`PoissonModelTest`:** Verificare che le distribuzioni di Poisson per $\lambda$ e $\mu$ restituiscano matrici di probabilità valide (la somma di tutte le probabilità della matrice $10 \times 10$ deve tendere a 1.0).
  2. **`DixonColesModelTest`:** Verificare che l'applicazione del fattore $\tau(x, y)$ con parametro $\rho = -0.12$ aumenti correttamente la probabilità dei punteggi (0,0) e (1,1) e riduca (1,0) e (0,1) rispetto a Poisson standard.
  3. **`EvCalculatorTest`:** Verificare la correttezza matematica dell'EV Back detratto dalla commissione e dell'EV Lay tarato sul rischio ($\text{EV}_{\text{Rischio}} = \text{EV}_{\text{Comm}} / (K_{\text{Lay}} - 1)$).
  4. **`LiveEngineModifiersTest`:** Testare l'effetto cumulativo dei cartellini rossi ($0.70^C$ e $1.30^C$), il decadimento temporale al 90° minuto (con clamp a zero) e lo sbilanciamento Must-Win nel secondo tempo.

---

### 2.2 Test di Integrazione degli Adattatori (Obiettivo Copertura > 80%)
Gli adattatori collegano il dominio all'infrastruttura esterna (Database MariaDB e File System CSV).

* **Strumenti:** `@SpringBootTest`, `@DataJpaTest`, H2 Database (in-memory per test veloci), JUnit 5.
* **Componenti sotto Test:**
  1. **`CsvParserAdapterTest`:**
     * Verificare il parsing di file CSV reali e fittizi di Football-Data con date a 2 e 4 cifre (`DD/MM/YY` vs `DD/MM/YYYY`).
     * Verificare la gestione delle colonne statistiche mancanti (fallback su gol reali per xG).
     * Verificare che se viene rilevata una squadra sconosciuta venga sollevata l'eccezione `AliasMappingRequiredException`.
  2. **`MatchRepositoryAdapterTest`:**
     * Verificare la persistenza delle entità tramite i mapper (`Match` $\leftrightarrow$ `MatchJpaEntity`).
     * Verificare la logica di **Upsert e Overwrite Protection**: controllare che la re-importazione di un match con `is_manually_edited = 1` salvi le quote ma preservi il punteggio modificato a mano dall'utente.
     * Verificare che la vista `v_matches_details` restituisca correttamente i nomi completi di team e competizioni.

---

### 2.3 Test degli Scenario e casi limite di Trading (Regression Testing)
Set di test di accettazione automatizzati che simulano interi flussi operativi del trader.

* **Scenario 1: Fallback Inter-Stagione con Decadimento $\gamma$**
  * *Setup:* Una squadra ha disputato solo 4 partite nella stagione corrente ($M=4$).
  * *Esecuzione:* Il servizio richiede un campione $N=10$.
  * *Verifica:* Il motore deve recuperare le ultime 6 partite della stagione precedente e applicare il fattore di deprezzamento $\gamma = 0.70$ ai loro valori di xG.
* **Scenario 2: Transizioni della Console Live**
  * *Setup:* Partita 0-0 al 60° minuto.
  * *Evento:* Gol della squadra ospite + Cartellino rosso per la squadra di casa.
  * *Verifica:* Il motore ricalcola la matrice per i 30 minuti residui applicando contemporaneamente Time-Decay ($30/90$), sbilanciamento da rosso ($0.70$ casa / $1.30$ ospite) ed eventuale flag Must-Win.

---

## 3. Analisi Statica e CI/CD con SonarQube Cloud & GitHub Actions

Per automatizzare il controllo della qualità del codice, il progetto viene collegato a **SonarQube Cloud (SonarCloud)** tramite una pipeline di **GitHub Actions** scatenata ad ogni `git push` o `pull request`.

### 3.1 Workflow GitHub Actions (`.github/workflows/build.yml`)
1. **Trigger:** Ad ogni push sui branch principali.
2. **Build & Test:** GitHub Actions esegue `mvn clean verify` generando il report di copertura del codice tramite **JaCoCo**.
3. **SonarCloud Scan:** La pipeline invia il codice sorgente e i report JaCoCo a SonarCloud utilizzando la chiave segreta `SONAR_TOKEN`.

### 3.2 Quality Gate SonarQube
* **Coverage globale minima:** $> 80\%$ su tutto il progetto (con $> 95\%$ sul pacchetto `inference.domain`).
* **Cognitive Complexity:** Massimo 15 per singolo metodo.
* **Duplicazione del codice:** $< 3\%$.
* **Bugs & Vulnerabilità (Security):** 0 per la categoria Blocker/Critical.
* **Eccezioni:** Nessun blocco `catch` vuoto o soppressione implicita di eccezioni.

---

## 4. Esecuzione dei Test nel Flusso di Lavoro Locale

In locale, i test si eseguono senza alcuna dipendenza da mocking esterni:

```bash
# Esecuzione dei test unitari del Domain Core (veloci)
mvn test -Dtest=*UnitTest

# Esecuzione dei test di integrazione e persistenza
mvn test -Dtest=*IntegrationTest

# Generazione report JaCoCo locale per SonarCloud
mvn clean verify
```
