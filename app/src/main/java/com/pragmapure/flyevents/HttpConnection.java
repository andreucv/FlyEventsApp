package com.pragmapure.flyevents;

import android.content.ContentValues;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Certificate;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by andreucortes on 20/02/16.
 */
public class HttpConnection {

    String url;

    public HttpConnection(String url){
        this.url = url;
    }

    private HttpURLConnection setUpConnection(String method) {
        HttpURLConnection urlConnection = null;
        URL urlObj;

        try {
            urlObj = new URL(url);

            urlConnection = (HttpURLConnection) urlObj.openConnection();
            urlConnection.setRequestMethod(method);

        } catch (MalformedURLException | ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlConnection;
    }

    private JSONObject makePostImage(HashMap<String, String> params, String fileUri){
        String charset = "UTF-8";
        File uploadFile1 = new File(fileUri);

        try {
            MultipartUtility multipart = new MultipartUtility(setUpConnection("POST"), charset);
            ArrayList<String> keys = (ArrayList) params.keySet();
            for(String n: keys) {
                multipart.addFormField(n, params.get(n));
            }
            multipart.addFilePart("photo", uploadFile1);
            return new JSONObject(multipart.finish());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject makePostText(HashMap<String, String> params){
        String charset = "UTF-8";
        JSONObject response = null;
        HttpURLConnection connection = setUpConnection("POST");
        connection.setDoOutput(true);

        try{
            MultipartUtility multipart = new MultipartUtility(setUpConnection("POST"), charset);
            ArrayList<String> keys = (ArrayList) params.keySet();
            for (String n : keys) {
                multipart.addFormField(n, params.get(n));
            }

            response = new JSONObject(multipart.finish());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Auxiliary class to create multipart forms and upload them
     */
    private class MultipartUtility {
        private final String boundary;
        private static final String LINE_FEED = "\r\n";
        private HttpURLConnection httpConn;
        private String charset;
        private OutputStream outputStream;
        private PrintWriter writer;


        public MultipartUtility(HttpURLConnection Conn, String charset)
                throws IOException {
            this.charset = charset;

            // creates a unique boundary based on time stamp
            boundary = "===" + System.currentTimeMillis() + "===";

            httpConn = Conn;
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true); // indicates POST method
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);
            httpConn.setRequestProperty("User-Agent", "CodeJava Agent");

            outputStream = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                    true);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        public void addFormField(String name, String value) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName  name attribute in <input type="file" name="..." />
         * @param uploadFile a File to be uploaded
         * @throws IOException
         */
        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a header field to the request.
         *
         * @param name  - name of the header field
         * @param value - value of the header field
         */
        public void addHeaderField(String name, String value) {
            writer.append(name + ": " + value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Completes the request and receives response from the server.
         *
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public String finish() throws IOException {
            String response = "";

            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            // checks server's status code first
            int status = httpConn.getResponseCode();
            BufferedReader reader;
            if (200 <= status && status < 300) {
                reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            } else if (400 <= status && status < 500) {
                reader = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
            } else {
                return null;
            }

            String line = null;
            while ((line = reader.readLine()) != null) {
                response += line;
            }
            reader.close();

            httpConn.disconnect();

            return response;
        }

    }
}
