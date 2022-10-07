import Express from "express"
import Router from "express-promise-router"
import FS from "fs"
import FSP from "fs/promises"
import Multer from "multer"
import AdmZip from "adm-zip"
import { Worker } from "worker_threads"

/**
 * Defnisce i dati di un progetto caricato.
 */
 interface Project {
    /**
     * Il nome utente del creatore.
     */
    userName: string,
    /**
     * Il nome (formattato) del creatore.
     */
    fmtUserName: string,
    /**
     * La data di creazione.
     */
    creationDate: string,
    /**
     * La data dell'ultima modifica.
     */
    lastModified: string,
    /**
     * Il numero di revisione.
     */
    revisionNumber: number
}

const PORT = process.env.PORT || 3000
const CWD = process.cwd()

const server = Express()
const router = Router()
const attachment = Multer()

server.set("view engine", "hbs")
server.use(router)
server.use(Express.static(`${CWD}/static`)) //rende i file nella directory /static raggiungibili staticamente

//Index
router.get("/", async (req, res) => {

    //file JSON contenente informazioni sui progetti caricati
    let projects: Project[] = JSON.parse(await FSP.readFile("./progetti/progetti.json", {encoding: "utf-8"}))
    res.render("index", {projects: projects})
})

//URL di download file
router.get("/download", (req, res) => {

    let userName = req.query.user as string //il nome dell'utente contenuto nella query

    //se l'utente ha già un progetto, restituisco quello
    if(FS.existsSync(`./progetti/${userName}`)) {

        res.attachment(`${userName}.zip`) //nome dell'allegato mandato
        //aggiunge la cartella ad uno zip e la manda al client
        let zip = new AdmZip()
        zip.addLocalFolder(`./progetti/${userName}`, `${userName}`)
        res.send(zip.toBuffer())

        console.log(`Inviato progetto ${userName}.zip a ${req.ip}`)
        return
    }

    //altrimenti, restituisco il progetto di default
    res.attachment(`${userName || "import"}.zip`)
    let zip = new AdmZip()
    zip.addLocalFolder(`./progetti/import`, `${userName || "import"}`)
    res.send(zip.toBuffer())

    console.log(`Inviato progetto di default con nome ${userName || "import"}.zip a ${req.ip}`)
})

let workerQueue: string[] = [] //coda contenente i nomi dei progetti da unire che verranno poi processati sequenzialmente da Worker
let isWorkerRunning = false //indica se c'è un worker per unire progetti in esecuzione al corrente

/**
 * Istanzia un nuovo worker per unire i progetti nella coda dei progetti da unire (workerQueue)
 */
function instantiateWorker() {
    
    //se non ci sono altri progetti da unire, termina l'esecuzione
    let userName: string | undefined = workerQueue.shift()
    if(!userName) {
        return
    }

    //inizializza un worker per unire il primo progetto nella coda
    let worker = new Worker(`${__dirname}/merge-worker.js`, {workerData: {workerPath: `${__dirname}/merge-worker.ts`, userName: userName}})
    worker.on("error", (err: Error) => console.log(`Impossibile unire il progetto di ${userName}: \n${err}`))
    worker.on("exit", (code: number) => {
        if(code === 0) {
            console.log(`Progetto di ${userName} unito con successo`)
        }
        isWorkerRunning = false
        instantiateWorker() //prova da istanziare un nuovo worker per unire il prossimo progetto nella coda
    })

    isWorkerRunning = true
}

//URL per caricare progetti
router.post("/upload", attachment.single("project"), async (req, res) => {

    let file = req.file //il file allegato
    let userName = req.query.user as string //il nome dell'utente contenuto nella query

    //controlli che il file allegato e l'utente siano validi
    if(file === undefined) {
        res.status(400).send("Nessun file allegato.")
        return
    }
    if(!file.originalname.endsWith(".zip")) {
        res.status(400).send("Il file allegato non è un archivio .zip.")
        return
    }
    if(userName === undefined) {
        res.status(400).send("Nessun nome utente passato.")
        return
    }
    if(userName === "import" || userName === "merged" || !userName.includes(".")) {
        res.status(400).send("Il nome utente passato non è accettabile.")
        return
    }

    let zip = new AdmZip(file!.buffer)

    //validazione zip inviato (se qfield_marconi.gpkg e qfield_marconi.qgs esistono, valido)
    if(!zip.getEntries().find(value => value.name === "qfield_marconi.gpkg") ||
    !zip.getEntries().find(value => value.name === "qfield_marconi.qgs")) {
        
        res.status(400).send("L'archivio allegato non contiene i file necessari.")
        return
    }

    //controlla se l'archivio contiene dentro direttamente i file o la cartella del progetto, poi lo estrae nella posizione giusta
    if(zip.getEntry(userName) === null) {
        zip.extractAllTo(`./progetti/${userName}`, true)
    } else {
        zip.extractAllTo("./progetti/", true)
    }

    //cerca se esiste già una voce per questo progetto nel file json
    let projects: Project[] = JSON.parse(await FSP.readFile("./progetti/progetti.json", {encoding: "utf-8"}))
    let index = projects.findIndex(value => value.userName === userName)

    //data attuale (per ultima modifica)
    let date: string = new Intl.DateTimeFormat("it-IT").format(Date.now()) //formattazione GG/MM/AAAA

    if(index != -1) { //se si, aggiorna la data dell'ultima modifica ed il numero revisione
        projects[index].lastModified = date
        projects[index].revisionNumber++

    } else { //se no, crea una nuova voce
        //formatta il nome con nome e congome separati da uno spazio e con l'iniziale maiuscola
        let [firstName, lastName] = userName.split(".")
        let formattedName = `${firstName.charAt(0).toUpperCase() + firstName.slice(1)} ${lastName.charAt(0).toUpperCase() + lastName.slice(1)}`

        let project: Project = {
            userName: userName,
            fmtUserName: formattedName,
            creationDate: date,
            lastModified: date,
            revisionNumber: 1
        }

        projects.push(project)
        projects.sort((a, b) => a.userName.localeCompare(b.userName)) //sort in ordine alfabetico
    }

    //aggiorna file json
    await FSP.writeFile("./progetti/progetti.json", JSON.stringify(projects), {encoding: "utf-8"})

    res.send("Progetto caricato!")

    console.log(`Caricato progetto inviato dall'utente ${userName} da ${req.ip}`)

    workerQueue.push(userName) //aggiunge questo progetto in coda alla coda dei progetti da unire
    //se non c'è alcun worker per unire i progetti in esecuzione, istanziane uno
    if(!isWorkerRunning) {
        instantiateWorker()
    }
})

//stopgap per essere sicuri sia la versioe giusta
router.get("/v", (req, res) => {
    res.send("1.0.3a")
})

// :)
router.get("/amogus", (req, res) => {
    res.status(418).send("<img src='https://c.tenor.com/9swGRuA4tNYAAAAC/sussy-the-rock.gif'> SUSSY")
})

//ascolta per connessioni
server.listen(PORT, () => console.log(`Server avviato su http://localhost:${PORT}`))