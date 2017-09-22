package com.holenet.nightsky;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.StringBuilderPrinter;

import org.apache.http.params.HttpParams;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.CRL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class NetworkManager {
    final static int CONNECTION_TIME = 5000;
    final static String MAIN_DOMAIN = "http://147.46.209.151:6147/";
    final static String CLOUD_DOMAIN = MAIN_DOMAIN+"cloud/";
    final static int RESULT_CODE_LOGIN_FAILED = 403;
    final static String RESULT_STRING_LOGIN_FAILED = "login failed";

    static String register(Map<String, String> data) {
        Log.e("Network", "register");
        String url = CLOUD_DOMAIN+"register/";

        StringBuilder output = new StringBuilder();
        try {
            String csrftoken = getCsrfToken(url);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            data.put("csrfmiddlewaretoken", csrftoken);
            StringBuilder postData = new StringBuilder();
            for(Map.Entry<String,String> param : data.entrySet()) {
                if(postData.length()!=0)
                    postData.append("&");
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode+"");

            if(resCode==HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while(true) {
                    String line = reader.readLine();
                    if(line==null)
                        break;
                    Log.d("line", line);
                    output.append(line);
                }
                reader.close();
                conn.disconnect();
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    static private boolean login(Context context) {
        Log.e("Network", "login");
        String url = MAIN_DOMAIN+"accounts/login/";
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        SharedPreferences pref = context.getSharedPreferences("settings_login", 0);
        try {
            String csrftoken = getCsrfToken(url);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            Map<String, String> data = new HashMap<>();
            data.put("username", pref.getString(context.getString(R.string.pref_key_username), ""));
            data.put("password", pref.getString(context.getString(R.string.pref_key_password), ""));
            data.put("next", "/cloud/");
            data.put("csrfmiddlewaretoken", csrftoken);

            StringBuilder postData = new StringBuilder();
            for(Map.Entry<String,String> param : data.entrySet()) {
                if(postData.length()!=0)
                    postData.append("&");
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("Network", "login: "+resCode);

            return resCode==HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static String get(Context context, String url) {
        Log.e("Network", "get: "+url);
        if(!login(context))
            return RESULT_STRING_LOGIN_FAILED;

        StringBuilder output = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");

            int resCode = conn.getResponseCode();
            Log.e("response Code", resCode + "");
            if(resCode==HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    Log.d("line", line);
                    output.append(line);
                }
                reader.close();
                conn.disconnect();
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    static String post(Context context, String url, Map<String, String> data) {
        Log.e("Network", "post: "+url);
        if(!login(context))
            return RESULT_STRING_LOGIN_FAILED;

        StringBuilder output = new StringBuilder();
        try {
            String csrftoken = getCsrfToken(url);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            data.put("csrfmiddlewaretoken", csrftoken);
            StringBuilder postData = new StringBuilder();
            for(Map.Entry<String,String> param : data.entrySet()) {
                if(postData.length()!=0)
                    postData.append("&");
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn.setConnectTimeout(CONNECTION_TIME);
            conn.setDoOutput(true);
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode+"");

            if(resCode==HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while(true) {
                    String line = reader.readLine();
                    if(line==null)
                        break;
                    Log.d("line", line);
                    output.append(line);
                }
                reader.close();
                conn.disconnect();
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    static int upload(Context context, Uri uri, String url) {
        Log.e("Network", "upload: "+uri+" / "+url);
        if(!login(context))
            return RESULT_CODE_LOGIN_FAILED;

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fineName = cursor.getString(nameIndex);

        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        int resCode;

        try {
            String csrftoken = getCsrfToken(url);
            URLConnection conn = new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

            OutputStream output = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

            // Send normal param.
            writer.append("--"+boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"csrfmiddlewaretoken\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset="+charset).append(CRLF);
            writer.append(CRLF).append(csrftoken).append(CRLF).flush();

            // Send binary file.
            writer.append("--"+boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"db_file\"; filename=\""+fineName+"\"").append(CRLF);
            writer.append("Content-Type: "+URLConnection.guessContentTypeFromName("asdfasdf.file")).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            int bytesAvailable = inputStream.available();
            int maxBufferSize = 1*1024*1024;
            int bufferSize = Math.min(bytesAvailable,maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            int bytesRead = inputStream.read(buffer, 0, bufferSize);

            while(bytesRead>0) {
                output.write(buffer, 0, bufferSize);
                bytesAvailable = inputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = inputStream.read(buffer, 0, bufferSize);
            }
            output.flush();
            writer.append(CRLF).flush();

            // End of multipart/form-data
            writer.append("--"+boundary+"--").append(CRLF).flush();

            // response from server
            resCode = ((HttpURLConnection)conn).getResponseCode();
            Log.d("resCode", resCode+"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while((line=reader.readLine())!=null) {
                Log.e("upload", line);
                stringBuilder.append(line).append("\n");
            }
            reader.close();
            return resCode;
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Post data rigth after call this method
    static String getCsrfToken(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        String csrftoken = null;
        String cookies = conn.getHeaderFields().get("Set-Cookie").get(0);
        for(String cookie: cookies.split(";")) {
            String[] cook = cookie.split("=");
            if(cook[0].equals("csrftoken")) {
                csrftoken = cook[1];
            }
        }
        conn.disconnect();
        return csrftoken;
    }
}
