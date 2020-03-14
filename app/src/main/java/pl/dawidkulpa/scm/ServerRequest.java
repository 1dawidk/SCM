package pl.dawidkulpa.scm;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.crypto.SecretKey;

public class ServerRequest extends AsyncTask<String, Integer, Integer> {
    public interface OnFinishListener {
        void onFinish(int respCode, JSONObject jObject);
    }

    public interface OnProgressListener {
        void onProgress(int p);
    }

    public static final String HEADER_CONTENT_TYPE="Content-type";

    public static final String CONTENT_TYPE_TEXT="application/text";
    public static final String CONTENT_TYPE_JSONPATCH="application/json-patch+json";
    public static final String CONTENT_TYPE_JSON="application/json";

    public static final String METHOD_POST="POST";
    public static final String METHOD_GET="GET";
    public static final String METHOD_PUT="PUT";

    public static final String RESPONSE_TYPE_TEXT ="text";
    public static final String RESPONSE_TYPE_JSON ="json";

    public static final int TIMEOUT_NONE=-1;
    public static final int TIMEOUT_DEFAULT=10000;

    private JSONObject jObj;
    private OnFinishListener onFinishListener;
    private OnProgressListener onProgressListener;
    private Query requestData;
    private ArrayList<HeaderEntry> headerEntries;
    private Query.FormatType queryType;
    private String requestMethod;
    private String responseType;
    private int timeout;

    public ServerRequest(Query.FormatType queryFormat, String requestMethod, String responseType, int timeout, OnFinishListener onFinishListener){
        this.onFinishListener= onFinishListener;
        requestData = new Query();
        this.queryType = queryFormat;
        this.requestMethod =requestMethod;
        this.responseType = RESPONSE_TYPE_JSON;
        headerEntries= new ArrayList<>();
        this.timeout= timeout;
    }

    // Set listeners
    public void setOnFinishListener(OnFinishListener onFinishListener) {
        this.onFinishListener= onFinishListener;
        requestData = new Query();
    }

    public void setOnProgressListener(OnProgressListener onProgressListener){
        this.onProgressListener= onProgressListener;
    }

    // Header
    public void addHeaderEntry(String name, String value){
        headerEntries.add(new HeaderEntry(name, value));
    }

    // Body / Data
    public void addRequestDataPair(String name, String v){
        requestData.addPair(name, v);
    }
    public void addRequestDataPair(String name, int v){
        requestData.addPair(name, String.valueOf(v));
    }
    public void addRequestDataPair(String name, double v){
        requestData.addPair(name, String.valueOf(v));
    }
    public void addRequestDataPair(String name, Query objDescr){
        requestData.addPair(name, objDescr);
    }


    public Query getRequestData(){
        return requestData;
    }



    // Encryption
    public boolean encryptPOSTQuery(SecretKey key){
        return requestData.encryptValue(key);
    }


    // Run
    public void start(String adr){
        this.execute(adr, requestData.build(queryType));
    }



    // JOB
    @Override
    protected void onPreExecute() {
        jObj=null;
    }


    @Override
    protected Integer doInBackground(String... params) {
        int rCode=0;
        InputStream is;
        HttpURLConnection httpURLConnection;
        URL url;

        try {
            publishProgress(0);
            if(requestMethod.equals(METHOD_GET) ) {
                Log.e("URL", params[0]+"?"+params[1]);
                url = new URL(params[0] + "?" + params[1]);
            }else
                url = new URL(params[0]);

            publishProgress(10);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(requestMethod);

            if(timeout>0)
                httpURLConnection.setConnectTimeout(timeout);

            publishProgress(20);

            if(headerEntries.size()>0){
                for(int i=0; i<headerEntries.size(); i++){
                    httpURLConnection.setRequestProperty(headerEntries.get(i).getName(), headerEntries.get(i).getValue());
                }
            }

            publishProgress(30);
            httpURLConnection.setDoInput(true);

            if(!requestMethod.equals(METHOD_GET)) {
                httpURLConnection.setDoOutput(true);
                PrintWriter urlOut = new PrintWriter(httpURLConnection.getOutputStream());
                urlOut.print(params[1]);
                urlOut.close();
            }

            publishProgress(40);
            httpURLConnection.connect();
            rCode=httpURLConnection.getResponseCode();
            publishProgress(50);

            if(rCode>=300)
                is=httpURLConnection.getErrorStream();
            else
                is=httpURLConnection.getInputStream();

            if(responseType.equals(RESPONSE_TYPE_JSON))
                jObj = JSONParser.getJSONFrmUrl(is);
            else {
                jObj= new JSONObject();
                jObj.put("response", getRawText());
            }

            publishProgress(97);
            is.close();
            httpURLConnection.disconnect();
            publishProgress(100);
        } catch (JSONException jsonE){
            Log.e("JSONE", jsonE.getMessage());
            publishProgress(-1);
            rCode=998;
        } catch (IOException ioe){
            Log.e("IOE", ioe.getLocalizedMessage());
            publishProgress(-1);
            rCode=999;
        }

        return rCode;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if(onProgressListener!=null){
            onProgressListener.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer rCode) {
        super.onPostExecute(rCode);

        if(onFinishListener !=null){
            onFinishListener.onFinish(rCode, jObj);
        }
    }

    public JSONObject getData(){
        return jObj;
    }

    private String getRawText(){
        return "No msg!";
    }
}
