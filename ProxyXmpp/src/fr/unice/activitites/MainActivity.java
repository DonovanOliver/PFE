package fr.unice.activitites;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.example.bouabid.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import fr.unice.proxy.ProxyManger;



public class MainActivity extends Activity {
	static TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	 super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
         
         
         //Creating Button variable
         Button btStPr = (Button) findViewById(R.id.btStartProxy);
         Button btSwLg = (Button) findViewById(R.id.btShowLog);    
        
        //Adding Listener to button
         btStPr.setOnClickListener(new View.OnClickListener() {
            
             @Override
             public void onClick(View v) {
                 // TODO Auto-generated method stub
            	 File file = new File("sdcard/log.txt");
            	 file.delete();
                 //Creating TextView Variable
                 text = (TextView) findViewById(R.id.tv);
                 text.setMovementMethod(new ScrollingMovementMethod());
 		        int processId = getApplicationInfo().uid;
 		        String excludedUid = String.valueOf(processId);
 		        Log.d("myapps","[Process id ] "+excludedUid);
                 
                 //Sets the new text to TextView (runtime click event)
 		   	try {
 				//new ProxyThread(null,8000,InetAddress.getByName("64.233.167.125"),5222,System.out,System.err);
 		   	ProxyManger.launch();
 			} catch(Exception xc) {
 				text.setText(xc.getMessage());
 			    //System.exit(1);
 				Log.d("myapps","errror "+xc);
 			}
                 text.setText("The button is clicked");
             }
         });
         
         
         
         btSwLg.setOnClickListener(new View.OnClickListener() {
             
             @Override
             public void onClick(View v) {
                 // TODO Auto-generated method stub
            	 int count = 0;
            	 //text.setText("");
            	 try {
         			File myFile = new File("sdcard/log.txt");
         			FileInputStream fIn = new FileInputStream(myFile);
         			BufferedReader myReader = new BufferedReader(
         					new InputStreamReader(fIn));
         			String aDataRow = "";
         			String aBuffer = "";
         			while (((aDataRow = myReader.readLine()) != null)||(count == 3)) {
         				aBuffer += aDataRow + "\n";
         				count++;
         			}
         			text.setText(aBuffer);
         			myReader.close();

         		} catch (Exception e) {
         			Toast.makeText(getBaseContext(), e.getMessage(),
         					Toast.LENGTH_SHORT).show();
         		}
            	 
            	 
             }     
         });
    
    }

   
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
