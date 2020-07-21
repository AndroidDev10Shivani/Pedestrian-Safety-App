package software.user.example.admin.pedestrian_safety.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import software.database.SQLiteAdapter;
import software.user.example.admin.pedestrian_safety.R;

import org.json.JSONException;
import org.json.JSONObject;




public class Activity_Login extends AppCompatActivity {

    Dialog d;
    //Edittext declaration
    EditText edtUserName,edtPassword;
    String strUserName,strPassword;

    Button btnLogin;
    //Signup textview
    TextView textSignup;
    private ProgressDialog pDialog;
    //Database sqllite declaration
    SQLiteAdapter dbhelper;
    String flag="0";
    public static int findtrack=0;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       /* SharedPreferences pref = getApplicationContext().getSharedPreferences("isLogin", MODE_PRIVATE);
        String p=pref.getString("isLogin","0");
        if(p.equals("1")){
            Intent i= new Intent(Activity_Login.this,MainActivity.class);
            startActivity(i);

        }*/


        setContentView(R.layout.activity_login);

        dbhelper=new SQLiteAdapter(getApplicationContext());

       //username initilization
        edtUserName=(EditText)findViewById(R.id.editTextUserName);
        //Password initilization
        edtPassword=(EditText)findViewById(R.id.editTextPassword);

        // //Login button initilization
        btnLogin=(Button)findViewById(R.id.buttonLogin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);


        //on click on button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get username and password from edittext and save in variable
                strUserName=edtUserName.getText().toString();
                strPassword=edtPassword.getText().toString();

                if(!strUserName.equals(""))
                {

                    if(!strPassword.equals(""))
                    {
                        checkLogin();

                    }else{
                        edtPassword.setText("");
                        edtPassword.setHint("Please Enter Password");
                        edtPassword.requestFocus();
                    }

                }else{
                    edtUserName.setText("");
                    edtUserName.setHint("Please Enter Email");
                    edtUserName.requestFocus();
                }


            }
        });


        textSignup=(TextView)findViewById(R.id.textViewSignup);
        textSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //start signup activity
                Intent i=new Intent(getApplicationContext(),Activity_SignUp.class);
                startActivity(i);
            }
        });
    }




    void checkLogin(){


        progressDialog.show();
        strPassword = edtPassword.getText().toString();

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        SharedPreferences sp=getSharedPreferences("IP", MODE_PRIVATE);
        String IP=sp.getString("IP","209.190.31.226:8080");
        String url="http://"+IP+"/Pedestrian_Safety/rest/AppService/Login?username="+strUserName+"&pass="+strPassword;
        url=url.replace(" ","%20");
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {

            public void onResponse(JSONObject jsonObject) {
                progressDialog.hide();



                Log.i("##", "##" + jsonObject.toString());

                System.out.println("## response:" + jsonObject.toString());


                try {
                    if(jsonObject.getString("result").equals("success")){



                        //show message
                        Toast.makeText(getApplicationContext(), "Login Successfully", Toast.LENGTH_SHORT).show();
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("isLogin", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("isLogin", "1");
                        editor.commit();

                        //when login success then go to new activity
                        Intent i=new Intent(getApplicationContext(),MainScreenActivity.class);
                        startActivity(i);
                        finish();


                    }else {

                        Toast.makeText(Activity_Login.this,"Login Fail",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, this.createRequestRegisterErrorListener());


        requestQueue.add(jsObjRequest);
    }

    /**
     * Error Listener of the requested url
     * @return Response.ErrorListener
     */
    private Response.ErrorListener createRequestRegisterErrorListener() {
        return new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("##", "##" + error.toString());
                progressDialog.hide();

            }
        };
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gcm, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                d = new Dialog(Activity_Login.this);
                d.setTitle("Set IP");
                d.setContentView(R.layout.dialog);
                d.getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

                final EditText ip = (EditText) d.findViewById(R.id.ip);
                Button submit = (Button) d.findViewById(R.id.submit);
                SharedPreferences sp=getSharedPreferences("IP", MODE_PRIVATE);
                String ipStr=sp.getString("IP","209.190.31.226:8080");
                ip.setText(ipStr);
                submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String str=ip.getText().toString();
                        SharedPreferences sp=getSharedPreferences("IP", MODE_PRIVATE);
                        SharedPreferences.Editor e=sp.edit();
                        e.putString("IP",str);
                        ///  ProjectConfig.IP=str;
                        e.apply();
                        d.dismiss();
                    }
                });
                d.show();
                //  Toast.makeText(this, "Option1", Toast.LENGTH_SHORT).show();
                return true;

        }
        return true;
    }

}
