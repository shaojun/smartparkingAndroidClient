package com.SmartParking.WebService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Think on 8/31/2015.
 */
public class RestClient {

    private ArrayList<NameValuePair> params;
    private ArrayList<NameValuePair> headers;
    private JSONObject jsonObject;
    private String url;

    private int responseCode;
    private String message;

    private String response;

    public String getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getRequestedUrl() {
        return this.url;
    }

    public RestClient(String url) {
        this.url = url;
        params = new ArrayList<NameValuePair>();
        headers = new ArrayList<NameValuePair>();
//        headers.add(new NameValuePair() {
//            @Override
//            public String getName() {
//                return "Content-Type";
//            }
//
//            @Override
//            public String getValue() {
//                return "application/json";
//            }
//        });
    }

    public void AddParam(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
    }

    public void AddJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public void AddHeader(String name, String value) {
        headers.add(new BasicNameValuePair(name, value));
    }

    public void Execute(String requestMethod) throws Exception {
        switch (requestMethod) {
            case "GET": {
                //add parameters
                String combinedParams = "";
                if (!params.isEmpty()) {
                    combinedParams += "?";
                    for (NameValuePair p : params) {
                        String paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), "UTF - 8");
                        if (combinedParams.length() > 1) {
                            combinedParams += "&" + paramString;
                        } else {
                            combinedParams += paramString;
                        }
                    }
                }

                HttpGet request = new HttpGet(url + combinedParams);

                //add headers
                for (NameValuePair h : headers) {
                    request.addHeader(h.getName(), h.getValue());
                }

                executeRequest(request, url);
                break;
            }
            case "POST": {
                HttpPost request = new HttpPost(url);
                boolean isJsonObjectPost = false;
                //add headers
                for (NameValuePair h : headers) {
                    request.addHeader(h.getName(), h.getValue());
                    if (h.getValue().contains("application/json")) {
                        isJsonObjectPost = true;
                    }
                }

                if (this.jsonObject != null && isJsonObjectPost) {
                    StringEntity se = new StringEntity(this.jsonObject.toString());
                    request.setEntity(se);
                } else if (!params.isEmpty()) {
                    request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                }

                executeRequest(request, url);
                break;
            }
            case "DELETE": {
                //add parameters
                String combinedParams = "";
                if (!params.isEmpty()) {
                    combinedParams += "?";
                    for (NameValuePair p : params) {
                        String paramString = p.getName() + "=" + URLEncoder.encode(p.getValue(), "UTF - 8");
                        if (combinedParams.length() > 1) {
                            combinedParams += "&" + paramString;
                        } else {
                            combinedParams += paramString;
                        }
                    }
                }

                HttpDelete request = new HttpDelete(url + combinedParams);

                //add headers
                for (NameValuePair h : headers) {
                    request.addHeader(h.getName(), h.getValue());
                }

                executeRequest(request, url);
                break;
            }
        }
    }


    private void executeRequest(HttpUriRequest request, String url) throws IOException {
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
        HttpConnectionParams.setSoTimeout(httpParameters, 10000);

        HttpClient client = new DefaultHttpClient(httpParameters);
        HttpResponse httpResponse;
        try {
            httpResponse = client.execute(request);
            responseCode = httpResponse.getStatusLine().getStatusCode();
            message = httpResponse.getStatusLine().getReasonPhrase();
            if (responseCode >= 300)
                throw new HttpResponseException(responseCode, "Http response code is >=300, message: " + message);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                response = convertStreamToString(instream);
                // Closing the input stream will trigger connection release
                instream.close();
            }
        } catch (ClientProtocolException e) {
            client.getConnectionManager().shutdown();
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            client.getConnectionManager().shutdown();
            e.printStackTrace();
            throw e;
        }
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}