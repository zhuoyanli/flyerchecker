package mb.android.flyerchecker.net;

import java.io.BufferedInputStream;

import mb.android.flyerchecker.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;


/**
 * Network thread to retrieve bitmap from URL
 * @author zhuoy_li
 *
 */
public class ImgNetworker extends Networker {

	
	
	public final static int ID_MSG_IMGNETWORKER = R.id.id_msg_imgnetworker;

	public ImgNetworker(Handler handler,String url){
		super(ID_MSG_IMGNETWORKER, handler,url);
	}

	public static Bitmap getResultBitmap(Message msg) throws ClassCastException{
		return (Bitmap)((Networker.Result)msg.obj).obj;
	}
	
	@Override
	protected void procNetworkResponse(Message msg) throws Exception{
			BufferedInputStream bis = new BufferedInputStream(urlConn.getInputStream());
			Bitmap bitmap = BitmapFactory.decodeStream(bis);
			bis.close();
			urlConn.disconnect();
			setMsgSuccess(msg, true);
			
			msg.obj = new Result(url, bitmap);
	}
	


}
