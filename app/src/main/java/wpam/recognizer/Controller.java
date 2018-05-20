package wpam.recognizer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.media.ToneGenerator.TONE_DTMF_1;
import static android.media.ToneGenerator.TONE_DTMF_2;
import static android.media.ToneGenerator.TONE_DTMF_3;
import static android.media.ToneGenerator.TONE_DTMF_4;
import static android.media.ToneGenerator.TONE_DTMF_5;
import static android.media.ToneGenerator.TONE_DTMF_6;
import static android.media.ToneGenerator.TONE_DTMF_7;
import static android.media.ToneGenerator.TONE_DTMF_8;


public class Controller 
{
	private boolean started;
	static int i = 0;
	static char lk = 0;
	private RecordTask recordTask;	
	private RecognizerTask recognizerTask;	
	MainActivity mainActivity;
	BlockingQueue<DataBlock> blockingQueue;

	private Character lastValue;
		
	public Controller(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;
	}

	public void changeState() 
	{
		if (started == false)
		{
			
			lastValue = ' ';
			
			blockingQueue = new LinkedBlockingQueue<DataBlock>();
			
			mainActivity.start();
			
			recordTask = new RecordTask(this,blockingQueue);
			
			recognizerTask = new RecognizerTask(this,blockingQueue);
			
			recordTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			recognizerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			
			started = true;
		} else {
			
			mainActivity.stop();
			
			recognizerTask.cancel(true);
			recordTask.cancel(true);
			
			started = false;
		}
	}

	public void clear() {
		mainActivity.clearText();
	}

	public boolean isStarted() {
		return started;
	}


	public int getAudioSource()
	{
		return mainActivity.getAudioSource();
	}
	
	/*public void spectrumReady(Spectrum spectrum)
	{
		mainActivity.drawSpectrum(spectrum);
	}*/

	public void keyReady(char key) 
	{
		//mainActivity.setAciveKey(key);
		
		if(key != ' ')
			if(lk != key || i==0) {
				i++;

				if (mainActivity.sender) {
                    //mainActivity.addText(key);
                    switch (key) {
						case '2':
							lk = '2';
							sendTone(TONE_DTMF_3, mainActivity.lengthOfTone);
							break;
						case '4':
							lk = '4';
							sendTone(TONE_DTMF_5, mainActivity.lengthOfTone);
							break;
						case '6':
							lk = '6';
							sendTone(TONE_DTMF_7, mainActivity.lengthOfTone);
							break;
						case '8':
							lk = '8';
							mainActivity.StopTimer();
							long t = mainActivity.updatedTime;
							printAlert(t);
							//finish send email.
							break;
					}
				} else {
                    //mainActivity.addText(key);

                    switch (key) {
						case '1':
							lk = '1';
							sendTone(TONE_DTMF_2, mainActivity.lengthOfTone);
							break;
						case '3':
							lk = '3';
							sendTone(TONE_DTMF_4, mainActivity.lengthOfTone);
							break;
						case '5':
							lk = '5';
							sendTone(TONE_DTMF_6, mainActivity.lengthOfTone);
							break;
						case '7':
							lk = '7';
							sendTone(TONE_DTMF_8, mainActivity.lengthOfTone);
							break;
					}
				}
			}
		lastValue = key;
	}

	public void sendTone(final int tone, final int lengthOfTone){
		MyThread myThread = new MyThread();
		myThread.instantiate(mainActivity.waitTime, tone, lengthOfTone, mainActivity.amountOfTone);
		myThread.run();
    }

		public void printAlert(long updatedTime){
		AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();
		alertDialog.setTitle("Alert");
		alertDialog.setMessage("Time : " + updatedTime);
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		alertDialog.show();
	}
	
	public void debug(String text) 
	{
		mainActivity.setText(text);
	}
}

class MyThread extends Thread {

	private int waitTime;
	private int tone;
	private int lengthOfTone;
	private int amountOfTone;

	public void instantiate(int waitTime, int tone, int lengthOfTone, int amountOfTone){
		this.waitTime = waitTime;
		this.tone = tone;
		this.lengthOfTone = lengthOfTone;
		this.amountOfTone = amountOfTone;
	}


	public void run() {
		try {
			Thread.sleep(waitTime + amountOfTone);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ToneGenerator _toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
		_toneGenerator.startTone(tone, lengthOfTone);
	}
}
