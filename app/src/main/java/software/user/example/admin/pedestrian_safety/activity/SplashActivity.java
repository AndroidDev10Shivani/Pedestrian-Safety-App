package software.user.example.admin.pedestrian_safety.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import software.user.example.admin.pedestrian_safety.R;


public class SplashActivity extends Activity implements AnimationListener {

	ImageView imgLogo;
	Button btnStart;
	TextView t1,t2;
LinearLayout l;
	// Animation
	Animation animTogether1,animTogether,t1anim,t2anim;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		imgLogo = (ImageView) findViewById(R.id.imgLogo);
		l=(LinearLayout) findViewById(R.id.splash1);
		t1=(TextView) findViewById(R.id.my);
		t2=(TextView) findViewById(R.id.app);
		
		// load the animation
		animTogether = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.zoom_in);
		t1anim = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.right_in);
		t2anim = AnimationUtils.loadAnimation(getApplicationContext(),
						R.anim.left_in);
		animTogether1 = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.bounce);

		// set animation listener
		animTogether.setAnimationListener(this);
		animTogether1.setAnimationListener(this);
		t1anim.setAnimationListener(this);
		t2anim.setAnimationListener(this);

		l.startAnimation(animTogether);
		
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		// Take any action after completing the animation

		// check for zoom in animation
		if (animation == animTogether) {
			t1.setVisibility(View.VISIBLE);

			t1.startAnimation(t1anim);


			}
		if (animation == t1anim) {

			t2.setVisibility(View.VISIBLE);

			t2.startAnimation(t2anim);

		}
		if (animation == t2anim ) {
			

			imgLogo.setVisibility(View.VISIBLE);
			imgLogo.startAnimation(animTogether1);
				}
		
		if (animation == animTogether1) {
			Thread t= new Thread(){
				
				public void run() {
					try{
						sleep(3000);
					}catch(InterruptedException e){
						e.printStackTrace();
					}finally{
						SharedPreferences pref = getApplicationContext().getSharedPreferences("isLogin", MODE_PRIVATE);
						String p=pref.getString("isLogin","0");
						if(p.equals("0")){
						Intent i=new Intent(SplashActivity.this,Activity_Login.class);

							startActivity(i);
							finish();
						}else {
							Intent i = new Intent(SplashActivity.this, MainScreenActivity.class);
							startActivity(i);
							finish();
						}
					}
					
				}
			};
			t.start();

			
		}

	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAnimationStart(Animation animation) {
		// TODO Auto-generated method stub

	}

}