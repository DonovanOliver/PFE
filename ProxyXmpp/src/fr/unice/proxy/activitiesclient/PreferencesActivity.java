package fr.unice.proxy.activitiesclient;

import fr.unice.proxyclient1.R;
import fr.unice.proxyclient1.R.xml;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.view.Menu;

/**
 * Activity that display the Preferences that can be set by the user for the sign up process
 * @author andrei
 *
 */
public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Add the entries for dedicated server info
		addPreferencesFromResource(R.xml.localpreferences);
		
		// Add a header for proxy server info
		PreferenceCategory header = new PreferenceCategory(this);
		header.setTitle("Proxy Server Settings");
		getPreferenceScreen().addPreference(header);

		// Add the entries for proxy server info
		addPreferencesFromResource(R.xml.proxyserverpreferences);
		
		// Add a header for the proxy info
		PreferenceCategory header2 = new PreferenceCategory(this);
		header2.setTitle("Dedicated Server Settings");
		getPreferenceScreen().addPreference(header2);
		
		// Add the entries for dedicated server info
		addPreferencesFromResource(R.xml.thirdpartypreferences);
		
		// Bind the summaries of EditText preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
		bindPreferenceSummaryToValue(findPreference("pcPort"));
		bindPreferenceSummaryToValue(findPreference("psPort"));
		bindPreferenceSummaryToValue(findPreference("proxyServerAddress"));
		bindPreferenceSummaryToValue(findPreference("proxyServerPort"));
		bindPreferenceSummaryToValue(findPreference("dedicatedServerAddress"));
		bindPreferenceSummaryToValue(findPreference("dedicatedServerPort"));
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			
			// Set the summary to the value's simple string representation.
			preference.setSummary(stringValue);
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

}