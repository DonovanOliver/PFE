package fr.unice.apptest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Documents {
	Context context;
	
	Documents(Context context){
		this.context=context;
	}
	
	private String join(String[] data){
		String res="";
		for(int i=0;i<data.length;i++){
			res+=data[i]+"-";
		}
		return res;
	}
	
	public void saveString(String name,String data) {
	    try {
	        /*OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.getApplicationContext().openFileOutput(name, Context.MODE_PRIVATE));
	        outputStreamWriter.write(data);
	        outputStreamWriter.close();*/
            File myFile = new File(name);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = 
                                    new OutputStreamWriter(fOut);
            myOutWriter.append(data);
            myOutWriter.close();
            fOut.close();
	    	
	    }
	    catch (IOException e) {
	        Log.e("Exception", "File write failed: " + e.toString());
	    } 
	}
	
	public void saveStrings(String name,String[] data){
		saveString(name,join(data));
	}


	public String loadString(String name) {
	    String res = "";
	    try {
	        //InputStream inputStream = context.getApplicationContext().openFileInput(name);

	    	//Get the text file
	    	File file = new File(name);
	        
	        

	        if ( file != null ) {
	            //InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
	            String receiveString = "";
	            StringBuilder stringBuilder = new StringBuilder();

	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                stringBuilder.append(receiveString);
	            }
	            //inputStream.close();
	            res = stringBuilder.toString();
	        }
	    }
	    catch (FileNotFoundException e) {
	        //Log.e("login activity", "File not found: " + e.toString());
	        return null;
	    } catch (IOException e) {
	        //Log.e("login activity", "Can not read file: " + e.toString());
	        return null;
	    }
	    return res;
	}
	
	public String[] loadStrings(String name){
		String tmp=loadString(name);
		if(tmp==null){
			return null;
		}
		String[] res=loadString(name).split("-");
		return res;
	}
}
