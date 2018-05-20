package wpam.recognizer;

//import pl.polidea.apphance.Apphance;
import android.R.string;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static android.media.ToneGenerator.TONE_DTMF_1;
import static java.lang.Integer.parseInt;

public class MainActivity extends Activity {


	private long startTime = 0L;
	private Handler customHandler = new Handler();
	long timeInMilliseconds = 0L;
	long timeSwapBuff = 0L;
	long updatedTime = 0L;

	public int lengthOfTone = 0;
	public int waitTime = 0;
	public int amountOfTone = 0;

	public boolean sender = false;
	private Button stateButton;
	private Button clearButton;	
	private EditText recognizeredEditText;
	private SpectrumView spectrumView;	
	private NumericKeyboard numKeyboard;
	
	Controller controller; 
	
	private String recognizeredText;

	History history;
	
	public static final String APP_KEY = "806785c1fb7aed8a867039282bc21993eedbc4e4";

	public void StartTimer() {

		startTime = SystemClock.uptimeMillis();
		customHandler.postDelayed(updateTimerThread, 0);
	}

	public void StopTimer() {
		timeSwapBuff += timeInMilliseconds;
		customHandler.removeCallbacks(updateTimerThread);
	}

	private Runnable updateTimerThread = new Runnable() {

		public void run() {

			timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

			updatedTime = timeSwapBuff + timeInMilliseconds;

			int secs = (int) (timeInMilliseconds / 1000);
			int mins = secs / 60;
			secs = secs % 60;
			int hours = mins / 60;
			mins = mins % 60;
			//int milliseconds = (int) (updatedTime % 1000);
			//+ ":" + String.format("%03d", milliseconds)
			String timer = "" + String.format("%02d", hours) + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs);
			//set yout textview to the String timer here
			customHandler.postDelayed(this, 1000);
		}

	};

	public static Activity getActivity() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
		Class activityThreadClass = Class.forName("android.app.ActivityThread");
		Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
		Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
		activitiesField.setAccessible(true);

		Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
		if (activities == null)
			return null;

		for (Object activityRecord : activities.values()) {
			Class activityRecordClass = activityRecord.getClass();
			Field pausedField = activityRecordClass.getDeclaredField("paused");
			pausedField.setAccessible(true);
			if (!pausedField.getBoolean(activityRecord)) {
				Field activityField = activityRecordClass.getDeclaredField("activity");
				activityField.setAccessible(true);
				Activity activity = (Activity) activityField.get(activityRecord);
				return activity;
			}
		}

		return null;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		Activity activity = null;
		try {
			activity = MainActivity.getActivity();
		}catch(Exception e){

		}
		ActivityManager am = (ActivityManager)this.getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
		controller = new Controller(this);

		stateButton = (Button)this.findViewById(R.id.stateButton);		
		stateButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				controller.changeState();
			}
		});
		
		clearButton = (Button)this.findViewById(R.id.clearButton);		
		clearButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				controller.clear();
			}
		});

		spectrumView = new SpectrumView(); 
		spectrumView.setImageView((ImageView) this.findViewById(R.id.spectrum));
				
		recognizeredEditText = (EditText)this.findViewById(R.id.recognizeredText);
		recognizeredEditText.setFocusable(false);
		
		numKeyboard = new NumericKeyboard();		
		numKeyboard.add('0', (Button)findViewById(R.id.button0));
		numKeyboard.add('1', (Button)findViewById(R.id.button1));
		numKeyboard.add('2', (Button)findViewById(R.id.button2));
		numKeyboard.add('3', (Button)findViewById(R.id.button3));
		numKeyboard.add('4', (Button)findViewById(R.id.button4));
		numKeyboard.add('5', (Button)findViewById(R.id.button5));
		numKeyboard.add('6', (Button)findViewById(R.id.button6));
		numKeyboard.add('7', (Button)findViewById(R.id.button7));
		numKeyboard.add('8', (Button)findViewById(R.id.button8));
		numKeyboard.add('9', (Button)findViewById(R.id.button9));
		numKeyboard.add('0', (Button)findViewById(R.id.button0));
		numKeyboard.add('#', (Button)findViewById(R.id.buttonHash));
		numKeyboard.add('*', (Button)findViewById(R.id.buttonAsterisk));
		
		setEnabled(false);
		
		recognizeredText = "";
		
		history = new History(this);
		history.load();
	}
	
	public void start()
	{
		runOnUiThread(new Runnable(){
			public void run() {
				String result = getIntent().getStringExtra("MESSAGE");
				EditText textViewLength = (EditText) findViewById(R.id.length_of_tone_to_be_sent);
				EditText textViewWait = (EditText) findViewById(R.id.wait_time_after_the_tone);
				EditText textViewAmount = (EditText) findViewById(R.id.length_of_tone_to_heard);

				try {
					lengthOfTone = parseInt(textViewLength.getText().toString());
					waitTime = parseInt(textViewWait.getText().toString());
					amountOfTone = Integer.parseInt(textViewAmount.getText().toString());
					if (result != null && result.equals("Sender")) {
						sender = true;

						ToneGenerator _toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
						_toneGenerator.startTone(TONE_DTMF_1, lengthOfTone);
					}
					stateButton.setText(R.string.stop);
					setEnabled(true);

				} catch (NumberFormatException e) {
					AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
					alertDialog.setTitle("Alert");
					alertDialog.setMessage("Please input time in milliseconds");
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
					alertDialog.show();
				}}
			});
		}

	
	public void stop()
	{
		history.add(recognizeredText);
		
		stateButton.setText(R.string.start);
		setEnabled(false);
	}
	
	public int getAudioSource()
	{
		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		
		if (telephonyManager.getCallState() != TelephonyManager.PHONE_TYPE_NONE)
			return MediaRecorder.AudioSource.VOICE_DOWNLINK;
		
		return MediaRecorder.AudioSource.MIC;
	}

	public void drawSpectrum(Spectrum spectrum) {
		spectrumView.draw(spectrum);		
	}
	
	public void clearText() 
	{
		history.add(recognizeredText);
		
		recognizeredText = "";
		recognizeredEditText.setText("");
	}
	
	public void addText(Character c) 
	{
		// here goes the logic
		// produce sound!

		recognizeredText += c;
		recognizeredEditText.setText(recognizeredText);
	}

	public void setText(String text)
	{
		if(sender){

		}
		recognizeredEditText.setText(text);		
	}
	
	public void setEnabled(boolean enabled) 
	{
		recognizeredEditText.setEnabled(enabled);
		numKeyboard.setEnabled(enabled);
	}
	public void setAciveKey(char key)
	{
		numKeyboard.setActive(key);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.layout.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.history:
	        	showHistory();
	            break;
	        case R.id.send:
	        	sendRecognizeredText();
	        	break;
	        case R.id.about:
	        	showAbout();
	            break;
	    }
	    return true;
	}

	private void showHistory()
	{
		history.add(recognizeredText);
		history.save();		
		
		Intent intent = new Intent(this, HistoryActivity.class);
		startActivity(intent);
	}

	private void sendRecognizeredText() {
		final Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
		sendIntent.setType("text/plain");
		sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, recognizeredText);
		startActivity(Intent.createChooser(sendIntent, getString(R.string.send)+":"));
	}

	private void showAbout() 
	{
		AlertDialog about = new AlertDialog.Builder(this).create();
		
		about.setTitle(getString(R.string.app_name)+" ("+getVersion()+")");
		about.setIcon(R.drawable.icon);
		about.setMessage(getString(R.string.about_text));
		about.show();
	}
	
	private String getVersion() 
	{
		PackageManager manager = getPackageManager();
		PackageInfo info = null;	
		
		try {	
			info = manager.getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
//			Log.wtf("NameNotFoundException", "getVersion() NameNotFoundException");
		}
        	 return info.versionName;
	}
	
	@Override
	protected void onDestroy() 
	{
		history.add(recognizeredText);
		
		history.save();
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		if (controller.isStarted())
			controller.changeState();
		super.onPause();
	}
}