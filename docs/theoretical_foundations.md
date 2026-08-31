# Fondamenti Teorici e Modelli Matematici - NEPE

Questo documento raccoglie la teoria matematica, statistica e finanziaria che costituisce il cuore dell'**Inference Engine** di **NEPE**. 

Il software abbandona qualsiasi approccio soggettivo, basando le proprie stime su modelli stocastici consolidati nella letteratura scientifica sul trading sportivo e sulla teoria delle probabilità.

---

## 1. Expected Goals (xG) ed Euristica sui Tiri

### 1.1 Concetto
Gli **Expected Goals (xG)** misurano la qualità delle occasioni da gol create e concesse da una squadra. Nelle metriche avanzate ufficiali, l'xG viene calcolato tracciando la posizione precisa di ogni tiro sul campo. 

Poiché i dataset gratuiti pubblicamente disponibili (come Football-Data) non forniscono coordinate spaziali ma solo il conteggio dei tiri totali e dei tiri in porta, NEPE adotta una **formula euristica di regressione** per stimare l'xG:

$$\text{xG} = (\text{Tiri in Porta} \times 0.30) + ((\text{Tiri Totali} - \text{Tiri in Porta}) \times 0.05)$$

* **Tiri in Porta (conversione 30%):** Un tiro nello specchio della porta ha una probabilità media del 30% di trasformarsi in gol.
* **Tiri Fuori (conversione 5%):** Un tiro fuori bersaglio o bloccato riflette comunque una situazione d'attacco pericolosa creata, con un valore atteso del 5%.

---

## 2. Modello di Poisson Semplificato (Forze delle Squadre)

### 2.1 Distribuzione di Poisson
La distribuzione di Poisson modella il numero di eventi indipendenti che si verificano in un intervallo di tempo fisso, dato un tasso medio costante di accadimento $\lambda$:

$$P(X = k) = \frac{\lambda^k e^{-\lambda}}{k!}$$

Nel calcio, l'evento è la segnatura di un gol nell'arco di una partita (90 minuti).

### 2.2 Calcolo dei Goal Attesi ($\lambda$ e $\mu$)
Per una partita tra la squadra di casa ($H$) e la squadra ospite ($A$), l'Inference Engine calcola i tassi di gol attesi pre-match ($\lambda_H$ per $H$ e $\mu_A$ per $A$) valutando la forza offensiva ($\alpha$) e difensiva ($\beta$) su un campione stocastico di partite:

#### Algoritmo di Selezione del Campione:
1. **Analisi della Stagione Corrente:** Se nella stagione corrente la squadra ha giocato $M \ge 10$ partite, vengono prese in considerazione **tutte le $M$ partite** disputate finora nella stagione attiva.
2. **Fallback Inter-Stagione per Campione Minimo ($N_{\text{min}} = 10$):** Se la squadra ha giocato $M < 10$ partite nella stagione corrente, il sistema recupera dal DB le ultime $10 - M$ partite giocate dalla squadra nella **stagione precedente**, fermandosi esattamente a 10 partite complessive.

#### Ponderazione Temporale (Recency Weighting):
Le metriche di xG non vengono calcolate con una semplice media aritmetica, ma con una **media pesata con decadimento temporale**:

$$\bar{xG}_{\text{segnati}, i} = \frac{\sum_{k=1}^K w_k \cdot \text{xG}_{\text{segnati}, k}}{\sum_{k=1}^K w_k}, \quad \bar{xG}_{\text{subiti}, i} = \frac{\sum_{k=1}^K w_k \cdot \text{xG}_{\text{subiti}, k}}{\sum_{k=1}^K w_k}$$

* **Peso di Recency $w_k$:** Assegna valore massimo alle partite più recenti e valore decrescente man mano che ci si allontana nel tempo (es. decadimento esponenziale $w_k = e^{-\lambda_{\text{decay}} \cdot t_k}$).
* **Sconto Inter-Stagionale $\gamma$:** Per eventuali partite recuperate dalla stagione precedente, il peso viene ulteriormente moltiplicato per $\gamma = 0.70$ per riflettere le discontinuità di rosa e guida tecnica tra due annate.

#### Formule delle Forze:
$$\alpha_i = \frac{\bar{xG}_{\text{segnati}, i}}{\bar{xG}_{\text{campionato}}}, \quad \beta_i = \frac{\bar{xG}_{\text{subiti}, i}}{\bar{xG}_{\text{campionato}}}$$

