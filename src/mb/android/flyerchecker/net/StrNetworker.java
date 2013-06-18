package mb.android.flyerchecker.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import mb.android.flyerchecker.R;
import android.os.Handler;
import android.os.Message;

/**
 * Network thread to textual string from URL
 * @author zhuoy_li
 *
 */

public class StrNetworker extends Networker {
	
	public final static int ID_MSG_STRNETWORKER = R.id.id_msg_strnetworker;

	public StrNetworker(Handler handler,String url){
		super(ID_MSG_STRNETWORKER, handler,url);
	}
	
	public static String getStringResult(Object rv) throws ClassCastException {
		
		return (String)((Networker.Result)rv).obj;
	}
	

	@Override
	protected void procNetworkResponse(Message msg) throws Exception {

		BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
		String str = br.readLine();
		
		urlConn.disconnect();

		setMsgSuccess(msg,true);
		
		Result rv = new Result(url,str);
		msg.obj = rv;
		
	}


}
