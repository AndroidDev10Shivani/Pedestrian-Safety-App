package software.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class SQLiteAdapter {


	//Database name
	public static final String MYDATABASE_NAME = "DF";

	//database version
	public static final int MYDATABASE_VERSION = 1;


	private SQLiteHelper sqLiteHelper;
	private SQLiteDatabase sqLiteDatabase;

	private Context context;
  

    private final ArrayList<User> Userlist = new ArrayList<User>();

	public SQLiteAdapter(Context c) {
		context = c;
	}

	public SQLiteAdapter openToRead() throws android.database.SQLException {
		sqLiteHelper = new SQLiteHelper(context, MYDATABASE_NAME, null,
				MYDATABASE_VERSION);
		sqLiteDatabase = sqLiteHelper.getReadableDatabase();
		return this;
	}

	public SQLiteAdapter openToWrite() throws android.database.SQLException {
		sqLiteHelper = new SQLiteHelper(context, MYDATABASE_NAME, null,
				MYDATABASE_VERSION);
		sqLiteDatabase = sqLiteHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		sqLiteHelper.close();
	}
	public long insertNotificationTable(String Notice,String date) {

		ContentValues contentValues = new ContentValues();
		contentValues.put("Notice", Notice);
		contentValues.put("date", date);

		return sqLiteDatabase.insert("NotificationTable", null, contentValues);
	}

//insert user details in databse
	public long insertUser(String name,String username,String password,String strMobile1) {

		SharedPreferences sp =context.getSharedPreferences("AI",Context.MODE_PRIVATE);
		String id=sp.getString("id","0");
		id=(Integer.parseInt(id)+1)+"";

		if(!id.equals("0")){
			id=(Integer.parseInt(id)+1)+"";
			SharedPreferences.Editor e=sp.edit();
			e.putString("id",id);
			e.apply();
		}
		ContentValues contentValues = new ContentValues();
		contentValues.put("id", id);

		contentValues.put("name", name);
		contentValues.put("username", username);
		contentValues.put("password", password);

		contentValues.put("strMobile1", strMobile1);




		return sqLiteDatabase.insert("User", null, contentValues);
	}



	//get user data from database

	public ArrayList<User> retrieveAllUser() {
		
		
	Userlist.clear();
	Cursor cursor = sqLiteDatabase.rawQuery(
			"select * from User;", null);
	try {
	  if (cursor.moveToFirst()) {
 		do {
 			User contact = new User();
 		   
 		    
 		    contact.setId(Integer.parseInt(cursor.getString(0)));
 		    contact.setName(cursor.getString(1));
 		   contact.setUsername(cursor.getString(2));
 		   contact.setPassword(cursor.getString(3));
 		  contact.setMobNumber1(cursor.getString(4));
 		   

 		   
 		   Userlist.add(contact);
 		  //  consumer_metadata_details.add(field_type1);
 		} while (cursor.moveToNext());
 	    }

 	    // return contact list
 	    cursor.close();
 	  //  db.close();
 	    return     Userlist;
 	} catch (Exception e) {
 	    // TODO: handle exception
 	    Log.e("all_contact", "" + e);
 	}finally {
 	    cursor.close();
 	}

 	return     Userlist;
}




//get user id
	public String getUSerID() {
	
	String USerID = "-1";
	try {
		Cursor cursor = sqLiteDatabase.rawQuery(
				"select userid from User;", null);
		if (cursor != null && cursor.moveToFirst()) {
			USerID =cursor.getString(0); // The 0 is the column index, we only
								// have 1 column, so the index is 0
		}
		
		return USerID;
	} catch (SQLiteException e) {
		//Log.e(TAG, "Get Last" + e);
		return "0";
	} finally {
		//Log.i(TAG, "Last User Id=" + USerID);
	
	}
}

	//delete user data
	public int deleteUser() {

		int k = sqLiteDatabase.delete("User", null, null);
		return k;
	}

	//get total user
	 public int Get_Total_User() {
			
			
			Cursor cursor = sqLiteDatabase.rawQuery(
					"select * from User;", null);
			int count = cursor.getCount();
		    cursor.close();
		  
		    return count;
		    }
	
	 




         //change password
	public void update_userpassword(int id,String password) {
		// TODO Auto-generated method stub

		sqLiteDatabase.execSQL("update user set password='" + password + "'  where id=1");
		//

	}


	
	public class SQLiteHelper extends SQLiteOpenHelper {
		/*
		 * Constructor called its super class
		 */
		public SQLiteHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}


                //create tables
		@Override
		public void onCreate(SQLiteDatabase db) {

			
			db.execSQL("create table IF NOT EXISTS User(id Integer,name text,username text,password text,strMobile1 text);");
		//	db.execSQL("create table NotificationTable(id Integer PRIMARY KEY AUTOINCREMENT,Notice text,date text);");


			Log.d("Log", "Database Created");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
		}
		@Override
		public synchronized void close() {
		    if(sqLiteDatabase != null){
		    	sqLiteDatabase.close();
		    super.close();
		    }   
		}
	}

}