package com.holenet.nightsky;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;

public class NetworkManager {
    public final static int CONNECTION_TIME_SHORT = 5000;
    public final static int CONNECTION_TIME_LONG = 10000;
    public final static String MAIN_DOMAIN = "http://147.46.209.151:6147/";//*/"http://118.219.23.120:8000/";
    public final static String CLOUD_DOMAIN = MAIN_DOMAIN+"cloud/";
    public final static String SECRET_DOMAIN = MAIN_DOMAIN+"secret/secret/secret/";
    public final static int RESULT_CODE_LOGIN_FAILED = 403;
    public final static String RESULT_STRING_LOGIN_FAILED = "login failed";

    public static String register(Map<String, String> data) {
        String url = MAIN_DOMAIN+"accounts/register/";
        Log.e("Network", "register: "+url);

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

            conn.setConnectTimeout(CONNECTION_TIME_SHORT);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode+"");

            if(resCode==HTTP_OK) {
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

    public static boolean login(Context context) {
        SharedPreferences pref = context.getSharedPreferences("settings_login", 0);
        return login(pref.getString(context.getString(R.string.pref_key_username), ""), pref.getString(context.getString(R.string.pref_key_password), ""));
    }

    public static boolean login(String username, String password) {
        String url = MAIN_DOMAIN+"accounts/login/?next=/cloud/";
        Log.e("Network", "login: "+url);

        try {
            String csrftoken = getCsrfToken(url);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("password", password);
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

            conn.setConnectTimeout(CONNECTION_TIME_SHORT);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("Network", "login: "+resCode);

            if(resCode==HTTP_OK) {
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while(true) {
                    String line = reader.readLine();
                    if(line==null)
                        break;
//                    Log.d("line", line);
                    output.append(line);
                }
                reader.close();

                return Parser.getMetaDataHTML(output.toString(), "view_name").equals("post_list");
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String get(Context context, String url) {
        Log.e("Network", "get: "+url);
        if(!login(context))
            return RESULT_STRING_LOGIN_FAILED;

        StringBuilder output = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            conn.setConnectTimeout(CONNECTION_TIME_SHORT);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");

            int resCode = conn.getResponseCode();
            Log.e("response Code", resCode + "");
            if(resCode==HTTP_OK) {
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
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    public static String post(Context context, String url, Map<String, String> data) {
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
                try {
                    JSONArray ja = new JSONArray(param.getValue());
                    for(int i=0; i<ja.length(); i++) {
                        String value = ja.getString(i);
                        if(postData.length()!=0)
                            postData.append("&");
                        postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                        postData.append('=');
                        postData.append(URLEncoder.encode(value, "UTF-8"));
                    }
                } catch(Exception e) {
                    if(postData.length()!=0)
                        postData.append("&");
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
                }
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            conn.setConnectTimeout(CONNECTION_TIME_SHORT);
            conn.setDoOutput(true);
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setRequestMethod("POST");
            conn.getOutputStream().write(postDataBytes);

            int resCode = conn.getResponseCode();
            Log.d("response Code", resCode+"");

            if(resCode==HTTP_OK) {
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
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return output.toString();
    }

    public static String upload(Context context, String url, String modelName, String path) {
        Uri uri = Uri.fromFile(new File(path));
        return upload(context, url, modelName, uri);
    }

    public static String upload(Context context, String url, String modelName, Uri uri) {
        Log.e("Network", "upload: "+uri+" / "+url);
        if(!login(context))
            return RESULT_STRING_LOGIN_FAILED;

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(nameIndex);
        if(fileName.length()>100) {
            fileName = fileName.replace(fileName.split("\\.")[0], fileName.split("\\.")[0].substring(0, 100-(fileName.split("\\.").length>1 ? fileName.split("\\.")[1].length()+1 : 0)));
        }
        cursor.close();
        Log.e("upload", fileName);

        String charset = "UTF-8";
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        int resCode;

        try {
            String csrftoken = getCsrfToken(url);
            URLConnection conn = new URL(url).openConnection();

            conn.setConnectTimeout(CONNECTION_TIME_LONG);
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
            writer.append("Content-Disposition: form-data; name=\""+modelName+"\"; filename=\""+fileName+"\"").append(CRLF);
            writer.append("Content-Type: "+URLConnection.guessContentTypeFromName(fileName)).append(CRLF);
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
            String line;
            StringBuilder response = new StringBuilder();
            while((line=reader.readLine())!=null) {
                Log.e("upload", line);
                response.append(line).append("\n");
            }
            reader.close();
            return response.toString();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int download(Context context, String url, File path) {
        Log.e("Network", "download: "+url);
        if(!login(context))
            return RESULT_CODE_LOGIN_FAILED;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            conn.setConnectTimeout(CONNECTION_TIME_LONG);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");

            int resCode = conn.getResponseCode();

            if(resCode==HTTP_OK) {
                String fileName = "";

                String disposition = conn.getHeaderField("Content-Disposition");
                String contentType = conn.getContentType();
                int contentLength = conn.getContentLength();

                if(disposition!=null) {
                    int index = disposition.indexOf("filename=");
                    if(index>0) {
                        fileName = disposition.substring(index+9, disposition.length()).trim();
                    }
                } else {
                    fileName = url.substring(url.substring(0, url.lastIndexOf("/")).lastIndexOf("/")+1, url.length());
                }

                InputStream inputStream = conn.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(path);

                int bytesRead;
                byte[] buffer = new byte[4096];
                while((bytesRead=inputStream.read(buffer))>0) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
            }

            return resCode;
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Post data rigth after call this method
    public static String getCsrfToken(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setConnectTimeout(CONNECTION_TIME_SHORT);
        String csrftoken = null;
        String cookies = conn.getHeaderField("Set-Cookie");
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
