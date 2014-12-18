package fr.unice.apptest.activities;

import java.io.File;
import java.io.IOException;

import fr.unice.apptest.R;
import fr.unice.apptest.api.SecureRequest;
import fr.unice.apptest.webserver.WebServerService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Activity is responsible with taking the data from the forms and sending
 * it along with the Security Properties, and their respective algorithms that
 * were selected.
 * 
 * @author andrei
 */
public class AdvancedActivity extends Activity {

	// UI View references
	private View statusView, formView, textTypeView, fileTypeView, destinationManualView, destinationAutomaticView;

	// UI references to the signUpStatusView Views
	private ProgressBar progressBar;

	private EditText edtData, edtDestinationIP, edtDestinationPort;

	private CheckBox chkConfidentiality, chkAuthenticity, chkIntegrity,
			chkNonRepudiation;

	private Spinner spinnerConfidentialityAlgorithms,
			spinnerAuthenticityAlgorithms, spinnerIntegrityAlgorithms;

	private Button btnSend, btnSelectFile, btnAutomatic;

	private TextView tvFilePath;

	private RadioGroup rgDataType, rgDestinationMode;

	private ImageView ivSecurity, ivConsumption, ivOverall;
	
	private Documents documents;

	/**
	 * Used to retrieve user preferences
	 */
	private SharedPreferences prefs;

