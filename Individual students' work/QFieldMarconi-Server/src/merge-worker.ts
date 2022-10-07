import { workerData } from "worker_threads"
import { JSDOM } from "jsdom"
import FS from "fs-extra"

const userName: string = workerData.userName //workerData.userName contiene il nome utente della persona di cui unire il progetto
const projectPath = `./progetti/${userName}` //path del progetto da unire
const mergedPath = "./progetti/merged" //path del progetto unito
const oldLayerName = "qfield_marconi" //nome del layer (e del file contenente il layer) da unire
const newLayerName = userName //nome del layer (e del file contenente il layer) dopo essere stato unito

//non esiste un progetto con il nome passato, quindi esci
if(!FS.existsSync(projectPath)) {
    throw new Error("The passed username doesn't have any project associated with it.")
}

//se il file geopackage è già presente nella cartella del progetto unito, questo vuole dire che questo file era già stato unito in precedenza.
//per questo, non serve modificare il file .qgs del progetto unito (dato che contiene già link al file .gpkg), e basta copiare il nuovo file e le immagini
if(FS.existsSync(`${mergedPath}/${newLayerName}.gpkg`)) {
    FS.copyFileSync(`${projectPath}/${oldLayerName}.gpkg`, `${mergedPath}/${newLayerName}.gpkg`)
    if(FS.existsSync(`${projectPath}/DCIM`)) {
        FS.copySync(`${projectPath}/DCIM`, `${mergedPath}/DCIM`)
    }
    process.exit(0)
}

//copia il file geopackage nella cartella del progetto unito, dandogli un nome univoco all'utente
FS.copyFileSync(`${projectPath}/${oldLayerName}.gpkg`, `${mergedPath}/${newLayerName}.gpkg`)
//copia le immagini se esistono
if(FS.existsSync(`${projectPath}/DCIM`)) {
    FS.copySync(`${projectPath}/DCIM`, `${mergedPath}/DCIM`)
}

//carica i file .qgs del progetto unito e di quello da unire
let mergedProjectDom = new JSDOM(FS.readFileSync(`${mergedPath}/qfield_marconi.qgs`, {encoding: "utf-8"}), {contentType: "application/xml"})
let singleProjectDom = new JSDOM(FS.readFileSync(`${projectPath}/qfield_marconi.qgs`, {encoding: "utf-8"}), {contentType: "application/xml"})
let mergedProjectDoc: Document = mergedProjectDom.window.document
let singleProjectDoc: Document = singleProjectDom.window.document

/*
<layer-tree-group>
    <customproperties/>
    <layer-tree-layer id="Open_Street_Map_bef9b152_d324_414c_b865_cdcab1267fb0" source="crs=EPSG:3857&amp;format&amp;type=xyz&amp;url=http://tile.openstreetmap.org/%7Bz%7D/%7Bx%7D/%7By%7D.png&amp;zmax=18&amp;zmin=0" name="Open Street Map" patch_size="-1,-1" legend_split_behavior="0" expanded="0" providerKey="wms" checked="Qt::Checked" legend_exp="">
        <customproperties/>
    </layer-tree-layer>
    <custom-order enabled="0">
        <item>area_bosco_e0f3af01_3ce7_442f_acec_ac6d3eaf367f</item>
    </custom-order>
</layer-tree-group>
*/
//tag contenente la definizione dei layer del progetto
let mergedLayerTreeGroup: Element = mergedProjectDoc.querySelector("layer-tree-group")!
//tutti i tag dei layer
let singleTreeLayerAll: Element[] = Array.from(singleProjectDoc.querySelectorAll("layer-tree-layer"))
//prende solo il tag che nell'ID contiene il nome del layer da unire
let singleTreeLayer: Element = singleTreeLayerAll.filter(layer => layer.getAttribute("id")?.includes(oldLayerName))[0]

const oldId = singleTreeLayer.getAttribute("id") //vecchio id del layer
const newId = `${oldId}_${userName}` //nuovo id univoco per utente
//source: ./qfield_marconi_[nome utente].gpkg|layername?qfield_marconi
const layerSource = `./${newLayerName}.gpkg|${singleTreeLayer.getAttribute("source")!.split("|")[1]}`

//aggiorna gli attributi del layer per renderli univoci
singleTreeLayer.setAttribute("id", newId)
singleTreeLayer.setAttribute("source", layerSource)
singleTreeLayer.setAttribute("name", newLayerName)
mergedLayerTreeGroup.prepend(singleTreeLayer) //aggiunge in testa il layer modificato nel progetto unito

