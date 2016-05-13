package com.example.myproject.webviewstarter;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;

    private static final int FILECHOOSER_RESULTCODE = 1;
    private static final String ANDROID_ASSET_URI = "file:///android_asset/";
    private static final String STATIC_URL = "http://local.host/static/";

    private static final String HTML_DIR = "www";
    private static final String STATIC_DIR = "static";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setWebView();
    }

    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    private void setWebView() {
        webView = (WebView) findViewById(R.id.webView);

        WebSettings web_settings = webView.getSettings();
        web_settings.setJavaScriptEnabled(true);
        web_settings.setAllowFileAccess(true);
        web_settings.setAllowContentAccess(true);
        web_settings.setAllowFileAccessFromFileURLs(true);
        web_settings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebChromeClient(new WebChromeClient() {
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                // Update message
                mUploadMessage = uploadMsg;

                try {
                    // Create AndroidExampleFolder at sdcard
                    File imageStorageDir = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES)
                            , getApplicationName(getApplicationContext()));

                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }

                    // Create camera captured image file path and name
                    File file = new File(
                            imageStorageDir + File.separator + "IMG_"
                                    + String.valueOf(System.currentTimeMillis())
                                    + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file);

                    // Camera capture image intent
                    final Intent captureIntent = new Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    // Create file chooser intent
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                    // Set camera intent to file chooser
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                            , new Parcelable[] { captureIntent });

                    // On select image call onActivityResult method of activity
                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                }
                catch (Exception e) {
                    Toast.makeText(getBaseContext(), "Exception:" + e,
                            Toast.LENGTH_LONG).show();
                }
            }

            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(getApplicationName(getApplicationContext()), cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("bbmi:") || url.startsWith("sms:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(url));
                    startActivity(intent);

                    return true;
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.startsWith(STATIC_URL)) {
                    String assetpath = url.substring(STATIC_URL.length());

                    // ignore querystring
                    if (assetpath.contains("?")) {
                        assetpath = assetpath.substring(0, assetpath.indexOf("?"));
                    }

                    String mime = "text/plain";


                    if (assetpath.endsWith(".css")) {
                        mime = "text/css";
                    } else if (assetpath.endsWith(".js")) {
                        mime = "text/javascript";
                    } else if (assetpath.endsWith(".jpg") || assetpath.endsWith(".jpeg")) {
                        mime = "image/jpeg";
                    } else if (assetpath.endsWith(".png")) {
                        mime = "image/png";
                    } else if (assetpath.endsWith(".ico")) {
                        mime = "image/x-icon";
                    }

                    return loadFromAssets(STATIC_DIR + "/" + assetpath, mime, "");
                }

                return null;
            }

            private WebResourceResponse loadFromAssets(String assetPath, String mimeType, String encoding) {
                AssetManager assetManager = getAssets();
                InputStream input = null;

                try {
                    Log.d(getApplicationName(getApplicationContext()), "Loading from assets: " + assetPath);

                    input = assetManager.open(assetPath);
                    WebResourceResponse response = new WebResourceResponse(mimeType, encoding, input);

                    return response;
                } catch (IOException e) {
                    Log.e(getApplicationName(getApplicationContext()), "Error loading " + assetPath + " from assets: " +
                            e.getMessage());
                }
                return null;
            }
        });

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.loadUrl(ANDROID_ASSET_URI + HTML_DIR + "/index.html");
    }

    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode==FILECHOOSER_RESULTCODE) {
            if (null == this.mUploadMessage) {
                return;
            }

            Uri result = null;

            try {
                if (resultCode != RESULT_OK) {
                    result = null;
                } else {
                    // retrieve from the private variable if the intent is null
                    result = intent == null ? mCapturedImageURI : intent.getData();
                }
            }
            catch(Exception e) {
                Toast.makeText(getApplicationContext(), "activity :"+e,
                        Toast.LENGTH_LONG).show();
            }

            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }
}
