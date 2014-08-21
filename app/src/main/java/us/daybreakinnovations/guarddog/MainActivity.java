package us.daybreakinnovations.guarddog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity
                          implements SensorEventListener
{
    /** Shared Preferences/Settings object */
    private SharedPreferences mSharedPreferences;
    private Resources mResources;

    /** Sensor variables */
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;

    /** Media Player*/
    private MediaPlayer mMediaPlayer;

    /** Time unit conversions */
    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long MICROSECONDS_PER_SECOND = 1000000;
    private static final long NANOSECONDS_PER_SECOND  = 1000000000;

    private boolean mIsLocked = false;
    private long mLastLock = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Get the SharedPreferences object */
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mResources = getResources();

        /** Request the system sensor manager and the accelerometer */
        mSensorManager          = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorAccelerometer    = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        /** Create the media player */
//        int alarmID;
//        String alarmName = mSharedPreferences.getString(PreferencesActivity.KEY_PREF_ALARM_TONE,
//                mResources.getString(R.string.pref_alarm_tone_default));
        mMediaPlayer = MediaPlayer.create( this, R.raw.alarm_beep_01 );
        // TODO: release() the MediaPlayer in onStop()
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        /** Handle action bar item clicks here */
        if (item.getItemId() == R.id.action_preferences)
        {
            Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
            startActivity(preferencesActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void lockUnlockButtonClicked( View view )
    {
        if ( !mIsLocked )
        {
            /** If device is unlocked / disarmed, start listening for motion */
            deviceLock();
        }
        else
        {
            /** If device is locked / armed, unlock/disarm it and update the button image */
            deviceUnlock();
        }
    }

    private void deviceLock()
    {
        mIsLocked = true;
        Button lockUnlockButton = (Button) findViewById(R.id.btn_lock_unlock);
        mSensorManager.registerListener(
                this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mLastLock = System.nanoTime();

        /** Set the button's text to "Deactivate" or something like that */
        lockUnlockButton.setText(R.string.btn_alarm_locked);

        /** Update the background color to red */
        View mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.drawable.bg_large_red);
    }

    private void deviceUnlock()
    {
        mIsLocked = false;
        Button lockUnlockButton = (Button) findViewById(R.id.btn_lock_unlock);
        // TODO: Implement the PIN check here

        /** Set the button's text to "Activate" or something like that */
        lockUnlockButton.setText(R.string.btn_alarm_unlocked);

        /** Update the background color to green */
        View mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.drawable.bg_large_green);

        if ( mMediaPlayer.isPlaying() )
        {
            mMediaPlayer.stop();
            mMediaPlayer.prepareAsync();
        }
    }

    private void triggerAlarm()
    {
        try
        {
            if ( !mMediaPlayer.isPlaying() )
            {
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
            }
        }
        catch (IllegalStateException ise)
        {
            /** The MediaPlayer hasn't been initialized */
            mMediaPlayer = MediaPlayer.create(this, R.raw.alarm_beep_01 );
        }
    }

    /******************************************************/
    /* SensorEventListener abstract method implementation */
    /******************************************************/

    @Override
    public void onSensorChanged( SensorEvent event )
    {
        /** If the device is not locked, don't do anything */
        if ( !mIsLocked )
            return;

        /** If the sensor was not the accelerometer, don't do anything */
        if ( event.sensor.getType() != Sensor.TYPE_ACCELEROMETER )
            return;

        /** If the event was within the arming period, don't do anything */
        // TODO: Implement a custom numberPreference
        int gracePeriod = Integer.parseInt(mSharedPreferences.getString(
                PreferencesActivity.KEY_PREF_GRACE_PERIOD,
                mResources.getString(R.string.pref_grace_period_default)) );

        if ( event.timestamp - mLastLock < gracePeriod*NANOSECONDS_PER_SECOND )
            return;

        /** Get the delta matrix */
        float[] values = event.values.clone();
        float x = values[0];
        float y = values[1];
        float z = values[2];

        /** Compute the magnitude of the acceleration vector
         *  Direction doesn't matter, since we need to detect movement along any axis */
        float eventAccelerationMagnitude =
                (x*x + y*y + z*z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        /** Immobile device on a flat surface has an acceleration vector magnitude of about 1.01
         *  Magnitude of 2 corresponds roughly to a sharp movement */
        float accelerationThreshold = Float.parseFloat( mSharedPreferences.getString(
                PreferencesActivity.KEY_PREF_MOTION_TOLERANCE,
                mResources.getString(R.string.pref_motion_tolerance_default) ));
        if ( eventAccelerationMagnitude > accelerationThreshold)
            triggerAlarm();
    }

    @Override
    public void onAccuracyChanged( Sensor sensor, int accuracy )
    {
        /** Do nothing */
    }
}
