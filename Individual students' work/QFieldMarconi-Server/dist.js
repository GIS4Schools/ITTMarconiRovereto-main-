//script per creare una cartella con tutti i file necessari per eseguire l'app
const FS = require("fs-extra")

if(FS.existsSync("./dist")) {
    FS.removeSync("./dist")
}

FS.mkdirSync("./dist")
FS.copyFileSync("./package.json", "./dist/package.json")
FS.copyFileSync("./package-lock.json", "./dist/package-lock.json")

FS.mkdirSync("./dist/build")
FS.copySync("./build", "./dist/build")

FS.mkdirSync("./dist/static")
FS.copySync("./static", "./dist/static")

FS.mkdirSync("./dist/views")
FS.copySync("./views", "./dist/views")

FS.mkdirSync("./dist/progetti")
FS.copySync("./progetti/import", "./dist/progetti/import")
FS.copySync("./progetti/merged", "./dist/progetti/merged")
FS.writeFileSync("./dist/progetti/progetti.json", "[]")