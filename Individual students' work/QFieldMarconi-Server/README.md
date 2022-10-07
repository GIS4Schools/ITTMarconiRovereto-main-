# GrandeProgettoGis
### Il grande server per il grande progetto di GIS

## Requisiti
Node.js 14 o superiore, 16 consigliata.

## Setup per sviluppo
Dopo aver clonato la repository, eseguire ```npm install``` per installare tutte le dependency e ```npm run dev``` per avviare un dev server con riavvii automatici ad ogni modifica nel codice.

## Setup per produzione
Dopo aver clonato la repository, basta eseguire ```npm run build``` e ```npm run start``` per avviare il server sulla porta 3000.  

## Struttura del progetto
```
+-- progetti                --> contiene i progetti caricati, oltre al progetto di default e quello unito
|   +-- import              --> la cartella del progetto di default
|   +-- merged              --> la certella del progetto unito
|   +-- progetti.json       --> tiene traccia dei progetti caricati (alla prima esecuzione è vuoto)
|
+-- src                     --> codice dell'applicazione
|   +-- app.ts              --> codice del server, entry point dell'applicazione
|   +-- merge-worker.ts     --> codice che esegue l'unione dei progetti
|   +-- merge-worker.js     --> alias Javascript per il Worker scritto in Typescript
|                           
+-- static                  --> tutti i file serviti staticamente dall'applicazione
|   +-- favicon.ico
|   +-- input.css           --> sorgente stili
|   +-- output.css          --> generato da Tailwind in compliazione
|   +-- qfield-marconi.apk  --> app mobile
|
+-- views                   --> template per pagine HTML
|   +-- index.hbs           --> la pagina principale
|
+-- dist.js                 --> script per copiare i file all'esecuzione del comando npm run dist
```

## Lista degli script npm
```node
nodemon        // avvia l'applicazione con hot reload ad ogni modifica del codice
tailwind:watch // compila il CSS di Tailwind, con hot reload ad ogni modifica degli stili
dev            // unisce i comandi tailwind:watch e nodemon
build          // compila i file Typescript ed il CSS di Tailwind
start          // avvia l'applicazione
dist           // crea una cartella dist con solo i file necessari per l'esecuzione in produzione dell'applicazione
docker         // crea un'immagine docker dell'applicazione (nome elperenza/grande-progetto-gis:grande-immagine-gis)
docker:save    // salva l'immagine docker dell'applicazione in docker-image.tar
```
