package pl.dawidkulpa.scm;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import javax.crypto.SecretKey;

public class ServerConnectionManager extends AsyncTask<String, Integer, Integer> {
    public interface OnFinishListener {
        void onFinish(int respCode, JSONObject jObject);
    }

    public interface OnProgressListener {
        void onProgress(int p);
    }

    public static final String CONTENTTYPE_TEXT="application/text";
    public static final String CONTENTTYPE_JSONPATCH="application/json-patch+json";
    public static final String CONTENTTYPE_JSON="application/json";

    public static final String METHOD_POST="POST";
    public static final String METHOD_GET="GET";
    public static final String METHOD_PUT="PUT";

    public static final String OUTTYPE_TEXT="text";
    public static final String OUTTYPE_JSON="json";

    public static final int NO_TIMEOUT=-1;

    private JSONObject jObj;
    private OnFinishListener onFinishListener;
    private OnProgressListener onProgressListener;
    private Query postData;
    private ArrayList<HeaderEntry> headerEntries;
    private Query.BuildType buildType;
    private String contentType;
    private String method;
    private String outtype;
    private int timeout;

    public ServerConnectionManager(Query.BuildType buildType, int timeout, OnFinishListener onFinishListener){
        this.onFinishListener= onFinishListener;
        postData= new Query();
        this.buildType= buildType;
        method=METHOD_POST;
        contentType="";
        outtype=OUTTYPE_JSON;
        headerEntries= new ArrayList<>();
        this.timeout= timeout;
    }

    @Deprecated
    public ServerConnectionManager(Query.BuildType buildType, OnFinishListener onFinishListener){
        this.onFinishListener= onFinishListener;
        postData= new Query();
        this.buildType= buildType;
        method=METHOD_POST;
        contentType="";
        outtype=OUTTYPE_JSON;
        headerEntries= new ArrayList<>();
        timeout= NO_TIMEOUT;
    }

    @Deprecated
    public ServerConnectionManager(OnFinishListener onFinishListener, Query.BuildType buildType){
        this.onFinishListener= onFinishListener;
        postData= new Query();
        this.buildType= buildType;
        method=METHOD_POST;
        contentType="";
        outtype=OUTTYPE_JSON;
        headerEntries= new ArrayList<>();
        timeout= NO_TIMEOUT;
    }

    public void setOnFinishListener(OnFinishListener onFinishListener) {
        this.onFinishListener= onFinishListener;
        postData= new Query();
    }

    public void setOnProgressListener(OnProgressListener onProgressListener){
        this.onProgressListener= onProgressListener;
    }

    public void addPOSTPair(String name, String v){
        postData.addPair(name, v);
    }
    public void addPOSTPair(String name, int v){
        postData.addPair(name, String.valueOf(v));
    }
    public void addPOSTPair(String name, double v){
        postData.addPair(name, String.valueOf(v));
    }
    public void addPOSTPair(String name, Query objDescr){
        postData.addPair(name, objDescr);
    }

    public void addHeaderEntry(String name, String value){
        headerEntries.add(new HeaderEntry(name, value));
    }

    public Query getPOSTQuery(){
        return postData;
    }
    public boolean encryptPOSTQuery(SecretKey key){
        return postData.encryptValue(key);
    }


    public void setContentType(String contentType){
        this.contentType= contentType;
    }

    public void setMethod(String method){
        this.method= method;
    }

    public void start(String adr){
        this.execute(adr, postData.build(buildType));
    }

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
            if(method.equals(METHOD_GET) ) {
                Log.e("URL", params[0]+"?"+params[1]);
                url = new URL(params[0] + "?" + params[1]);
            }else
                url = new URL(params[0]);

            publishProgress(10);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(method);

            if(timeout>0)
                httpURLConnection.setConnectTimeout(timeout);

            publishProgress(20);
            if(!contentType.isEmpty())
                httpURLConnection.setRequestProperty("Content-type", contentType);

            if(headerEntries.size()>0){
                for(int i=0; i<headerEntries.size(); i++){
                    httpURLConnection.setRequestProperty(headerEntries.get(i).getName(), headerEntries.get(i).getValue());
                }
            }

            publishProgress(30);
            httpURLConnection.setDoInput(true);

            if(!method.equals(METHOD_GET)) {
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

            if(outtype.equals(OUTTYPE_JSON))
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
