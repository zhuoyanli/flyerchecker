package mb.android.flyerchecker.dialog;
import mb.android.flyerchecker.R;
import mb.android.flyerchecker.exception.NetErrorQuitException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;


/**
 * An alertdialog that prompts users networking error and 
 * let users choose to re-try or abort
 * @author zhuoy_li
 *
 */
public class NetErrorDialog {
	
	/**
	 * Listener for dialog buttons
	 * @author zhuoy_li
	 *
	 */
	static abstract public class ListenerAlertDlg implements DialogInterface.OnClickListener{
		Handler handler;
		boolean cancelled ;
		public static final int ID_MSG_NETERRORDLG = R.id.id_msg_neterrordlg;
		
		public ListenerAlertDlg(Handler hdlr){
			handler = hdlr;
			cancelled = true;
		}
		public void onClick(DialogInterface dialog, int which){
			setCancelled();
			Message msg = new Message();
			msg.what = ID_MSG_NETERRORDLG;
			msg.arg1 = cancelled==true?0:1;
			handler.sendMessage(msg);
		}
		abstract protected void setCancelled();
	}
	
	
	static public class ListenerAlertDlgCancel extends ListenerAlertDlg{
		boolean cancelled ;
		
		public ListenerAlertDlgCancel(Handler hdlr){super(hdlr);}
		
		@Override
		protected void setCancelled(){ cancelled = true;}
	}

	static public class ListenerAlertDlgConfirm extends ListenerAlertDlg{
		
		public ListenerAlertDlgConfirm(Handler hdlr){super(hdlr);}
		@Override
		protected void setCancelled(){ cancelled = false;}
	}

    public static void showNetErrorDialog(Context context, ListenerAlertDlgCancel listener) 
    		throws NetErrorQuitException{
    }

    /**
     * Build the alertdialog
     * @param context
     * @param handler
     * @return
     */
    public static AlertDialog getNetErrorDlg(Context context, Handler handler){
		ListenerAlertDlgConfirm listenerConf = 
				new ListenerAlertDlgConfirm(handler);
		ListenerAlertDlgCancel listenerCancl = 
				new ListenerAlertDlgCancel(handler);
			
		return new AlertDialog.Builder(context)
    		.setCancelable(false)
        	.setTitle(R.string.title_dlgneterror)
        	.setMessage(R.string.str_msgneterror)
        	.setPositiveButton("Retry", listenerConf)
        	.setNegativeButton("Quit", listenerCancl).create();		

    }
    
    /**
     * The alertdialog needs to be cleared properly in that
     * we have assigned listeners with reference, they might
     * not be released automatically by gc. And the dialog might
     * not be dismissed automatically at all
     * 
     * @param dlgNetError
     */
    public static void clearNetErrorDlg(AlertDialog dlgNetError){
    	if(null!=dlgNetError){
			dlgNetError.dismiss();
			Button btn = null ;
			if(null != (btn=dlgNetError.getButton(AlertDialog.BUTTON_POSITIVE)))
				btn.setOnClickListener(null);
			if(null != (btn=dlgNetError.getButton(AlertDialog.BUTTON_NEGATIVE)))
				btn.setOnClickListener(null);
		}
    }

}
