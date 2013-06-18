package mb.android.flyerchecker.net;

import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Handler;
import android.os.Message;

/**
 * Core thread class to perform network data retrieval
 * @author zhuoy_li
 *
 */
abstract public class Networker extends Thread {
	
	/**
	 * Class representing thread running results
	 * @author zhuoy_li
	 *
	 */
	public class Result{
		/** 
		 * url: on which the thread runs
		 * obj: the Object of actual result to be returned
		 */
		public String url;
		public Object obj;
		
		public Result(String u, Object o){
			this.url = u;
			this.obj = o;
		}
	}

	public class NETWORKER_RESULT{
		final public static int SUCCESS = 1;
		final public static int FAILURE = 2;
	}

	/**
	 * We ask subclass to implement the actual operation on httpconnection response
	 * @param msg The message to be marked according to network result
	 * @throws Exception
	 */
	abstract protected void procNetworkResponse(Message msg) throws Exception;
	
	static protected void setMsgSuccess(Message msg, boolean success){
		msg.what = idMsg;
		msg.arg1 = 
				success? NETWORKER_RESULT.SUCCESS : NETWORKER_RESULT.FAILURE;
	}
	
	
	Handler handler;
	String url;
	Result rv;
	HttpURLConnection urlConn;
	static int idMsg;
	
	static public Message generateEmptyMessage(boolean success){
		Message msg = new Message();
		msg.what = idMsg;
		
		setMsgSuccess(msg, success);
		return msg;
	}
	
	public Networker(int id, Handler handler,String url){
		idMsg = id;
		
		this.handler = handler;
		this.url = url;
	}
	
	public void run(){
		// we should stop if the thread is stopped
		while(!Thread.currentThread().isInterrupted()){
			Message msg = new Message();
	
			try{
				
				URL url = new URL(this.url);
				urlConn = (HttpURLConnection) url.openConnection();
				urlConn.setDoInput(true);
				
				// we just assume always using GET method
				urlConn.setRequestMethod("GET");
				
				int codeHttp = urlConn.getResponseCode();
				
				// we only accept HTTP_200
				if(codeHttp != HttpURLConnection.HTTP_OK)
					throw new Exception();
				procNetworkResponse(msg);
				
			}catch(Exception ex){
				setMsgSuccess(msg, false);
			}
	
			handler.sendMessage(msg);
			
			// stop the thread, i.e. only run ONCE!
			Thread.currentThread().interrupt();
		}
	}

}
