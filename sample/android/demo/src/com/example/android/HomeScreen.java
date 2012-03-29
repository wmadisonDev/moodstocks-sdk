package com.example.android;

import com.moodstocks.android.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class HomeScreen extends Activity implements View.OnClickListener, Scanner.SyncListener {

	public static final String TAG = "HomeScreen";
	private ProgressDialog progress;
	private boolean ready = false;
	private boolean syncing = false;
	private boolean compatible = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		findViewById(R.id.scan_button).setOnClickListener(this);
		try {
			/* open the scanner, necessary to perform any operation using it.
			 * This step also checks at runtime that the device is compatible.
			 * You *must* implement a try/catch(UnsupportedDeviceException) and
			 * abort using the scanner if an Exception is returned, otherwise
			 * it may crash. See example below.
			 */
			Scanner.get().open(this, "ms.db");
			/* At this point, we check that the database is not empty. If it is,
			 * we guess sync() was never called and make a first (blocking) call
			 * to ensure that the scanner activity is not shown to the user until
			 * there is scanning content available. If it's not empty, we make a
			 * non-blocking call to it to update database at application startup.
			 * (see Scanner.SyncListener implementation below).
			 */
			if (Scanner.get().count() != 0) ready = true; 
			Scanner.get().sync(this);
		} catch (UnsupportedDeviceException e) {
			/* device is *not* compatible. In this demo application, we chose
			 * to inform the user and exit application. `compatible` flag is here
			 * to avoid calling scanner methods that *will* fail and log errors. 
			 */
			compatible = false;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(false);
			builder.setTitle("Unsupported device!");
			if (e.getMessage().equals(UnsupportedDeviceException.Message.VERSION)) {
				builder.setMessage("Device must run Android Gingerbread or over, sorry...");
			}
			else {
				builder.setMessage("Device not compatible with Moodstocks SDK, sorry...");
			}
			builder.setNeutralButton("Quit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					finish();
				}
			});
			builder.show();
		} catch (MoodstocksError e) {
			/* an error occurred while opening the scanner */
			if (e.getErrorCode() == MoodstocksError.Code.CREDMISMATCH) {
				// == DO NOT USE IN PRODUCTION: THIS IS A HELP MESSAGE FOR DEVELOPERS
				String errmsg = "there is a problem with your key/secret pair: "+
						"the current pair does NOT match with the one recorded within the on-disk datastore. "+
						"This could happen if:\n"+
						" * you have first build & run the app without replacing the default"+
						" \"ApIkEy\" and \"ApIsEcReT\" pair, and later on replaced with your real key/secret,\n"+
						" * or, you have first made a typo on the key/secret pair, build & run the"+
						" app, and later on fixed the typo and re-deployed.\n"+
						"\n"+
						"To solve your problem:\n"+
						" 1) uninstall the app from your device,\n"+
						" 2) make sure to properly configure your key/secret pair within Scanner.java\n"+
						" 3) re-build & run\n";
				MoodstocksError err = new MoodstocksError(errmsg, MoodstocksError.Code.CREDMISMATCH);
				err.log(Log.ERROR);
				finish();
				// == DO NOT USE IN PRODUCTION: THIS IS A HELP MESSAGE FOR DEVELOPERS
			}
			else {
				e.log(Log.ERROR);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (compatible) {
			try {
				/* you must close the scanner before exiting */
				Scanner.get().close();
			} catch (MoodstocksError e) {
				e.log(Log.ERROR);
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == findViewById(R.id.scan_button)) {
			/* As an example, we call sync() whenever the scanner activity is launched.
			 * In a real app context, it is your responsibility to place it wisely and
			 * ensure that it is called often enough to keep your users up-to-date.
			 * If your app has its own data synchronization process somewhere, we encourage
			 * you to call sync() at the same time.
			 */
			if (!syncing) Scanner.get().sync(this);
			// launch scanner
			startActivity(new Intent(this, ScanActivity.class));
		}
	}

	//----------------------
	// Scanner.SyncListener
	//----------------------

	@Override
	public void onSyncStart() {
		syncing = true;
		if (!ready) {
			/* initial sync: show a blocking view while syncing */
			progress = ProgressDialog.show(this, null, "Syncing...");
			TextView tv = (TextView)progress.findViewById(android.R.id.message);
			tv.setTextSize(20);
			tv.setPadding(10,0,0,0);
		}
	}

	@Override
	public void onSyncComplete() {
		syncing = false;
		if (!ready) {
			/* discard blocking view at initial sync */
			progress.dismiss();
			ready = true;
		}
	}

	@Override
	public void onSyncFailed(MoodstocksError e) {
		syncing = false;
		if (!ready) {
			/* discard blocking view at initial sync */
			progress.dismiss();
			ready = true;
			/* if an error occurred during the initial sync, we chose to notify 
			 * the user as the database is still empty and the scanner won't work
			 * as it should.
			 */
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Network Error!");
			builder.setMessage(e.getMessage());
			builder.setPositiveButton("OK", null);
			builder.create().show();
		}
	}
	
	//------
	// MENU
	//------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.sync && !syncing) {
			Scanner.get().sync(this);
		}
		return true;
	}

}
