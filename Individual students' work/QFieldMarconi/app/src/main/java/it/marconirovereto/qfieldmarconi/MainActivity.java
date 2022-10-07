package it.marconirovereto.qfieldmarconi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    // Bottone nella pagina principale per accedere alla pagina delle informazioni
    FloatingActionButton fab;

    /* Bottoni:
        btn_import:     importare i dati dal web Service
        btn_export:     per esportare i dati sul Web Service
        open_qfield:    aprire l'applicazione Qfield
     */
    Button btn_import, btn_export, open_qfield, open_website;

    ImageView imageProfile, imageQField;

    // Controllare i permessi
    // ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /* Stringhe:
        UNIQUE_URL:     Link del Web Service
        NAME_SURNAME:   Nome e cognome dell'utente loggato
     */
    // private static String UNIQUE_URL = "http://www.192.168.125.41.sslip.io:3000/", NAME_SURNAME;
    // private static String UNIQUE_URL = "http://www.192.168.88.251.sslip.io:3000/", NAME_SURNAME;
    private static String UNIQUE_URL = "https://gis.andtrentini.it/", NAME_SURNAME;

    /* Vettori:
        FILENAMES:      Nome di tutti i file da importare ed esportare
        informazioni:   Vettore contenente i campi del file [info]
     */
    private static String[] FILENAMES = {"area_bosco.cpg", "area_bosco.dbf", "area_bosco.prj", "area_bosco.shp", "area_bosco.shx", "qfield_marconi.gpkg", "qfield_marconi.qgs", "DCIM"}, informazioni;

    /* Cartelle:
        CARTELLA_PRIVATA:   /storage/emulated/0/Android/data/it.marconirovereto.qfieldmarconi/files
        CARTELLA_FILES:     /storage/emulated/0/Android/data/it.marconirovereto.qfieldmarconi/[NAME_SURNAME]
        CARTELLA_PUBBLICA:  /storage/emulated/0/Documents/it.marconirovereto.qfieldmarconi
     */
    private static File CARTELLA_PRIVATA, CARTELLA_PUBBLICA, CARTELLA_FILES;

    // Messaggio Pop-Up per Download e Upload
    private ProgressDialog pDialog;

    // File .txt con le informazioni dell'utente loggato
    private File info;

    // ArrayList per inserire i file mancanti durante l'esportazione
    private ArrayList<String> fileMancanti = new ArrayList<>();

    /* Booleani:
        statusDownload: true    → Nessun problema durante il Download
                        false   → C'è stato un problema durante il Download
        statusUpload:   true    → Nessun problema durante l'Upload
                        false   → C'è stato un problema durante l'Upload
        folderDCIM:     true    → La cartella DCIM esiste durante l'esportazione
                        false   → La cartella DCIM non esiste durante l'esportazione
     */
    private boolean statusDownload, statusUpload, folderDCIM = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inizializzazioni delle variabili
        fab = findViewById(R.id.goToInfo);
        btn_import = findViewById(R.id.btn_importa);
        btn_export = findViewById(R.id.btn_esporta);
        open_qfield = findViewById(R.id.btn_apri_qfield);
        open_website = findViewById(R.id.btn_apri_sito_web);
        CARTELLA_PRIVATA = getExternalFilesDir(null);
        info = new File(CARTELLA_PRIVATA, "info.txt");
        getInfos();
        NAME_SURNAME = informazioni[3].split("@")[0];
        CARTELLA_PUBBLICA = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/it.marconirovereto.qfieldmarconi/");
        CARTELLA_FILES = new File(CARTELLA_PUBBLICA, NAME_SURNAME);
        deleteRecursive(new File(CARTELLA_PUBBLICA, "files"));

        // Controllo dei permessi
        havePermission();

        // Impostazione dei bottoni [btn_import] e [btn_export]
        if (!isImportati()) {
            btn_import.setEnabled(true);
            btn_export.setEnabled(false);
        } else {
            btn_import.setEnabled(false);
            btn_export.setEnabled(true);
        }

        // Impostazione dei metodi "onClick()"
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });
        btn_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                havePermission();
                importa();
            }
        });
        btn_export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                havePermission();
                esporta();
            }
        });
        open_qfield.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apri(2);
            }
        });
        open_website.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apri(3);
            }
        });
        imageProfile = findViewById(R.id.imageView2);
        imageProfile.setVisibility(View.INVISIBLE);
        try {
            Glide.with(this).load(informazioni[5]).into(imageProfile);
            imageQField = findViewById(R.id.imageView);
            imageQField.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            imageQField.setVisibility(View.INVISIBLE);
                            imageProfile.setVisibility(View.VISIBLE);
                            return true;
                        case MotionEvent.ACTION_UP:
                            imageQField.setVisibility(View.VISIBLE);
                            imageProfile.setVisibility(View.INVISIBLE);
                            return true;
                    }
                    return false;
                }
            });
        } catch (ArrayIndexOutOfBoundsException e) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            try {
                File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "it.marconirovereto.qfieldmarconi");
                creaCartella(f1);
                creaCartella(CARTELLA_PUBBLICA);
                creaCartella(CARTELLA_FILES);
            } catch (Exception exception) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Attenzione");
                builder.setMessage(exception.getMessage());
                builder.setPositiveButton("OK", null);
                builder.show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Sono obbligatori i permessi", Toast.LENGTH_LONG).show();
            apri(1);
            finish();
        }
    }

    /**
     * Controlla se l'utente ha accettato i permessi
     */
    private void havePermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        try {
            File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "it.marconirovereto.qfieldmarconi");
            creaCartella(f1);
            creaCartella(CARTELLA_PUBBLICA);
            creaCartella(CARTELLA_FILES);
        } catch (Exception exception) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Attenzione");
            builder.setMessage(exception.getMessage());
            builder.setPositiveButton("OK", null);
            builder.show();
        }
    }

    /**
     * Inserisce nel vettore [informazioni] i dati ricevuti da Google
     */
    private void getInfos() {
        try {
            Scanner input = new Scanner(info);
            informazioni = input.nextLine().split("&");
            info.delete();
        } catch (Exception exception) {
        }
    }

    /**
     * Avvio della procedura di Download per i dati
     */
    private void importa() {
        // Bisogna importare i dati dal Web Service
        new DownloadFileFromURL().execute();
    }

    /**
     * Avvio della procedura di Upload per i dati
     */
    private void esporta() {
        try {
            int nrFile = 8;
            ArrayList<String> fileRidondanti = new ArrayList<>();
            File[] files = CARTELLA_FILES.listFiles();
            for (int i = 0; i < files.length; i++) {
                switch (files[i].getName()) {
                    case "DCIM":
                        folderDCIM = true;
                        nrFile++;
                        break;
                }
            }
            if (files.length <= nrFile) {
                resetArraylist();
                for (int i = 0; i < files.length; i++) {
                    switch (files[i].getName()) {
                        case "area_bosco.cpg":
                            fileMancanti.remove("area_bosco.cpg");
                            break;
                        case "area_bosco.dbf":
                            fileMancanti.remove("area_bosco.dbf");
                            break;
                        case "area_bosco.prj":
                            fileMancanti.remove("area_bosco.prj");
                            break;
                        case "area_bosco.shp":
                            fileMancanti.remove("area_bosco.shp");
                            break;
                        case "area_bosco.shx":
                            fileMancanti.remove("area_bosco.shx");
                            break;
                        case "qfield_marconi.gpkg":
                            fileMancanti.remove("qfield_marconi.gpkg");
                            break;
                        case "qfield_marconi.qgs":
                            fileMancanti.remove("qfield_marconi.qgs");
                            break;
                        case "DCIM":
                            break;
                        default:
                            fileRidondanti.add(files[i].getName());
                            break;
                    }
                }
                String tmpM = "";
                Iterator iter = fileMancanti.iterator();
                while (iter.hasNext()) {
                    String elementM = (String) iter.next();
                    tmpM += ">" + elementM;
                }
                if (!fileRidondanti.isEmpty()) {
                    String tmpR = "";
                    Iterator iter2 = fileRidondanti.iterator();
                    while (iter2.hasNext()) {
                        String elementR = (String) iter2.next();
                        tmpR += "\n" + elementR;
                    }
                    throw new Exception(tmpR);
                }
                if (!tmpM.equals("")) {
                    throw new IndexOutOfBoundsException(tmpM);
                }
            } else {
                for (int i = 0; i < files.length; i++) {
                    switch (files[i].getName()) {
                        case "area_bosco.cpg":
                        case "area_bosco.dbf":
                        case "area_bosco.prj":
                        case "area_bosco.shp":
                        case "area_bosco.shx":
                        case "qfield_marconi.gpkg":
                        case "qfield_marconi.qgs":
                        case "DCIM":
                            break;
                        default:
                            fileRidondanti.add(files[i].getName());
                            break;
                    }
                }
                if (!fileRidondanti.isEmpty()) {
                    String tmpR = "";
                    Iterator iter2 = fileRidondanti.iterator();
                    while (iter2.hasNext()) {
                        String elementR = (String) iter2.next();
                        if (elementR.equals("qfield_marconi.gpkg-wal") || elementR.equals("qfield_marconi.marconi.gpkg-shm")) {
                            throw new IllegalArgumentException("È necessario salvare il progetto QField");
                        }
                        tmpR += "\n" + elementR;
                    }
                    throw new Exception(tmpR);
                }
            }
            inviaFiles(null);
        } catch (IndexOutOfBoundsException ex) {
            String[] msg = ex.getMessage().split(">");
            String output = "";
            for (int i = 1; i < msg.length; i++) {
                output += msg[i] + "\n";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Mancano i seguenti file");
            builder.setMessage(output);
            builder.setPositiveButton("Richiedi i file mancanti", (dialog, which) -> richiediFileMancanti(ex.getMessage()));
            builder.setNegativeButton("Annulla", null);
            builder.show();
        } catch (IllegalArgumentException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Attenzione");
            builder.setMessage(ex.getMessage());
            builder.setPositiveButton("Come salvare?", (dialog, which) -> popupSalvare());
            builder.setNegativeButton("Annulla", null);
            builder.show();
        } catch (Exception ex) {
            if (!ex.getMessage().equals("Attempt to get length of null array")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Verranno eliminati i seguenti file");
                builder.setMessage(ex.getMessage());
                builder.setPositiveButton("Elimina", (dialog, which) -> inviaFiles(ex.getMessage()));
                builder.setNegativeButton("Annulla", null);
                builder.show();
            } else {
                Toast.makeText(getApplicationContext(), "La cartella dei file ora esiste. Riprovare.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Visualizza il Pop-Up con la guida per salvare i dati
     */
    private void popupSalvare() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Come salvare il progetto");
        builder.setMessage("Quando viene aperto un progetto su QField, appaiono 2 file temporanei. Sull'applicazione bisognerà salvare le modifiche selezionando uno dei progetti default proposti da QField");
        builder.setPositiveButton("Apri Qfield", (dialog, which) -> apri(2));
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    /**
     * Prepara il file [NAME_SURNAME].zip da esportare
     *
     * @param message File residui da eliminare
     */
    private void inviaFiles(String message) {
        if (message != null) {
            eliminaFiles();
        } else {
            new File(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip").delete();
            try {
                new ZipFile(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip").addFiles(Arrays.asList(CARTELLA_FILES.listFiles()));
                new ZipFile(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip").addFolder(new File(CARTELLA_FILES, "DCIM"));
            } catch (ZipException e) {
                e.printStackTrace();
            }
            new UploadFileFromURL().execute();
        }
    }

    /**
     * Vengono ripristinati i file mancanti dalla cartella [CARTELLA_PUBBLICA]
     *
     * @param message File da ripristinare
     */
    private void richiediFileMancanti(String message) {
        String[] files = message.split(">");
        File[] dir = CARTELLA_PRIVATA.listFiles();
        boolean fileZIP = false;
        for (int i = 0; i < dir.length && !fileZIP; i++) {
            if (dir[i].getName().equals(NAME_SURNAME + ".zip")) {
                fileZIP = true;
            }
        }
        if (fileZIP) {
            try {
                for (int i = 1; i < files.length; i++) {
                    new ZipFile(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip").extractFile(files[i], CARTELLA_FILES + "/");
                }
            } catch (ZipException e) {
                e.printStackTrace();
            }
        } else {
            importa();
        }
    }

    /**
     * Metodo per eliminare i file ridondanti
     *
     * @param folder true   → Elimina i file nella cartella [CARTELLA_FILES]<br />
     *               false  → Elimina i file nella cartella [CARTELLA_PUBBLICA]
     */
    private void eliminaFiles() {
        File[] dir = null;
        dir = CARTELLA_PUBBLICA.listFiles();
        for (int i = 0; i < dir.length; i++) {
            switch (dir[i].getName()) {
                case "area_bosco.cpg":
                case "area_bosco.dbf":
                case "area_bosco.prj":
                case "area_bosco.shp":
                case "area_bosco.shx":
                case "qfield_marconi.gpkg":
                case "qfield_marconi.qgs":
                case "DCIM":
                    break;
                default:
                    deleteRecursive(dir[i]);
                    break;
            }
        }
    }

    /**
     * Metodo ricorsivo per l'eliminazione dei file
     *
     * @param fileOrDirectory File o cartella da eliminare
     */
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * Reset dell'ArrayList [fileMancanti]
     */
    private void resetArraylist() {
        fileMancanti.removeAll(fileMancanti);
        for (int i = 0; i < FILENAMES.length - 1; i++) {
            fileMancanti.add(FILENAMES[i]);
        }
    }

    /**
     * Crea una cartella
     *
     * @param file Cartella
     * @throws Exception - Il telefono non riesce ad accedere alle cartelle
     */
    private void creaCartella(File file) throws Exception {
        if (file.exists()) {
            //throw new Exception("I file sono già stati importati.\nPremere UNA sola volta.\nGrazie.");
        } else {
            file.mkdirs();
            if (!file.isDirectory()) {
                throw new Exception("Si è verificato un problema GRAVE.\nIl telefono non riesce ad accedere alle cartelle.");
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setTitle("Download");
                pDialog.setMessage("Attendere durante l'importazione dei dati...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.setCancelable(false);
                pDialog.show();
                return pDialog;
            case 1: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setTitle("Upload");
                pDialog.setMessage("Attendere durante l'esportazione dei dati...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.setCancelable(false);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    /**
     * Background AsyncTask per il Download dei File dal Web Service
     */
    class DownloadFileFromURL extends AsyncTask<Void, String, String> {

        /**
         * Prima di avviare il Thread in background, mostra il Pop-Up di Download
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(0);
        }

        /**
         * Avvio del Download dei file tramite un Thread in background
         *
         * @return OK      → Download avvenuto con successo<br />
         * null    → Download fallito
         */
        @Override
        protected String doInBackground(Void... voids) {
            int count;
            try {
                URL url = new URL(UNIQUE_URL + "download?user=" + NAME_SURNAME);
                //URL url = new URL(UNIQUE_URL + "download");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                //int lenghtOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    //publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                Thread.sleep(2000);
                new ZipFile(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip").extractAll(CARTELLA_PUBBLICA + "/");
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                statusDownload = false;
                return null;
            }
            statusDownload = true;
            return "OK";
        }

        /**
         * Dopo aver completato il Download, rimuove il Pou-Up
         **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(0);
            if (!statusDownload) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Errore");
                builder.setMessage("Impossibile connettersi al Server");
                builder.setPositiveButton("Impostazioni Rete", (dialog, which) -> apri(0));
                builder.setNegativeButton("Annulla", null);
                builder.show();
            } else {
                btn_import.setEnabled(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Download riuscito");
                builder.setMessage("Dati importati correttamente");
                builder.setPositiveButton("OK", null);
                builder.show();
                btn_export.setEnabled(true);

            }
        }
    }

    private boolean isImportati() {
        File[] dir = CARTELLA_PRIVATA.listFiles();
        for (int i = 0; i < dir.length; i++) {
            if (dir[i].getName().equals(NAME_SURNAME + ".zip")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Background AsyncTask per l'Upload dei File dal Web Service
     */
    class UploadFileFromURL extends AsyncTask<Void, Void, String> {

        /**
         * Prima di avviare il Thread in background, mostra il Pop-Up di Upload
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(1);
        }

        /**
         * Avvio dell'Upload dei file tramite un Thread in background
         *
         * @return OK      → Upload avvenuto con successo<br />
         * null    → Upload fallito
         */
        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            DataOutputStream dOut = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBuffersize = 1 * 1024 * 1024;
            File file = new File(CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip");

            //path was declared in the constructor of my AsyncTask-Class.
            // path has to look like this: /storage/some/dirs/myarchive.zip, when you use a .zip
            //At this point it is clear, that you have to use the full path, but many posts just used
            // a simple name like "file" as an example in the following code, which confused me.

            try {
                FileInputStream fileIn = new FileInputStream(file);
                URL url = new URL(UNIQUE_URL + "upload?user=" + NAME_SURNAME);

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("fileupload", CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip");

                dOut = new DataOutputStream(conn.getOutputStream());
                dOut.writeBytes(twoHyphens + boundary + lineEnd);
                dOut.writeBytes("Content-Disposition: form-data; name=\"project\";filename=\"" + CARTELLA_PRIVATA + "/" + NAME_SURNAME + ".zip" + "\"" + lineEnd);

                //Here you have to use the field in your html/php-code, which contains the file after "name"
                //In my case it was a file chooser (input type="file"), which has the name="fileupload".
                //There wasn't any post again, where it was explained what kind of value is expected here.


                dOut.writeBytes(lineEnd);

                bytesAvailable = fileIn.available();
                bufferSize = Math.min(bytesAvailable, maxBuffersize);
                buffer = new byte[bufferSize];
                bytesRead = fileIn.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dOut.write(buffer, 0, bufferSize);
                    bytesAvailable = fileIn.available();
                    bufferSize = Math.min(bytesAvailable, maxBuffersize);
                    bytesRead = fileIn.read(buffer, 0, bufferSize);
                }

                dOut.writeBytes(lineEnd);
                dOut.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                int responseCode = conn.getResponseCode();
                String responseMessage = conn.getResponseMessage();

                Log.i("UPLOAD", "HTTP Response is: " + responseCode + ": " + responseMessage);

                if (responseCode == 200) {
                    statusUpload = true;
                    return "OK";
                }
                fileIn.close();
                dOut.flush();
                dOut.close();
            } catch (Exception e) {
                e.printStackTrace();
                statusUpload = false;
                return null;
            }
            statusUpload = false;
            return null;
        }

        /**
         * Dopo aver completato l'Upload, rimuove il Pou-Up
         **/
        @Override
        protected void onPostExecute(String s) {
            dismissDialog(1);
            if (!statusUpload) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Errore");
                builder.setMessage("Impossibile connettersi al Server");
                builder.setPositiveButton("Impostazioni Rete", (dialog, which) -> apri(0));
                builder.setNegativeButton("Annulla", null);
                builder.show();
            } else {
                btn_import.setEnabled(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Upload riuscito");
                builder.setMessage("Dati esportati correttamente");
                builder.setPositiveButton("OK", null);
                builder.show();
                btn_export.setEnabled(true);
            }
        }


    }

    /**
     * Avviare una nuova attività
     *
     * @param cheCosa 0 → Impostazioni di Rete<br />
     *                1 → Impostazioni dell'applicazione<br />
     *                2 → Applicazione Qfield<br />
     *                3 → Sito Web [UNIQUE_URL]
     */
    private void apri(int cheCosa) {
        switch (cheCosa) {
            case 0:
                startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
                break;
            case 1:
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                break;
            case 2:
                try {
                    startActivity(getPackageManager().getLaunchIntentForPackage("ch.opengis.qfield"));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=ch.opengis.qfield")));
                }
                break;
            case 3:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(UNIQUE_URL)));
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (intent == null) {
            intent = new Intent();
        }
        super.startActivityForResult(intent, requestCode);
    }
}