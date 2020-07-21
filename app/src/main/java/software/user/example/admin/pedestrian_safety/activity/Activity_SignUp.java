package software.user.example.admin.pedestrian_safety.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONException;
import org.json.JSONObject;

import software.database.SQLiteAdapter;
import software.user.example.admin.pedestrian_safety.R;


public class Activity_SignUp extends AppCompatActivity {

    //Edittext declaration
    EditText edtPassword,edtName,editTextUsername,edtMobile1;
    String strPassword,strName,strUsername,strMobile1;
    //login button Register
    Button buttonRegister;

    private ProgressDialog pDialog;
    //database declaration
    SQLiteAdapter dbhelper;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);//set layout

           //database initilization
        dbhelper=new SQLiteAdapter(getApplicationContext());

        //name,usrname,mobile number initilization
        edtName=(EditText)findViewById(R.id.editTextName);
        editTextUsername=(EditText)findViewById(R.id.editTextUsername);
        edtMobile1=(EditText)findViewById(R.id.editTextMobile1);
        edtPassword=(EditText)findViewById(R.id.editTextPassword);

        //Register button initilization
        buttonRegister=(Button)findViewById(R.id.buttonRegister);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        //on click on button
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                registerInSQLiteDatabase();
               /* Toast.makeText(getApplicationContext(),"Save Successfully",Toast.LENGTH_SHORT).show();
                Intent i=new Intent(getApplicationContext(),Activity_Login.class);
                startActivity(i);
*/

        }
        });

    }

void registerOnServer(){


        progressDialog.show();
    strPassword = edtPassword.getText().toString();
    strName = edtName.getText().toString();
    strUsername = editTextUsername.getText().toString();
    strMobile1 = edtMobile1.getText().toString();

    SharedPreferences sp=getSharedPreferences("IP", MODE_PRIVATE);
    String IP=sp.getString("IP","209.190.31.226:8080");
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
System.out.println("#####     name  "+strName+"   strPassword   "+strPassword+  "  username  "+strUsername);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest("http://"+IP+"/Pedestrian_Safety/rest/AppService/Registration?name="+strName+"&pass="+strPassword+"&username="+strUsername+"&mob="+strMobile1, null, new Response.Listener<JSONObject>() {

            public void onResponse(JSONObject jsonObject) {
                progressDialog.hide();



                Log.i("##", "##" + jsonObject.toString());

                System.out.println("## response:" + jsonObject.toString());


                try {
                    if(jsonObject.getString("result").equals("success")){

                        //store user data in database
                        dbhelper.openToWrite();
                        dbhelper.insertUser(strName, strUsername, strPassword, strMobile1);
                        dbhelper.close();


                        //start new activity
                        Intent i=new Intent(getApplicationContext(),Activity_Login.class);
                        startActivity(i);
                        finish();
                        Toast.makeText(getApplicationContext(),"Registration Successul",Toast.LENGTH_SHORT).show();



                    }else if(jsonObject.getString("result").equals("already")){
                        Toast.makeText(getApplicationContext(),"User Already Registered",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getApplicationContext(),"Registration Fail",Toast.LENGTH_SHORT).show();

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





void registerInSQLiteDatabase(){
    strPassword = edtPassword.getText().toString();
    strName = edtName.getText().toString();
    strUsername = editTextUsername.getText().toString().trim();
    final String emailPattern="[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    strMobile1 = edtMobile1.getText().toString();
    final String mobilePattern="[0-9]{10}";


       if (!strName.equals("")) {

        if (!strUsername.equals("")&& strUsername.matches(emailPattern)){

            if (!strPassword.equals("")) {

                if (!strMobile1.equals("")&& strMobile1.matches(mobilePattern)) {

                /*    //open database to get total user
                    dbhelper.openToWrite();
                    int toatluser= dbhelper.Get_Total_User();
                    dbhelper.close();
                    if(toatluser==0)
                    {*/

                      registerOnServer();

                  /*  }else{
                        Toast.makeText(getApplicationContext(),"User Register already",Toast.LENGTH_SHORT).show();
                    }
*/

                } else {
                    edtMobile1.setText("");
                    edtMobile1.setError("Please Enter Mobile");
                    edtMobile1.requestFocus();
                }
            } else {
                edtPassword.setText("");
                edtPassword.setError("Please Enter Password");
                edtPassword.requestFocus();
            }
        } else {
            editTextUsername.setText("");
            editTextUsername.setError("Please Enter Correct Email Id");
            editTextUsername.requestFocus();
        }

    } else {
        edtName.setText("");
        edtName.setError("Please Enter UserName");
        edtName.requestFocus();
    }

}





}
