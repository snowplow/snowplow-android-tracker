package com.snowplowanalytics.snowplowtrackerdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.content.Context;
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
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplowtrackerdemo.utils.DemoUtils;
import com.snowplowanalytics.snowplowtrackerdemo.utils.TrackerEvents;

public class RxDemo extends Activity {

    private Tracker tracker;
    private Button _startButton;
    private EditText _uriField;
    private RadioGroup _type, _security;
    private RadioButton _radio_get, _radio_http;
    private TextView _log_output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_demo);
        setupTrackerListener();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Emitter e = tracker.getEmitter();
        e.shutdown();
        e.getEventStore().removeAllEvents();
    }

    /**
     * Builds and sets up the Tracker listener for the demo.
     */
    private void setupTrackerListener() {

        _startButton = (Button)findViewById(R.id.btn_rx_start);
        _uriField    = (EditText)findViewById(R.id.emitter_uri_field);
        _type        = (RadioGroup)findViewById(R.id.radio_send_type);
        _security    = (RadioGroup)findViewById(R.id.radio_send_security);
        _radio_get   = (RadioButton)findViewById(R.id.radio_get);
        _radio_http  = (RadioButton)findViewById(R.id.radio_http);
        _log_output  = (TextView)findViewById(R.id.log_output);

        _log_output.setMovementMethod(new ScrollingMovementMethod());
        _log_output.setText("");
        tracker = getTracker();

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
                    _startButton.setText("Sending events...");
                    updateLogger("Sending events to endpoint:\n");
                    TrackerEvents.trackAll(tracker);
                } else {
                    updateLogger("Empty URI found, please fill in first\n");
                }
            }
        });
    }

    /**
     * Returns a Lite Tracker instance.
     */
    private Tracker getTracker() {
        Context context = getApplicationContext();
        Emitter emitter = DemoUtils.getEmitterRx(context, getCallback());
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerRx(emitter, subject);
    }

    /**
     * Returns a new Request Callback which logs to a TextView.
     */
    private RequestCallback getCallback() {
        return new RequestCallback() {

            @Override
            public void onSuccess(int successCount) {
                updateLogger("Emitter Success:\n " + "- Events sent: " + successCount + "\n");
                resetStartButton();
            }

            @Override
            public void onFailure(int successCount, int failureCount) {
                updateLogger("Emitter Failure:\n " +
                        "- Events sent: " + successCount + "\n " +
                        "- Events failed: " + failureCount + "\n");
                resetStartButton();
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
     * Resets the start button from running state.
     */
    private void resetStartButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _startButton.setText("Send more?");
            }
        });
    }
}
