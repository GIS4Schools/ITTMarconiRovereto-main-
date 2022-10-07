//intermediario tra il worker e processo principale dato che non si possono creare Worker da file .ts
//questo codice non fa altro che eseguire un file .ts di cui il percorso è passato tramite workerData.workerPath
//questo file non viene usato in produzione
const { workerData } = require("worker_threads")
require('ts-node').register()
require(workerData.workerPath)