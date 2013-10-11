package com.macrowen.macromap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;

import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class TitlebarActivity extends Activity
{
	Handler	mHandler	= new Handler();
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// getActionBar().setIcon(R.drawable.header_return_button);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.titlebar);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
		setOnClickTitleButtonListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				KeyEvent newEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
				// dispatchKeyEvent(newEvent);
				onKeyDown(KeyEvent.KEYCODE_BACK, newEvent);
			}
		});
	}

	protected void enableSearch(boolean enable)
	{
		Button titleButton = (Button) findViewById(R.id.search_button);
		titleButton.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
	}

	protected void setOnClickSearchButtonListener(OnClickListener onClick)
	{
		Button titleButton = (Button) findViewById(R.id.search_button);
		titleButton.setOnClickListener(onClick);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		TextView textView = (TextView) findViewById(R.id.head_center_text);
		textView.setText(title);
	}

	protected void setOnClickTitleButtonListener(OnClickListener onClick)
	{
		Button titleButton = (Button) findViewById(R.id.head_title_button);
		titleButton.setOnClickListener(onClick);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.shop_detail, menu);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	protected void downloadJson(String url, Runnable onCompleted)
	{
		new DownloadJson(url, onCompleted).start();
	}

	protected JSONObject getJson(String url)
	{
		try
		{
			File file = Environment.getExternalStorageDirectory();
			file = new File(file, "/Palmap/MacroMap/" + Base64.encodeToString(url.getBytes(), Base64.NO_WRAP));
			FileInputStream input = new FileInputStream(file);
			byte[] buf = new byte[input.available()];
			input.read(buf);
			input.close();
			String json = EncodingUtils.getString(buf, "UTF-8");
			JSONObject obj = new JSONObject(json);
			return obj;
		}
		catch (Throwable e)
		{
			logd(e);
		}
		return null;
	}

	class DownloadJson extends Thread
	{
		String		mUrl;
		Runnable	mOnCompleted;

		DownloadJson(String url, Runnable onCompleted)
		{
			mUrl = url;
			mOnCompleted = onCompleted;
		}

		@Override
		public void run()
		{
			try
			{
				File file = Environment.getExternalStorageDirectory();
				file = new File(file, "/Palmap/MacroMap/" + Base64.encodeToString(mUrl.getBytes(), Base64.NO_WRAP));
				logd("url=" + mUrl + ", file=" + file.getAbsolutePath());
				if (file.length() < 4)
				{
					file.getParentFile().mkdirs();
					file.createNewFile();
					downloadJson(mUrl, file);
				}
				if (mOnCompleted != null)
				{
					mHandler.post(mOnCompleted);
				}
			}
			catch (Throwable e)
			{
				logd(e);
			}
		}

		int downloadJson(String u, File file)
		{
			try
			{
				URL url = new URL(u);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setRequestProperty("Accept", "application/json");
				// urlConnection.setRequestMethod("GET");
				// urlConnection.setDoOutput(true);
				urlConnection.connect();
				InputStream inputStream = urlConnection.getInputStream();
				int totalSize = urlConnection.getContentLength();
				int downloadedSize = 0;
				byte[] buffer = new byte[1024];
				int bufferLength = 0;
				FileOutputStream fileOutput = new FileOutputStream(file);
				while ((bufferLength = inputStream.read(buffer)) > 0)
				{
					fileOutput.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;
				}
				fileOutput.close();
				inputStream.close();
			}
			catch (Throwable e)
			{
				logd(e);
			}
			return 0;
		}

	}

	protected void logd(String log)
	{
		StackTraceElement ste = new Throwable().getStackTrace()[1];
		Log.d(ste.getClassName(), "at " + ste.getMethodName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")" + "  " + log);
	}

	protected void logd(Throwable e)
	{
		e.printStackTrace();
	}
}