$$\lambda_H = \alpha_H \times \beta_A \times \text{Home Advantage}$$
$$\mu_A = \alpha_A \times \beta_H \times \text{Away Disadvantage}$$

* **Home Advantage:** Rapporto tra i gol segnati in casa e in trasferta nell'intero campionato.

---

## 3. Correzione di Dixon-Coles (1997)

### 3.1 Limite del Modello di Poisson Standard
La distribuzione di Poisson assume l'indipendenza stocastica tra i gol della squadra di casa e della squadra ospite: $P(X=x, Y=y) = P(X=x) \cdot P(Y=y)$. 

Tuttavia, nei dati reali del calcio esiste una **dipendenza per i punteggi bassi**: i risultati 0-0 e 1-1 si verificano con una frequenza significativamente maggiore rispetto a quanto previsto da Poisson, mentre i risultati 1-0 e 0-1 si verificano meno frequentemente (atteggiamenti tattici prudenti sull'0-0 o ricerca del pareggio sull mezza partita).

### 3.2 La Formula di Dixon-Coles
Dixon e Coles (1997) introducono un fattore di correzione $\tau(x, y)$ che modifica la probabilità congiunta solo per i punteggi $x \le 1$ e $y \le 1$:

$$P(X=x, Y=y) = \tau(x, y) \times \frac{\lambda^x e^{-\lambda}}{x!} \times \frac{\mu^y e^{-\mu}}{y!}$$

Dove il fattore di aggiustamento $\tau(x, y)$ è così definito:

$$\tau(x, y) = \begin{cases} 
1 - \lambda \mu \rho & \text{se } (x,y) = (0,0) \\
1 + \mu \rho & \text{se } (x,y) = (1,0) \\
1 + \lambda \rho & \text{se } (x,y) = (0,1) \\
1 - \rho & \text{se } (x,y) = (1,1) \\
1 & \text{in tutti gli altri casi}
\end{cases}$$

* **$\rho$ (Rho):** Coefficiente di dipendenza inter-squadra specificato per il campionato (valore di default $-0.12$). Un $\rho < 0$ incrementa la probabilità dello 0-0 e dell'1-1 e riduce quella dell'1-0 e 0-1.

### 3.3 Come Approssimare o Calcolare il Valore di $\rho$ (Rho)

Esistono tre metodi per ottenere il parametro $\rho$:

#### 1. Valori Benchmark da Letteratura Accademica
Nel paper fondamentale di Dixon & Coles (1997), la stima calcolata sui dati storici dei campionati inglesi risultava $\rho \approx -0.13$ (nello specifico $-0.128 \pm 0.038$). 
Studi empirici successivi sui principali campionati europei (Serie A, Premier League, La Liga, Bundesliga) confermano che per i massimi campionati continentali $\rho$ ricade stabilmente nell'intervallo:
$$\rho \in [-0.15, -0.09]$$
Il valore di default globale impostato in NEPE (**$-0.1200$**) rappresenta un ottimo stimatore *off-the-shelf* applicabile a qualsiasi competizione prima di aver calibrato i dati.

#### 2. Calcolo Esatto tramite Maximum Likelihood Estimation (MLE)
Disponendo nel database dello storico dei risultati $(x_k, y_k)$ di una competizione (ad esempio l'intera stagione precedente composta da 380 partite) e dei relativi tassi $(\lambda_k, \mu_k)$, $\rho$ si ottiene massimizzando la funzione di log-verosimiglianza:

$$\ln L(\rho) = \sum_{k=1}^K \ln \tau(x_k, y_k; \lambda_k, \mu_k, \rho)$$

Poiché per tutti i match con punteggi superiori a 1 $\tau(x, y) = 1$ (e quindi $\ln 1 = 0$), la sommatoria si riduce **esclusivamente alle partite finite con punteggio basso (0-0, 1-0, 0-1, 1-1)**:

$$\ln L(\rho) = \sum_{k \in (0,0)} \ln(1 - \lambda_k \mu_k \rho) + \sum_{k \in (1,0)} \ln(1 + \mu_k \rho) + \sum_{k \in (0,1)} \ln(1 + \lambda_k \rho) + \sum_{k \in (1,1)} \ln(1 - \rho)$$

Trattandosi di una funzione concava mono-dimensionale definita per $\rho \in (-1, 1)$, il valore ottimo $\rho^*$ si calcola in frazioni di secondo in Java tramite una semplice ricerca monodimensionale (es. metodo di Brent, Newton-Raphson o bisezione sulla derivata prima $\frac{d \ln L}{d\rho} = 0$).

#### 3. Approssimazione Empirica Rapida (Metodo dei Momenti)
Se $f_{\text{oss}}(0,0)$ è la frequenza reale di 0-0 osservata nella stagione passata e $f_{\text{pois}}(0,0) = \frac{1}{K}\sum e^{-\lambda_k - \mu_k}$ è la frequenza attesa da Poisson puro:
$$\rho \approx \frac{1 - \frac{f_{\text{oss}}(0,0)}{f_{\text{pois}}(0,0)}}{\bar{\lambda} \cdot \bar{\mu}}$$
Questa formula offre una stima rapida ed intuitiva senza richiedere algoritmi iterativi.

---

## 4. Teoria del Valore Atteso (Expected Value - EV) nel Betting Exchange

### 4.1 Definizione di Expected Value
Nel trading finanziario e sportivo, l'**Expected Value (EV)** rappresenta il guadagno o la perdita media teorica a lungo termine per ogni scommessa piazzata. Una giocata è a "valore positivo" ($\text{EV} > 0$) quando la quota offerta dal mercato è superiore alla "quota equa" (l'inverso della probabilità reale $1/P$).

### 4.2 Posizione Punta (Back) con Commissione
Nel Punta (Back), si scommette sull'accadimento dell'evento. Detraendo la commissione dell'exchange ($\text{comm}$) dalle vincite nette:

$$\text{EV}_{\text{Punta, Comm}} = P \times (K_{\text{Back}} - 1) \times (1 - \text{comm}) - (1 - P)$$

* $P$: Probabilità stimata dal modello di Dixon-Coles.
* $K_{\text{Back}}$: Quota Punta di mercato.

### 4.3 Posizione Banca (Lay) e Rischio sulla Responsabilità
Nel Banca (Lay), si agisce come il bookmaker, bancando l'esito. Se l'evento NON si verifica, si incassa la stake del backer (al netto della commissione). Se l'evento SI verifica, si paga la responsabilità (liability): $\text{Liability} = K_{\text{Lay}} - 1$.

* **EV Banca Classico (su 1 unità vinta):**
  $$\text{EV}_{\text{Banca, Comm}} = (1 - P) \times (1 - \text{comm}) - P \times (K_{\text{Lay}} - 1)$$

* **EV Banca Tarato sul Rischio ($\text{EV}_{\text{Rischio}}$):**
  Per confrontare correttamente un Lay a quota $1.20$ con un Lay a quota $8.00$, l'EV deve essere rapportato al **capitale reale a rischio** ($\text{Liability}$):

  $$\text{EV}_{\text{Banca, Rischio}} = \frac{\text{EV}_{\text{Banca, Comm}}}{K_{\text{Lay}} - 1}$$

  Questo indice esprime il **Rendimento sul Capitale Investito (ROI%)** per la posizione di bancata.

---

## 5. Dinamica Temporale e Modificatori Live (Live Engine)

### 5.1 Decadimento Temporale (Time-Decay)
Con il trascorrere del tempo, la quantità attesa di gol residui cala in modo proporzionale ai minuti rimanenti prima del 90°:

$$\lambda_{\text{residuo}}(t) = \lambda_{\text{iniziale}} \times \left( \frac{90 - t}{90} \right) \quad \text{per } t \in [0, 90]$$

Per $t \ge 90$, $\lambda_{\text{residuo}} = 0$ (clamp a zero per gestione del rischio).

### 5.2 Superiorità/Inferiorità Numerica (Cartellini Rossi)
L'espulsione di un giocatore altera l'equilibrio tattico in modo non lineare. Per ogni cartellino rosso subito da una squadra ($C$):
* **Squadra in inferiorità:** Il suo tasso d'attacco diminuisce del 30% cumulativo: $\lambda \times 0.70^C$.
* **Squadra in superiorità:** Il suo tasso d'attacco aumenta del 30% cumulativo: $\lambda \times 1.30^C$.

### 5.3 Modificatore Motivazionale Must-Win nel Secondo Tempo
Nel secondo tempo ($t \ge 45$), se una squadra ha la necessità assoluta di vincere (`must_win = true`) e si trova in situazione di pareggio o svantaggio:
* Incrementa la spinta offensiva del 25%: $\lambda_{\text{must\_win}} \times 1.25$.
* Aumenta la vulnerabilità difensiva del 20%: $\mu_{\text{avversario}} \times 1.20$.
