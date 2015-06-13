package com.snowplowanalytics.snowplowtrackerdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import com.snowplowanalytics.snowplow.tracker.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplowtrackerdemo.utils.DemoUtils;
import com.snowplowanalytics.snowplowtrackerdemo.utils.TrackerEvents;

public class LiteDemo extends Activity {

    private Tracker tracker;
    private Button _startButton;
    private EditText _uriField;
    private RadioGroup _type, _security;
    private RadioButton _radio_get, _radio_http;
    private TextView _log_output, _events_created, _events_sent, _emitter_online, _emitter_status,
            _database_size;

    private int events_created = 0;
    private int events_sent = 0;
    private boolean isLite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lite_demo);
        isLite = true;
        setupTrackerListener();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Emitter e = tracker.getEmitter();
        e.shutdown();
        isLite = false;
    }

    /**
     * Builds and sets up the Tracker listener for the demo.
     */
    private void setupTrackerListener() {

        _startButton = (Button)findViewById(R.id.btn_lite_start);
        _uriField    = (EditText)findViewById(R.id.emitter_uri_field);
        _type        = (RadioGroup)findViewById(R.id.radio_send_type);
        _security    = (RadioGroup)findViewById(R.id.radio_send_security);
        _radio_get   = (RadioButton)findViewById(R.id.radio_get);
        _radio_http  = (RadioButton)findViewById(R.id.radio_http);
        _log_output  = (TextView)findViewById(R.id.log_output);
        _events_created  = (TextView)findViewById(R.id.created_events);
        _events_sent     = (TextView)findViewById(R.id.sent_events);
        _emitter_online  = (TextView)findViewById(R.id.online_status);
        _emitter_status  = (TextView)findViewById(R.id.emitter_status);
        _database_size   = (TextView)findViewById(R.id.database_size);

        _log_output.setMovementMethod(new ScrollingMovementMethod());
        _log_output.setText("");
        tracker = DemoUtils.getAndroidTrackerLite(getApplicationContext(), getCallback());
        makePollingUpdater();

        _startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Update Emitter Settings
                String uri = _uriField.getText().toString();
                HttpMethod method = _type.getCheckedRadioButtonId() ==
                        _radio_get.getId() ? HttpMethod.GET : HttpMethod.POST;
                RequestSecurity security = _security.getCheckedRadioButtonId() ==
                        _radio_http.getId() ? RequestSecurity.HTTP : RequestSecurity.HTTPS;

                Emitter e = tracker.getEmitter();
                e.setEmitterUri(uri);
                e.setRequestSecurity(security);
                e.setHttpMethod(method);

                // If the URI is not empty send event..
                if (!uri.equals("")) {
                    events_created += 28;
                    _events_created.setText("Made: " + events_created);
                    TrackerEvents.trackAll(tracker);
                } else {
                    updateLogger("Empty URI found, please fill in first\n");
                }
            }
        });
    }

    /**
     * Returns a new Request Callback which logs to a TextView.
     */
    private RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                updateLogger("Emitter Send Success:\n " +
                        "- Events sent: " + successCount + "\n");
                updateEventsSent(successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                updateLogger("Emitter Send Failure:\n " +
                        "- Events sent: " + successCount + "\n " +
                        "- Events failed: " + failureCount + "\n");
                updateEventsSent(successCount);
            }
        };
    }

    /**
     * Updates the logger with a message.
     *
     * @param message the message to add to the log.
     */
    private void updateLogger(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _log_output.append(message);

            }
        });
    }

    /**
     * Updates the events sent counter.
     *
     * @param count the amount of successful events
     */
    private void updateEventsSent(final int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                events_sent += count;
                _events_sent.setText("Sent: " + events_sent);
            }
        });
    }

    /**
     * Starts a polling updater.
     */
    private void makePollingUpdater() {
        DemoUtils.executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        while (isLite) {
                            updateEmitterStats();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
    }

    /**
     * Updates the state of the emitter being online.
     */
    private void updateEmitterStats() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String online = tracker.getEmitter().isOnline() ? "yes" : "no";
                _emitter_online.setText("Online: " + online);
                String status = tracker.getEmitter().getEmitterStatus() ? "yes" : "no";
                _emitter_status.setText("Running: " + status);
                long db_size = tracker.getEmitter().getEventStore().getSize();
                _database_size.setText("DB Size: " + db_size);

                if (status.equals("yes")) {
                    _startButton.setText("Running...");
                } else {
                    _startButton.setText("Start!");
                }
            }
        });
    }
}
