package mb.android.flyerchecker.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Core utility class to encapsulate common operation
 * @author zhuoy_li
 *
 */
public class Helper {

	private Helper(){}
	
	/** missbaggio.dyndns.org is my home server */
	final  static private String URL_CHECKCONNECT = "http://192.168.1.253:8080/AndroidServer/Check";
	final  static private String URL_QUERY = "http://192.168.1.253:8080/AndroidServer/Server";
	
	static private String versionApp;
	static public void setVersionApp(String ver){ versionApp = ver; }
	
	/**
	 * We mark the network query with app version
	 * so that server might be able to response according to different
	 * version of apps
	 * @param query
	 * @return
	 */
	static protected String markVersion(String query){
		return query.concat("&ver="+versionApp);
	}
	
	public static String prepUrlCheckConnect(){
		return markVersion(URL_CHECKCONNECT.concat("?q="+URL_CHECKCONNECT));
	}
	
	public static boolean isConnectOK(String urlCheck, String result){
		if(null == result || result.equals(URL_CHECKCONNECT) == false ) return false;
		return true;
	}
	
	public static String getQueryVendorList(){
		return markVersion(URL_QUERY.concat("?q=vendorlist"));
	}
	
	public static String getQueryFlyerUrls(int idxVendor){
		return markVersion(URL_QUERY.concat("?q=vendorflyerurls&idx="+idxVendor));
	}
	
	/**
	 * Check the network interface (system-level) connectivity
	 * ATTENTION: permissions must be set in manifest.xml
	 * @param context
	 * @return
	 */
	public static boolean isNetworkReady(Context context){


		boolean status=false;
	    try{
	        ConnectivityManager cm = (ConnectivityManager) 
	        		context.getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo netInfo = cm.getNetworkInfo(0);
	        if (netInfo != null && netInfo.getState()==NetworkInfo.State.CONNECTED) {
	            status= true;
	        }else {
	            netInfo = cm.getNetworkInfo(1);
	            if(netInfo!=null && netInfo.getState()==NetworkInfo.State.CONNECTED)
	                status= true;
	        }
	    }catch(Exception e){
	        //e.printStackTrace();  
	        return false;
	    }
	    return status;
 
	}
	

}
