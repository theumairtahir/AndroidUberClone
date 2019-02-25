package ut786.clone.AndroidUberClone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import ut786.clone.AndroidUberClone.Models.User;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn, btnRegister;
    RelativeLayout rootLayout;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            CalligraphyConfig.initDefault(new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Arkhip_font.ttf").setFontAttrId(R.attr.fontPath).build());
            setContentView(R.layout.activity_main);

            //initiate firebase
            auth = FirebaseAuth.getInstance();
            db = FirebaseDatabase.getInstance();
            users = db.getReference("Users");


            //initiate views
            btnRegister = (Button) findViewById(R.id.btnRegister);
            btnSignIn = (Button) findViewById(R.id.btnSignIn);
            rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);

            //adding events
            btnRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showRegisterDialog();
                }
            });
            btnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSignInDialog();
                }
            });
        }
        catch (Exception ex){
            Toast.makeText(this,ex.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void showSignInDialog() {
        try {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Sign In");
            dialog.setMessage("Please use email & password to sign-in");
            LayoutInflater inflater = LayoutInflater.from(this);
            final View signIn_layout = inflater.inflate(R.layout.layout_sign_in, null);
            final MaterialEditText edtEmail = signIn_layout.findViewById(R.id.edtEmail);
            final MaterialEditText edtPass = signIn_layout.findViewById(R.id.edtPassword);

            dialog.setView(signIn_layout);

            //set dialog button
            dialog.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    //disable sign in button if signing in is in progress
                    btnSignIn.setEnabled(false);
                    //check validation
                    if (TextUtils.isEmpty(edtEmail.getText())) {
                        Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(edtPass.getText())) {
                        Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    final AlertDialog waitingDialog =new SpotsDialog(MainActivity.this);
                    waitingDialog.show();
                    //Login
                    auth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPass.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            waitingDialog.dismiss();
                            try {
                                startActivity(new Intent(MainActivity.this, Welcome.class));
                            }
                            catch (Exception ex){
                                Toast.makeText(MainActivity.this,ex.getMessage(),Toast.LENGTH_LONG).show();
                            }
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            waitingDialog.dismiss();
                            Toast.makeText(MainActivity.this,"Login Failed",Toast.LENGTH_LONG).show();
                            //Snackbar.make(rootLayout, "Sign In Failed. " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            //active sign in button again
                            btnSignIn.setEnabled(true);
                        }
                    });
                }
            });
            dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.show();
        }
        catch (Exception ex){
            Toast.makeText(this,ex.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegisterDialog() {
        try {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Register");
            dialog.setMessage("Please use email to register");
            LayoutInflater inflater = LayoutInflater.from(this);
            final View register_layout = inflater.inflate(R.layout.layout_register, null);
            final MaterialEditText edtEmail = register_layout.findViewById(R.id.edtEmail);
            final MaterialEditText edtPass = register_layout.findViewById(R.id.edtPassword);
            final MaterialEditText edtName = register_layout.findViewById(R.id.edtName);
            final MaterialEditText edtPhone = register_layout.findViewById(R.id.edtPhone);

            dialog.setView(register_layout);

            //set dialog button
            dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    //check validation
                    if (TextUtils.isEmpty(edtEmail.getText())) {
                        Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(edtPass.getText())) {
                        Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(edtName.getText())) {
                        Snackbar.make(rootLayout, "Please enter name", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(edtPhone.getText())) {
                        Snackbar.make(rootLayout, "Please enter phone", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (edtPass.getText().length() < 6) {
                        Snackbar.make(rootLayout, "Pass must contain at-least 6 characters", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    //register new user
                    auth.createUserWithEmailAndPassword(edtEmail.getText().toString(), edtPass.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            //saving user to the database
                            User user = new User(edtName.getText().toString(), edtEmail.getText().toString(), edtPass.getText().toString(), edtPhone.getText().toString());
                            //set primary key
                            users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Snackbar.make(rootLayout, "Registered Successfully", Snackbar.LENGTH_SHORT).show();
                                    return;
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(rootLayout, "Failed!" + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    return;
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(rootLayout, "Failed!" + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                    });
                }
            });
            dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.show();
        }
        catch (Exception ex){
            Toast.makeText(this,ex.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }
}
