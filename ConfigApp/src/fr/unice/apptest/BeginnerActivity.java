package fr.unice.apptest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;



import fr.unice.apptest.R;
import fr.unice.apptest.api.SecureRequest;
import fr.unice.apptest.webserver.WebServerService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import org.xml.sax.Attributes;
import org.xmlpull.v1.XmlPullParserException;

/**
 *  This Activity is responsible with taking the data from the forms and sending it along
 * with the Security Level and Data Type that was selected.
 * 
 * @author andrei
 */
public class BeginnerActivity extends Activity {

	// UI View references
	private View statusView, formView, textTypeView, fileTypeView, destinationManualView, destinationAutomaticView;
	
	// UI references to the signUpStatusView Views
	private ProgressBar progressBar;
	
	private EditText edtData, edtDestinationIP, edtDestinationPort;
	
	private Spinner spinnerLevel, spinnerType;
	
	private Button btnSend, btnSelectFile, btnAutomatic;
	
	private TextView tvFilePath;
	
	private RadioGroup rgDataType, rgDestinationMode;
	
	private ImageView ivSecurity, ivConsumption, ivOverall;
	
	private Documents documents;
	
	private XML xml;
	
	/**
	 * Used to retrieve user preferences
	 */
	private SharedPreferences prefs;
	
	private static final int ACTIVITY_CHOOSE_FILE = 1;
	private static final int ACTIVITY_SELECT_CONTACT = 2;
	
	private int state=0;
	
	String[] states={"Hangouts","Browser","HTTP"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_beginner);
		
		documents=new Documents(this);

		state=getIntent().getIntExtra("state", 0);
		
		xml=new XML(documents,"sdcard/data.xml");
		xml.addGetChild(states[state]);
		if(!xml.isChild("beginner")){
			xml.addGetChild("Config");
			xml.add("beginner","1");
			/*xml.add("ip","127.0.0.1");
			xml.add("port","8002");
			xml.add("target","0");
			xml.add("text","hello world!");*/
			xml.add("type","0");
			xml.add("level","0");
		}
		else{
			xml.addGetChild("Config");
			xml.add("beginner","1");
		}
		
		// Get a Shared Preferences instance to get the user preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// Add references to the 2 main Views
		 
		statusView = findViewById(R.id.view_status_Beginner);
		formView = findViewById(R.id.view_form_Beginner);
		
		// Add references to the file and text Views
		textTypeView = findViewById(R.id.view_Text_Type_Beginner);
		fileTypeView = findViewById(R.id.view_File_Type_Beginner);
		
		// Add references to the destination type Views
		destinationManualView = findViewById(R.id.view_Destination_Manual_Beginner);
		destinationAutomaticView = findViewById(R.id.view_Destination_Automatic_Beginner);
		
		// References to the progress bar from within the statusView
		progressBar = (ProgressBar) findViewById(R.id.progressBarBeginner);
		
		// References to views from within the formView
		edtData = (EditText) findViewById(R.id.edtDataBeginner);
		//edtData.setText(xml.getValue("text"));
		if(state!=3){edtData.setVisibility(View.GONE);}
		spinnerLevel = (Spinner) findViewById(R.id.spLevelBeginner);
		spinnerLevel.setId(xml.getInt("level"));
		
		spinnerType = (Spinner) findViewById(R.id.spTypeBeginner);
		spinnerType.setId(xml.getInt("type"));
		btnSend = (Button) findViewById(R.id.btnSendBeginner);
		if(state!=3){btnSend.setText("Save");}
		
		btnAutomatic = (Button) findViewById(R.id.btnAutomaticBeginner);
		edtDestinationIP = (EditText) findViewById(R.id.edtDestinationIPBeginner);
		//edtDestinationIP.setText(xml.getValue("ip"));
		if(state!=3){edtDestinationIP.setVisibility(View.GONE);}
		edtDestinationPort = (EditText) findViewById(R.id.edtDestinationPortBeginner);
		//edtDestinationPort.setText(xml.getValue("port"));
		if(state!=3){edtDestinationPort.setVisibility(View.GONE);}
		
		// References to fileTypeViewBeginner
		btnSelectFile = (Button) findViewById(R.id.btnSelectFileBeginner);
		tvFilePath = (TextView) findViewById(R.id.tvFilePathBeginner);
		
		rgDataType = (RadioGroup) findViewById(R.id.rgDataTypeBeginner);
		if(state!=3){rgDataType.setVisibility(View.GONE);}
		rgDestinationMode = (RadioGroup) findViewById(R.id.rgDestinationEnterModeBeginner);
		//rgDestinationMode.check(xml.getInt("target"));
		if(state!=3){rgDestinationMode.setVisibility(View.GONE);}
		
		ivSecurity = (ImageView) findViewById(R.id.ivSecurityBeginner);
		ivConsumption = (ImageView) findViewById(R.id.ivConsumptionBeginner);
		ivOverall = (ImageView) findViewById(R.id.ivOverallBeginner);
		
