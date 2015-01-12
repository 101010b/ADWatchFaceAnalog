package com.alphadraco.watchface.analog;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class AnalogConfActivity extends Activity {
    private static final String TAG = "AlphaDracoWFConfig";
    //private TextView mTextView;
//    private Button mStartTimer;
//    private Button mStopTimer;
    SharedPreferences prefs;

    private TimerFragment mTimerFragment;
    private CountdownFragment mCountdownFragment;
    private CountdownStartStopFragment mCountdownStartStopFragment;
    private TimePicker timePicker;


    private class TimerFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View RetView = inflater.inflate(R.layout.fragment_start_timer, container, false);
            RetView.findViewById(R.id.BtnStartTimer).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","START");
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                    finish();
                }
            });
            RetView.findViewById(R.id.BtnStopTimer).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","STOP");
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                    finish();
                }
            });
            RetView.findViewById(R.id.showTimerHand).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","SHOWTIMERHAND");
                    intent.putExtra("SHOW",((ToggleButton)v).isChecked());
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                }
            });

            Intent intent = new Intent("AlphaDracoTMR");
            intent.putExtra("message","STATUS");
            LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);

            return RetView;
        }
    }

    private class CountdownFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View RetView = inflater.inflate(R.layout.fragment_countdown_config, container, false);
            timePicker = (TimePicker) RetView.findViewById(R.id.timePicker);
            timePicker.setIs24HourView(true);

            timePicker.setCurrentHour((int)prefs.getLong("cntdown.hours",1));
            timePicker.setCurrentMinute((int)prefs.getLong("cntdown.minutes",1));
            return RetView;
        }
        @Override
        public void onDestroy() {
            //SharedPreferences prefs=getSharedPreferences("com.alphadraco.watchface.analog",Context.MODE_PRIVATE);
            SharedPreferences.Editor e=prefs.edit();
            e.putLong("cntdown.hours",timePicker.getCurrentHour());
            e.putLong("cntdown.minutes", timePicker.getCurrentMinute());
            e.commit();
            super.onDestroy();
        }
    }

    private class CountdownStartStopFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View RetView = inflater.inflate(R.layout.fragment_countdown_startstop, container, false);
            RetView.findViewById(R.id.BtnStartCountdown).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","CTDSTART");
                    int hours = timePicker.getCurrentHour();
                    int minutes=timePicker.getCurrentMinute();
                    intent.putExtra("TIME",(int)hours*3600+minutes*60);
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                    finish();
                }
            });
            RetView.findViewById(R.id.BtnStopCountdown).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","CTDSTOP");
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                    finish();
                }
            });
            RetView.findViewById(R.id.showCountdownHand).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("AlphaDracoTMR");
                    intent.putExtra("message","SHOWCOUNTDOWNHAND");
                    intent.putExtra("SHOW",((ToggleButton)v).isChecked());
                    LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                }
            });
            Intent intent = new Intent("AlphaDracoTMR");
            intent.putExtra("message", "STATUS");
            LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);

            return RetView;
        }
    }

    public class ConfGridPagerAdapter extends FragmentGridPagerAdapter {

        private final Context mContext;

        public ConfGridPagerAdapter(Context ctx, FragmentManager fm) {
            super(fm);
            mContext = ctx;
        }

        /*
        static final int[] BG_IMAGES = new int[] {
            R.drawable.debug_background_1, ...
            R.drawable.debug_background_5
        };
        */

        // Create a static set of pages in a 2D array
        // private final Page[][] PAGES = { ... };

        // Obtain the UI fragment at the specified position
        @Override
        public Fragment getFragment(int row, int col) {
            // Page page = PAGES[row][col];
            /*String title =
                    page.titleRes != 0 ? mContext.getString(page.titleRes) : null;
            String text =
                    page.textRes != 0 ? mContext.getString(page.textRes) : null;
            */

            // CardFragment fragment;
            if (row == 0) {
                // Timer
                if (mTimerFragment == null)
                    mTimerFragment = new TimerFragment();
                return mTimerFragment;
            }

            if (row == 1) {
                // Count Down
                if (col == 0) {
                    if (mCountdownStartStopFragment == null)
                        mCountdownStartStopFragment = new CountdownStartStopFragment();
                    return mCountdownStartStopFragment;
                }
                if (col == 1) {
                    if (mCountdownFragment == null)
                        mCountdownFragment = new CountdownFragment();
                    return mCountdownFragment;
                }
            }


            return null;

            /*     fragment = CardFragment.create(title, text, page.iconRes);


            // Advanced settings (card gravity, card expansion/scrolling)
            fragment.setCardGravity(page.cardGravity);
            fragment.setExpansionEnabled(page.expansionEnabled);
            fragment.setExpansionDirection(page.expansionDirection);
            fragment.setExpansionFactor(page.expansionFactor);
            return fragment;*/
        }

        /*
        // Obtain the background image for the page at the specified position
        @Override
        public ImageReference getBackground(int row, int column) {
            return ImageReference.forDrawable(R.drawable.alphadraco_analog);
        }
        */

        // Obtain the number of pages (vertical)
        @Override
        public int getRowCount() {
            return 2;
        }

        // Obtain the number of pages (horizontal)
        @Override
        public int getColumnCount(int rowNum) {
            switch (rowNum) {
                case 0: return 1;
                case 1: return 2;
            }
            return 0;
        }

    }

    // private GoogleApiClient mGoogleApiClient=null;


    private BroadcastReceiver LocalMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ToggleButton tb =(ToggleButton)findViewById(R.id.showTimerHand);
            if (tb != null) tb.setChecked(intent.getBooleanExtra("TIMERHAND",false));
            tb = (ToggleButton)findViewById(R.id.showCountdownHand);
            if (tb != null) tb.setChecked(intent.getBooleanExtra("COUNTDOWNHAND",false));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs=getSharedPreferences("com.alphadraco.watchface.analog",Context.MODE_PRIVATE);

        setContentView(R.layout.activity_alphadraco_config);

        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ConfGridPagerAdapter(this,getFragmentManager()));

        // Register broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(LocalMessageReceiver,
                new IntentFilter("AlphaDracoCFG"));

        // Request status from the watch face service
        Intent intent = new Intent("AlphaDracoTMR");
        intent.putExtra("message","STATUS");
        LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);



        /*pager.findViewById(R.id.BtnStartTimer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("AlphaDracoTMR");
                intent.putExtra("message","START");
                LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                finish();
            }
        });*/
        /*
        this.findViewById(R.id.BtnStopTimer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("AlphaDracoTMR");
                intent.putExtra("message","STOP");
                LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                finish();
            }
        });
        */


        /*
        setContentView(R.layout.activity_analog_conf);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                // mTextView = (TextView) stub.findViewById(R.id.text);
                mStartTimer = (Button) stub.findViewById(R.id.BtnStartTimer);
                mStartTimer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Clicked");
                        }
                        Intent intent = new Intent("AlphaDracoTMR");
                        intent.putExtra("message","START");
                        LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                        //if (mGoogleApiClient.isConnected()) {
                        //    Wearable.MessageApi.sendMessage(mGoogleApiClient,TAG,"START",null);
                        //}
                        finish();
                    }
                });

                mStopTimer = (Button) stub.findViewById(R.id.BtnStopTimer);
                mStopTimer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Clicked");
                        }
                        Intent intent = new Intent("AlphaDracoTMR");
                        intent.putExtra("message","STOP");
                        LocalBroadcastManager.getInstance(AnalogConfActivity.this).sendBroadcast(intent);
                        //if (mGoogleApiClient.isConnected()) {
                        //    Wearable.MessageApi.sendMessage(mGoogleApiClient,TAG,"START",null);
                        //}
                        finish();
                    }
                });


            }
        });
        */

        /*mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle connectionHint) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                }
                @Override
                public void onConnectionSuspended(int cause) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                }
            })
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult result) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                }
            })
            .addApi(Wearable.API)
            .build();*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        //if (mGoogleApiClient != null)
        //    mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        //if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
        //    mGoogleApiClient.disconnect();
        //}
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // prefs.edit().commit();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(LocalMessageReceiver);
        super.onDestroy();
    }

}
