package com.snowplowanalytics.snowplow.tracker.interceptors;

import android.support.annotation.Nullable;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.zip.GZIPInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class GzipInterceptorTest extends AndroidTestCase {

    public void testGzipInterceptor() throws IOException, InterruptedException {
        String expectedContent = "dummy_request";

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new GzipInterceptor()).build();
        okHttpClient.newCall(new Request.Builder()
                .url(mockWebServer.url("/"))
                .method("POST", RequestBody.create(MediaType.parse("text"), expectedContent))
                .build()).execute();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertEquals("gzip", recordedRequest.getHeader("Content-Encoding"));
        assertEquals(expectedContent, ungzipRequestBody(recordedRequest.getBody()));

        mockWebServer.shutdown();
    }

    @Nullable
    private String ungzipRequestBody(Buffer requestBody) {
        String body = null;
        String charset = "UTF-8";
        try (
                InputStream gzippedResponse = requestBody.inputStream();
                InputStream ungzippedResponse = new GZIPInputStream(gzippedResponse);
                Reader reader = new InputStreamReader(ungzippedResponse, charset);
                Writer writer = new StringWriter();
        ) {
            char[] buffer = new char[10240];
            for (int length; (length = reader.read(buffer)) > 0; ) {
                writer.write(buffer, 0, length);
            }
            body = writer.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }
}