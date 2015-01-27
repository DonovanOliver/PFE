package fr.unice.proxy.proxy.direct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

//import fr.unice.proxy.serial.Base64.InputStream;

import android.os.AsyncTask;
import android.util.Log;

public class Test {
	public Test(String url, String name)
	{
		SendHttpRequestTask t = new SendHttpRequestTask();
		
		String[] params = new String[]{url, name};
		t.execute(params);
	}
	
	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			String name = params[1];

			String data = sendHttpRequest(url, name);
			Log.d("DATA","Data [" + data + "]");
			return data;
		}

		@Override
		protected void onPostExecute(String result) {
			/*
			 * edtResp.setText(result); item.setActionView(null);
			 */
			Log.d("RESPONSE ROUTEUR",result);

		}

	}

	private String sendHttpRequest(String url, String name) {
		StringBuffer buffer = new StringBuffer();
		try {
			System.out.println("URL [" + url + "] - Name [" + name + "]");

			// Apache HTTP Reqeust
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			List<NameValuePair> nvList = new ArrayList<NameValuePair>();
			BasicNameValuePair bnvp = new BasicNameValuePair("name", name);
			// We can add more
			nvList.add(bnvp);
			post.setEntity(new UrlEncodedFormEntity(nvList));

			HttpResponse resp = client.execute(post);
			// We read the response
			InputStream is = resp.getEntity().getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				str.append(line + "\n");
			}
			is.close();
			buffer.append(str.toString());
			// Done!
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return buffer.toString();
	}

}
