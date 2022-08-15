package com.snowplowanalytics.snowplow.internal.remoteconfiguration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ConfigurationCache {

    @Nullable
    private String cacheFilePath;
    @Nullable
    private FetchedConfigurationBundle configuration;
    @NonNull
    private RemoteConfiguration remoteConfiguration;

    public ConfigurationCache(@NonNull RemoteConfiguration remoteConfiguration) {
        this.remoteConfiguration = remoteConfiguration;
    }

    @Nullable
    public synchronized FetchedConfigurationBundle readCache(@NonNull Context context) {
        if (configuration != null) {
            return configuration;
        }
        loadCache(context);
        return configuration;
    }

    public synchronized void writeCache(@NonNull Context context, @NonNull FetchedConfigurationBundle configuration) {
        this.configuration = configuration;
        storeCache(context, configuration);
    }

    public synchronized void clearCache(@NonNull Context context) {
        String path = getCachePath(context);
        File file = new File(path);
        file.delete();
    }

    // Private methods

    private String getCachePath(@NonNull Context context) {
        if (cacheFilePath != null) {
            return cacheFilePath;
        }
        String cacheDirPath = context.getCacheDir().getAbsolutePath() + File.separator + "snowplow-cache";
        File cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        String fileName = "remoteConfig-" + remoteConfiguration.endpoint.hashCode() + ".data";
        cacheFilePath = cacheDir.getAbsolutePath() + File.separator + fileName;
        return cacheFilePath;
    }

    private void loadCache(@NonNull Context context) {
        String path = getCachePath(context);
        ObjectInputStream objectIn = null;
        try {
            FileInputStream fileIn = new FileInputStream(path);
            objectIn = new ObjectInputStream(fileIn);
            configuration = (FetchedConfigurationBundle)objectIn.readObject();
        } catch (FileNotFoundException e) {
            // TODO log exception
        } catch (IOException e) {
            // TODO log exception
        } catch (ClassNotFoundException e) {
            // TODO log exception
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {}
            }
        }
    }

    private void storeCache(@NonNull Context context, @NonNull FetchedConfigurationBundle configuration) {
        String path = getCachePath(context);
        ObjectOutputStream objectOut = null;
        try {
            FileOutputStream fileOut = new FileOutputStream(path,false);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(configuration);
            fileOut.getFD().sync();
        } catch (IOException e) {
            // TODO log exception
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e) {}
            }
        }
    }
}
