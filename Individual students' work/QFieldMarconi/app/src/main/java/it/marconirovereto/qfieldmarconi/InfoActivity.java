package it.marconirovereto.qfieldmarconi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

import pl.droidsonroids.gif.GifImageView;

public class InfoActivity extends AppCompatActivity {

    FloatingActionButton fabLogOut;
    GifImageView gif;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        gif = findViewById(R.id.gif);
        imageView = findViewById(R.id.imageView);
        fabLogOut = findViewById(R.id.logOut);
        fabLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this);
                builder.setTitle("Arresta l'app");
                builder.setMessage("Verranno eliminati solamente i file per QField.\n\nContinuare?");
                builder.setPositiveButton("Sì", (dialog, which) -> esci());
                builder.setNegativeButton("No", null);
                builder.show();
            }
        });
        fabLogOut.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoActivity.this);
                builder.setTitle("Area segreta");
                builder.setMessage("Accesso ai soli autorizzati");
                builder.setPositiveButton("Continua", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gis.andtrentini.it/amogus"))));
                builder.setNegativeButton("Annulla", null);
                builder.show();
                return false;
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imageView.setVisibility(View.INVISIBLE);
                gif.setVisibility(View.VISIBLE);
            }
        }, 10000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imageView.setVisibility(View.VISIBLE);
                gif.setVisibility(View.INVISIBLE);
            }
        }, 10069);
    }

    private void esci() {
        deleteRecursive(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "it.marconirovereto.qfieldmarconi"));
        Toast.makeText(getApplicationContext(), "Tutti i file sono stati eliminati", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
            }
        }, 1000);
    }
    // NON TOCCARE
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}