	private static final int ACTIVITY_CHOOSE_FILE = 1;
	private static final int ACTIVITY_SELECT_CONTACT = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advanced);
		
		documents=new Documents(this);

		if(documents.loadStrings("configAdvanced.txt")==null){
			documents.saveStrings("configAdvanced.txt",new String[]{"127.0.0.1","8002","0","hello world!","true","false","false","false"});
		}
		
		int state=getIntent().getIntExtra("state", 0);
		
		// Get a Shared Preferences instance to get the user preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// Add references to the 2 main Views
		statusView = findViewById(R.id.view_status_Advanced);
		formView = findViewById(R.id.view_form_Advanced);

		// Add references to the file and text Views
		textTypeView = findViewById(R.id.view_Text_Type_Advanced);
		fileTypeView = findViewById(R.id.view_File_Type_Advanced);
		
		// Add references to the destination type Views
		destinationManualView = findViewById(R.id.view_Destination_Manual_Advanced);
		destinationAutomaticView = findViewById(R.id.view_Destination_Automatic_Advanced);

		// References to the progress bar from within the statusView
		progressBar = (ProgressBar) findViewById(R.id.progressBarAdvanced);

		// References to views from within the formView
		edtData = (EditText) findViewById(R.id.edtDataAdvanced);
		edtData.setText(documents.loadStrings("configIntermediate.txt")[3]);
		if(state!=3){edtData.setVisibility(View.GONE);}
		chkConfidentiality = (CheckBox) findViewById(R.id.chkConfidentialityAdvanced);
		chkAuthenticity = (CheckBox) findViewById(R.id.chkAuthenticityAdvanced);
		chkIntegrity = (CheckBox) findViewById(R.id.chkIntegrityAdvanced);
		chkNonRepudiation = (CheckBox) findViewById(R.id.chkNonRepudiationAdvanced);
		spinnerConfidentialityAlgorithms = (Spinner) findViewById(R.id.spConfidentialityAlgorithmsAdvanced);
		spinnerAuthenticityAlgorithms = (Spinner) findViewById(R.id.spAuthenticityAlgorithmsAdvanced);
		spinnerIntegrityAlgorithms = (Spinner) findViewById(R.id.spIntegrityAlgorithmsAdvanced);
		btnSend = (Button) findViewById(R.id.btnSendAdvanced);
		if(state!=3){btnSend.setText("Save");}
		
		if(documents.loadStrings("configIntermediate.txt")[4].equals("true")){
			chkConfidentiality.setChecked(true);
		}
		if(documents.loadStrings("configIntermediate.txt")[5].equals("true")){
			chkAuthenticity.setChecked(true);
		}
		if(documents.loadStrings("configIntermediate.txt")[6].equals("true")){
			chkIntegrity.setChecked(true);
		}
		if(documents.loadStrings("configIntermediate.txt")[7].equals("true")){
			chkNonRepudiation.setChecked(true);
		}
		
		btnAutomatic = (Button) findViewById(R.id.btnAutomaticAdvanced);
		edtDestinationIP = (EditText) findViewById(R.id.edtDestinationIPAdvanced);
		edtDestinationIP.setText(documents.loadStrings("configIntermediate.txt")[0]);
		if(state!=3){edtDestinationIP.setVisibility(View.GONE);}
		edtDestinationPort = (EditText) findViewById(R.id.edtDestinationPortAdvanced);
		edtDestinationPort.setText(documents.loadStrings("configIntermediate.txt")[1]);
		if(state!=3){edtDestinationPort.setVisibility(View.GONE);}

		// References to fileTypeViewAdvanced
		btnSelectFile = (Button) findViewById(R.id.btnSelectFileAdvanced);
		tvFilePath = (TextView) findViewById(R.id.tvFilePathAdvanced);

		rgDataType = (RadioGroup) findViewById(R.id.rgDataTypeAdvanced);
		if(state!=3){rgDataType.setVisibility(View.GONE);}
		rgDestinationMode = (RadioGroup) findViewById(R.id.rgDestinationEnterModeAdvanced);
		if(state!=3){rgDestinationMode.setVisibility(View.GONE);}

		ivSecurity = (ImageView) findViewById(R.id.ivSecurityAdvanced);
		ivConsumption = (ImageView) findViewById(R.id.ivConsumptionAdvanced);
		ivOverall = (ImageView) findViewById(R.id.ivOverallAdvanced);

		informUser();

		// We set a CheckedChangeListener on the RadioGroup
		rgDataType
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {

						// When the text type has been selected, show the text
						// correspondent view
						// When the file type has been selected, show the file
						// correspondent view

						switch (group.getCheckedRadioButtonId()) {
						case R.id.typeTextAdvanced:
							textTypeView.setVisibility(View.VISIBLE);
							fileTypeView.setVisibility(View.GONE);
							break;

						case R.id.typeFileAdvanced:
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
				case R.id.typeManualAdvanced:
					destinationManualView.setVisibility(View.VISIBLE);
					destinationAutomaticView.setVisibility(View.GONE);
					break;

				case R.id.typeAutomaticAdvanced:
					destinationAutomaticView.setVisibility(View.VISIBLE);
					destinationManualView.setVisibility(View.GONE);
					break;

				default:
					break;
				}
				
			}
		});

		// We set a Click Listener for when the select file button is
		// clicked/touched
		btnSelectFile.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent chooseFile, start;
				// Set the type of intent (implicit intent) and it's action (for
				// getting a content)
				chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
				// Set it's file type to recognise (in our case any type)
				chooseFile.setType("file/*");
				start = Intent.createChooser(chooseFile,
						getString(R.string.chooseafile));
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

		// We set a Click Listener for when the button is clicked/touched
		btnSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				documents.saveStrings("configAdvanced.txt", new String[]{edtDestinationIP.getText().toString(),edtDestinationPort.getText().toString(),
						""+rgDestinationMode.getId(),edtData.getText().toString(),""+chkConfidentiality.isChecked(),""+chkAuthenticity.isChecked(),
						""+chkIntegrity.isChecked(),""+chkNonRepudiation.isChecked()});
				attemptSend();

			}
		});

		chkConfidentiality
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						informUser();
						if (isChecked) {
							spinnerConfidentialityAlgorithms
									.setVisibility(View.VISIBLE);
						} else {
							spinnerConfidentialityAlgorithms
									.setVisibility(View.GONE);
						}

					}
				});

		chkAuthenticity
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						informUser();
						if (isChecked) {
							spinnerAuthenticityAlgorithms
									.setVisibility(View.VISIBLE);
						} else {
							spinnerAuthenticityAlgorithms
									.setVisibility(View.GONE);
						}

					}
				});

		chkIntegrity
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						informUser();
						if (isChecked) {
							spinnerIntegrityAlgorithms
									.setVisibility(View.VISIBLE);
						} else {
							spinnerIntegrityAlgorithms.setVisibility(View.GONE);
						}

					}
				});

		chkNonRepudiation
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						informUser();
					}
				});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_CHOOSE_FILE:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				tvFilePath.setText(uri.getPath());
			} else {
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
			Intent intent = new Intent(getBaseContext(),
					PreferencesActivity.class);
			startActivity(intent);
			break;

		}
		return false;
	}

	/**
	 * Attempts to send the data specified by the form. If there are form errors
	 * (invalid data, missing fields, etc.), those will be presented and no
	 * actual sending attempt is made.
	 */
	public void attemptSend() {
		boolean cancel = false;
		View focusView = null;
		String data = null;

		// Reset errors
		edtData.setError(null);
		tvFilePath.setError(null);

		// Get the info from the form (either text or file)
		if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextAdvanced) {
			data = edtData.getText().toString();
		} else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileAdvanced) {
			data = tvFilePath.getText().toString();
		} else {
			// Unknown id, so exit the function
			cancel = true;
			return;
		}

		// Check to see if the form is empty
		if (TextUtils.isEmpty(data)) {
			// If the EditText is empty, set error
			if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextAdvanced) {
				edtData.setError(getString(R.string.error_field_required));
				focusView = edtData;
			}

			// else if File path is empty, set error
			else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileAdvanced) {
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
		boolean confidentiality = chkConfidentiality.isChecked();
		boolean authenticity = chkAuthenticity.isChecked();
		boolean integrity = chkIntegrity.isChecked();
		boolean nonrepudiation = chkNonRepudiation.isChecked();

		if (!confidentiality && !authenticity && !integrity && !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_bad);
			ivConsumption.setImageResource(R.drawable.ic_good);
			ivOverall.setImageResource(R.drawable.ic_bad);
		} else if (confidentiality && !authenticity && !integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && authenticity && !integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && authenticity && integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && authenticity && integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		} else if (confidentiality && !authenticity && integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && !authenticity && integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && !authenticity && !integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (confidentiality && authenticity && !integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_bad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		} else if (!confidentiality && authenticity && !integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_bad);
			ivConsumption.setImageResource(R.drawable.ic_good);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		} else if (!confidentiality && authenticity && integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_bad);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_bad);
		} else if (!confidentiality && authenticity && integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_good);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (!confidentiality && authenticity && !integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		} else if (!confidentiality && !authenticity && integrity
				&& !nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_bad);
			ivConsumption.setImageResource(R.drawable.ic_good);
			ivOverall.setImageResource(R.drawable.ic_preetybad);
		} else if (!confidentiality && !authenticity && integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetybad);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		} else if (!confidentiality && !authenticity && !integrity
				&& nonrepudiation) {
			ivSecurity.setImageResource(R.drawable.ic_preetygood);
			ivConsumption.setImageResource(R.drawable.ic_preetygood);
			ivOverall.setImageResource(R.drawable.ic_preetygood);
		}
	}
	
	private void stopWebService() {
		// Stop the WebServerService
		Intent intent = new Intent(AdvancedActivity.this,
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
			alertDialog = new AlertDialog.Builder(AdvancedActivity.this)
					.create();
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
			sr.setUserLevel(SecureRequest.USER_LEVEL_ADVANCED);

			// Get the info from the forms
			if (rgDataType.getCheckedRadioButtonId() == R.id.typeTextAdvanced) {
				sr.setContentType(SecureRequest.TYPE_TEXT);
				sr.setContent(edtData.getText().toString().getBytes());
			} else if (rgDataType.getCheckedRadioButtonId() == R.id.typeFileAdvanced) {
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
				
				// Get the Security properties to be applied and set them to the
				// Security Preferences

				// If the Confidentiality is checked, put it along with its algorithm
				if (chkConfidentiality.isChecked()) {
					sr.setConfidentiality(true, spinnerConfidentialityAlgorithms
									.getSelectedItem().toString());
				}
				else {
					sr.setConfidentiality(false);
				}

				// If the Authenticity is checked, put it along with its algorithm
				if (chkAuthenticity.isChecked()) {
					sr.setAuthenticity(true, spinnerAuthenticityAlgorithms
							.getSelectedItem().toString());
				}
				else {
					sr.setAuthenticity(false);
				}

				// If the Integrity is checked, put it along with its algorithm
				if (chkIntegrity.isChecked()) {
					sr.setIntegrity(true, spinnerIntegrityAlgorithms
							.getSelectedItem().toString());
				}
				else {
					sr.setIntegrity(false);
				}
				
				// If the Non Repudiation is checked, put it
				if (chkNonRepudiation.isChecked()) {
					sr.setNonRepudiation(true);
				}
				else {
					sr.setNonRepudiation(false);
				}
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

			alertDialog.setMessage(result); // put the content of response in
											// the dialog
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