		informUser();
		
		// We set a CheckedChangeListener on the RadioGroup
		rgDataType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				// When the text type has been selected, show the text correspondent view
				// When the file type has been selected, show the file correspondent view
				
				switch(group.getCheckedRadioButtonId()) {
				case R.id.typeTextBeginner:
					textTypeView.setVisibility(View.VISIBLE);
					fileTypeView.setVisibility(View.GONE);
					break;
					
				case R.id.typeFileBeginner:
					textTypeView.setVisibility(View.GONE);
					fileTypeView.setVisibility(View.VISIBLE);
					break;
				
					default:
						break;
				}	
			}
		});
		
		// We set a CheckedChangeListener on the RadioGroup
		rgDestinationMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				// When the Manual type has been selected, show the correspondent view
				// When the Automatic type has been selected, show the  correspondent view

				switch (group.getCheckedRadioButtonId()) {
				case R.id.typeManualBeginner:
					destinationManualView.setVisibility(View.VISIBLE);
					destinationAutomaticView.setVisibility(View.GONE);
					break;

				case R.id.typeAutomaticBeginner:
					destinationAutomaticView.setVisibility(View.VISIBLE);
					destinationManualView.setVisibility(View.GONE);
					break;

				default:
					break;
				}
				
			}
		});
		
		spinnerLevel.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				informUser();
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		spinnerType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				informUser();
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		// We set a Click Listener for when the select file button is clicked/touched
		btnSelectFile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent chooseFile, start;
				// Set the type of intent (implicit intent) and it's action (for getting a content)
				chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
				// Set it's file type to recognise (in our case any type)
				chooseFile.setType("file/*");
				start = Intent.createChooser(chooseFile, getString(R.string.chooseafile));
				startActivityForResult(start, ACTIVITY_CHOOSE_FILE);
				
			}
		});
		
		// We set a Click Listener for when the Search contants button is
		// clicked/touched
		btnAutomatic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Create an intent to get to the Contact list
				Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
	            startActivityForResult(intent, ACTIVITY_SELECT_CONTACT);

			}
		});
		
		// We set a Click Listener for when the send button is clicked/touched
		btnSend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				xml.setValue("type", spinnerType.getSelectedItem().toString());
				xml.setValue("level", spinnerLevel.getSelectedItem().toString());
				/*xml.setValue("text", edtData.getText().toString());
				
				spinnerLevel.setId(xml.getInt("level"));
				
				spinnerType = (Spinner) findViewById(R.id.spTypeBeginner);
				spinnerType.setId(xml.getInt("type"));*/
				
				Log.i("data",xml.toData());
				xml.save();
				if(state==3){attemptSend();}
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case ACTIVITY_CHOOSE_FILE:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				tvFilePath.setText(uri.getPath());
			}
			else {
				tvFilePath.setText("");
			}
			break;
			
		case ACTIVITY_SELECT_CONTACT:
			if (resultCode == RESULT_OK) {
				/*
				 * This is to be implemented later
				 */
				Toast.makeText(getBaseContext(), getString(R.string.error_not_implemented), Toast.LENGTH_LONG).show();
			} else {
				/*
				 * This is to be implemented later
				 */
				Toast.makeText(getBaseContext(), getString(R.string.error_not_implemented), Toast.LENGTH_LONG).show();
			}
			break;
		}
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Assign actions to menu items when they are selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_quit:
				stopWebService();
				finish();
				break;
				
			case R.id.action_settings:
				Intent intent = new Intent(getBaseContext(), PreferencesActivity.class);
				startActivity(intent);
				break;

		}
		return false;
	}
	
	
	/**
	 * Attempts to send the data specified by the form. If there are form
	 * errors (invalid data, missing fields, etc.), those will be presented
	 * and no actual sending attempt is made.
	 */
	public void attemptSend() {
		boolean cancel = false;
		View focusView = null;
		String data = null;
		
		// Reset errors
		edtData.setError(null);
		tvFilePath.setError(null);
		
		// Get the info from the form (either text or file)
		if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextBeginner) {
			data = edtData.getText().toString();
		}
		else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileBeginner) {
			data = tvFilePath.getText().toString();
		}
		else {
			// Unknown id, so exit the function
			cancel = true;
			return;
		}
		
		// Check to see if the form is empty
		if (TextUtils.isEmpty(data)) {
			// If the EditText is empty, set error
			if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextBeginner) {
				edtData.setError(getString(R.string.error_field_required));
				focusView = edtData;
			}
			
			// else if File path is empty, set error
			else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileBeginner) {
				tvFilePath.setError(getString(R.string.error_field_required));
				focusView = tvFilePath;
			}
			cancel = true;
		}
		
		if (cancel) {
			// If something went bad, focus on the field that is invalid
			focusView.requestFocus();
		} else {
			// Execute the AsyncTask in background
			SendTask sendTask = new SendTask();
			sendTask.execute();
		}
	}
	
	/**
	 * Sets the appropriate smiley for Security, Consumption and Overall
	 */
	public void informUser() {
		String levelSelectedString = spinnerLevel.getSelectedItem().toString();
		int levelSelected = Integer.parseInt(levelSelectedString.substring(levelSelectedString.length()-1)); // Get only the level digit and parse to int
		String dataType = spinnerType.getSelectedItem().toString();
		
		if (levelSelected == 1 && dataType.equals("Administrative")) {
			ivSecurity.setImageResource(R.drawable.ic_bad);
			ivConsumption.setImageResource(R.drawable.ic_good);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 2 && dataType.equals("Administrative")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 3 && dataType.equals("Administrative")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 4 && dataType.equals("Administrative")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 1 && dataType.equals("Personal")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 2 && dataType.equals("Personal")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 3 && dataType.equals("Personal")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 4 && dataType.equals("Personal")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 1 && dataType.equals("Medical")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 2 && dataType.equals("Medical")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 3 && dataType.equals("Medical")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 4 && dataType.equals("Medical")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 1 && dataType.equals("Professional")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 2 && dataType.equals("Professional")) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 3 && dataType.equals("Professional")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 4 && dataType.equals("Professional")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 1 && dataType.equals("Banking")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 2 && dataType.equals("Banking")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
		else if (levelSelected == 3 && dataType.equals("Banking")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}
		else if (levelSelected == 4 && dataType.equals("Banking")) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		}

	}
	
	private void stopWebService() {
		// Stop the WebServerService
		Intent intent = new Intent(BeginnerActivity.this,
				WebServerService.class);
		stopService(intent);
	}

	/**
	 * Represents an asynchronous sign up task to execute the sign up process
	 * 
	 * @author andrei
	 * 
	 */
	private class SendTask extends AsyncTask<Void, Integer, String> {
		
		/**
		 * Used for displaying the response from the HttpRequest
		 */
		AlertDialog alertDialog;
		
		/**
		 * This is to be executed before the background task. It is some sort of
		 * preparation for the doInBackground
		 */
		@Override
		protected void onPreExecute() {
			formView.setVisibility(View.GONE); // hide the forms
			statusView.setVisibility(View.VISIBLE); // show the progress
			alertDialog = new AlertDialog.Builder(BeginnerActivity.this).create();
			alertDialog.setTitle(getString(R.string.alertDialogTitle));
			alertDialog.setCancelable(true);
		}
		
		/**
		 * This function is used for updating the UI Thread if needed
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			progressBar.setProgress(values[0]); // feed the progressbar
		}

		/**
		 * This function will be the one executing in the background thread
		 */
		@Override
		protected String doInBackground(Void... params) {
			// Create a new SecureRequest and set the user level as beginner
			SecureRequest sr = new SecureRequest();
			sr.setUserLevel(SecureRequest.USER_LEVEL_BEGINNER);
			
			// Get the info from the forms
			if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextBeginner) {
				sr.setContentType(SecureRequest.TYPE_TEXT);
				sr.setContent(edtData.getText().toString().getBytes());
			}
			else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileBeginner) {
				try {
					sr.setContentType(SecureRequest.TYPE_FILE);
					sr.setContent(new File(tvFilePath.getText().toString()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// Get the target address and port number
			String targetIP = edtDestinationIP.getText().toString();
			int targetPort = Integer.parseInt(edtDestinationPort.getText().toString());
			
			sr.setTarget(targetIP, targetPort);
			
			// If the user specified using a proxy, set it to the HttpClient
			boolean useProxy = prefs.getBoolean("useProxy", false);
			if (useProxy) {
				String proxyIP = prefs.getString("proxyAddress", "127.0.0.1");
				int proxyPort = Integer.parseInt(prefs.getString("proxyPort", "8000"));
				
				sr.setProxy(proxyIP, proxyPort);
				
				// Get the security level and the data type and set them to the security preferences
				String levelSelected = spinnerLevel.getSelectedItem().toString();
				int level = Integer.parseInt(levelSelected.substring(levelSelected.length()-1)); // Get only the level digit and parse to int
				sr.setSecurityLevel(level);
				
				String dataType = spinnerType.getSelectedItem().toString();
				sr.setDataType(dataType);
			}
			
			try {
				// Send the SecureRequest
				sr.send();
				
				// Return the response string that will be passed as a parameter
				// to the onPostExecute method
				return sr.getResponse();

			} catch (Exception e) {
				e.printStackTrace();
			}

			// if something went bad, return null
			return null;
		}
		
		/**
		 * The instructions from this function will be the ones that will be
		 * executed after the background thread will have completed.
		 */
		@Override
		protected void onPostExecute(String result) {
			statusView.setVisibility(View.GONE); // hide the progress
			formView.setVisibility(View.VISIBLE); // show the forms back
			
			alertDialog.setMessage(result); // put the content of response in the dialog
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();

				}
			});
			alertDialog.show(); // show the response in an alert dialog
		}
		
	}
		
}
