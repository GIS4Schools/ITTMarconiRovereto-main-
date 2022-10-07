package it.marconirovereto.qfieldmarconi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class LoginActivity extends AppCompatActivity {

    static GoogleSignInClient mGoogleSignInClient;
    private File info;
    private Button btn;
    private FloatingActionButton btn_logout;
    private SignInButton signInButton;
    private boolean messageLogOut;

    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null && result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        messageLogOut = false;
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_AUTO);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        btn = findViewById(R.id.btn_signIn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        btn_logout = findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popUpLogout();
            }
        });
        if (ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "it.marconirovereto.qfieldmarconi");
                creaCartella(f1);
                File f3 = getExternalFilesDir(null);
                info = new File(f3, "info.txt");
            } catch (Exception exception) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Attenzione");
                builder.setMessage(exception.getMessage());
                builder.setPositiveButton("OK", null);
                builder.show();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.performClick();
                }
            }, 500);
        } else {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

    }

    private void popUpLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Logout");
        if (messageLogOut) {
            builder.setMessage("Disconnettere l'account Google?");
            builder.setPositiveButton("Sì", (dialog, which) -> revokeAccess());
            builder.setNegativeButton("No", null);
        } else {
            builder.setMessage("Nessun account connesso.");
            builder.setPositiveButton("Accedi", (dialog, which) -> btn.performClick());
        }
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            try {
                File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "it.marconirovereto.qfieldmarconi");
                creaCartella(f1);
                File f3 = getExternalFilesDir(null);
                info = new File(f3, "info.txt");
            } catch (Exception exception) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Attenzione");
                builder.setMessage(exception.getMessage());
                builder.setPositiveButton("OK", null);
                builder.show();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.performClick();
                }
            }, 500);
        } else {
            Toast.makeText(getApplicationContext(), "Sono obbligatori i permessi", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            finish();
        }
    }

    private void creaCartella(File file) throws Exception {
        if (!file.exists()) {
            file.mkdirs();
            if (!file.isDirectory()) {
                throw new Exception("Si è verificato un problema GRAVE.\nIl telefono non riesce ad accedere alle cartelle.");
            }
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startForResult.launch(signInIntent);
    }

    public void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(LoginActivity.this, "Account Google disconnesso", Toast.LENGTH_LONG).show();
                        messageLogOut = false;
                    }
                });
    }

    private void setInfos(String personaName, String personaGivenName, String personaFamilyName, String personaEmail, String personaId, String personaPhoto) {
        try {
            FileWriter writer = new FileWriter(info);
            String infoPersona = personaName + "&" + personaGivenName + "&" + personaFamilyName + "&" + personaEmail + "&" + personaId + "&" + personaPhoto;
            writer.append(infoPersona);
            writer.flush();
            writer.close();
        } catch (Exception exception) {

        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                String personName = acct.getDisplayName();
                String personGivenName = acct.getGivenName();
                String personFamilyName = acct.getFamilyName();
                String personEmail = acct.getEmail();
                String personId = acct.getId();
                Uri personPhoto = acct.getPhotoUrl();
                messageLogOut = true;
                setInfos(personName, personGivenName, personFamilyName, personEmail, personId, personPhoto != null ? personPhoto.toString() : "");
                if (personEmail.split("@")[1].equals("marconirovereto.it")) {
                    Toast.makeText(this, "Ciao " + personName + "!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setTitle("Attenzione");
                    //builder.setMessage("Sei un impostore?" + "\npersonName: " + personName + "\npersonGivenName: " + personGivenName + "\npersonFamilyName: " + personFamilyName + "\npersonEmail: " + personEmail + "\npersonId: " + personId);
                    builder.setMessage("È possibile accedere all'applicazione solo con account\n\n@marconirovereto.it");
                    builder.show();
                    revokeAccess();
                }
            }


        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.d("signInResult:failed code=", e.toString());
        }
    }
}