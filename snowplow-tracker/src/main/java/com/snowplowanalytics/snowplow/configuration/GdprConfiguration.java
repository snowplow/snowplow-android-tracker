package com.snowplowanalytics.snowplow.configuration;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;
import com.snowplowanalytics.snowplow.util.Basis;

import org.json.JSONObject;

/**
 * This class allows the GDPR configuration of the tracker.
 */
public class GdprConfiguration implements Configuration, GdprConfigurationInterface {

    /** Basis for processing. */
    @NonNull
    public final Basis basisForProcessing;
    /** ID of a GDPR basis document. */
    @NonNull
    public final String documentId;
    /** Version of the document. */
    @NonNull
    public final String documentVersion;
    /** Description of the document. */
    @NonNull
    public final String documentDescription;

    // Constructors

    /**
     * Enables GDPR context to be sent with each event.
     * @param basisForProcessing GDPR Basis for processing.
     * @param documentId ID of a GDPR basis document.
     * @param documentVersion Version of the document.
     * @param documentDescription Description of the document.
     */
    public GdprConfiguration(@NonNull Basis basisForProcessing,
                             @NonNull String documentId,
                             @NonNull String documentVersion,
                             @NonNull String documentDescription)
    {
        this.basisForProcessing = basisForProcessing;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
        this.documentDescription = documentDescription;
    }

    // Getters

    /** Basis for processing. */
    @Override
    @NonNull
    public Basis getBasisForProcessing() {
        return basisForProcessing;
    }

    /** ID of a GDPR basis document. */
    @Override
    @NonNull
    public String getDocumentId() {
        return documentId;
    }

    /** Version of the document. */
    @Override
    @NonNull
    public String getDocumentVersion() {
        return documentVersion;
    }

    /** Description of the document. */
    @Override
    @NonNull
    public String getDocumentDescription() {
        return documentDescription;
    }

    // Copyable

    @NonNull
    @Override
    public GdprConfiguration copy() {
        return new GdprConfiguration(basisForProcessing, documentId, documentVersion, documentDescription);
    }

    // Parcelable

    protected GdprConfiguration(@NonNull Parcel in) {
        basisForProcessing = Basis.valueOf(in.readString());
        documentId = in.readString();
        documentVersion = in.readString();
        documentDescription = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(basisForProcessing.name());
        dest.writeString(documentId);
        dest.writeString(documentVersion);
        dest.writeString(documentDescription);
    }

    public static final Creator<GdprConfiguration> CREATOR = new Parcelable.Creator<GdprConfiguration>() {
        @Override
        public GdprConfiguration createFromParcel(Parcel in) {
            return new GdprConfiguration(in);
        }

        @Override
        public GdprConfiguration[] newArray(int size) {
            return new GdprConfiguration[size];
        }
    };
}
