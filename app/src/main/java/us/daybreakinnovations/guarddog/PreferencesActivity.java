package us.daybreakinnovations.guarddog;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class PreferencesActivity extends Activity
{
    /** Keys for the different preferences */
    public final static String KEY_PREF_GRACE_PERIOD = "pref_grace_period";
    public final static String KEY_PREF_MOTION_TOLERANCE = "pref_motion_tolerance";
    public final static String KEY_PREF_ALARM_TONE = "pref_alarm_tone";

    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferencesFragment())
                .commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
}