//aggiunge in testa il layer nell'ordine dei layer
//<item>[id layer]</item>
let singleCustomOrderItem = mergedProjectDoc.createElement("item")
singleCustomOrderItem.textContent = newId
mergedLayerTreeGroup.querySelector("custom-order")!.prepend(singleCustomOrderItem)

/*
<individual-layer-settings>
    <layer-setting id="qfield_marconi_11f96ec8_a22b_4c46_9ee9_962dd48ef9d0" tolerance="12" enabled="0" units="1" minScale="0" maxScale="0" type="1"/>
</individual-layer-settings>
*/
//tag contenente le impostazioni dei layer
let mergedLayerSettings: Element = mergedProjectDoc.querySelector("individual-layer-settings")!
//tag contenente le impostazioni del singolo layer da unire
let singleLayerSetting: Element = singleProjectDoc.querySelector(`layer-setting#${oldId}`)!

singleLayerSetting.setAttribute("id", newId)
mergedLayerSettings.prepend(singleLayerSetting)

/*
<legend updateDrawingOrder="true">
    <legendlayer drawingOrder="-1" name="qfield_marconi" open="true" checked="Qt::Checked" showFeatureCount="0">
        <filegroup open="true" hidden="false">
        <legendlayerfile layerid="qfield_marconi_11f96ec8_a22b_4c46_9ee9_962dd48ef9d0" visible="1" isInOverview="0"/>
        </filegroup>
    </legendlayer>
</legend>
*/
//tag contenente la legenda dei layer
let mergedLegend: Element = mergedProjectDoc.querySelector("legend[updateDrawingOrder]")!
//tag contenente la singola entry nella legenda del layer da unire
let singleLegendLayer: Element = singleProjectDoc.querySelector(`legendlayer[name="${oldLayerName}"]`)!

singleLegendLayer.setAttribute("name", newLayerName)
singleLegendLayer.querySelector("legendlayerfile")!.setAttribute("layerid", newId)
mergedLegend.prepend(singleLegendLayer)

/*
<projectlayers>
    <maplayer simplifyMaxScale="1" simplifyAlgorithm="0" refreshOnNotifyEnabled="0" geometry="Point" minScale="100000000" styleCategories="AllStyleCategories" simplifyDrawingTol="1" refreshOnNotifyMessage="" readOnly="0" wkbType="Point" maxScale="0" simplifyDrawingHints="0" autoRefreshTime="0" labelsEnabled="0" autoRefreshEnabled="0" simplifyLocal="1" hasScaleBasedVisibilityFlag="0" type="vector">
        <id>qfield_marconi_11f96ec8_a22b_4c46_9ee9_962dd48ef9d0</id>
        <datasource>./qfield_marconi.gpkg|layername=qfield_marconi</datasource>
        <keywordList>
            <value></value>
        </keywordList>
        <layername>qfield_marconi</layername>
        ...
    </maplayer>
</projectlayers>
*/
//tag contenente tutte le definizioni dei layer sulla mappa
let mergedMapLayers: Element = mergedProjectDoc.querySelector("projectlayers")!
//tutti i tag dei layer della mappa
let singleMapLayerAll: Element[] = Array.from(singleProjectDoc.querySelectorAll("maplayer"))
//prende solo il tag contenente un tag <id> con contenuto pari all'ID del layer da unire
let singleMapLayer: Element = singleMapLayerAll.filter((mapLayer: Element) => mapLayer.querySelector("id")!.textContent === oldId)[0]

singleMapLayer.querySelector("id")!.textContent = newId
singleMapLayer.querySelector("datasource")!.textContent = layerSource
singleMapLayer.querySelector("layername")!.textContent = newLayerName
mergedMapLayers.prepend(singleMapLayer)

/*
<layerorder>
    <layer id="qfield_marconi_11f96ec8_a22b_4c46_9ee9_962dd48ef9d0"/>
</layerorder>
*/
//tag contenente l'ordine dei layer
let mergedLayerOrder: Element = mergedProjectDoc.querySelector("layerorder")!

//aggiunge il layer da unire all'ordine
let singleOrderEntry: Element = mergedProjectDoc.createElement("layer")
singleOrderEntry.setAttribute("id", newId)
mergedLayerOrder.prepend(singleOrderEntry)

//aggiorna il file .qgs del progetto unito con il nuovo file XML
FS.writeFileSync(`${mergedPath}/qfield_marconi.qgs`, mergedProjectDom.serialize(), {encoding: "utf-8"})

process.exit(0)