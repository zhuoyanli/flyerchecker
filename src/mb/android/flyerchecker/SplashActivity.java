package mb.android.flyerchecker;

import mb.android.flyerchecker.dialog.NetErrorDialog;
import mb.android.flyerchecker.dialog.NetErrorDialog.ListenerAlertDlg;
import mb.android.flyerchecker.net.Networker;
import mb.android.flyerchecker.net.StrNetworker;
import mb.android.flyerchecker.utils.Helper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {
	
	enum PROGRESS {STATUS_START, STATUS_CHKCONNECT,STATUS_GETVENDORLIST, STATUS_OVER};
	
	
	private ProgressBar bar;
	private TextView txtStatus;
	
	private String jsonVendorList;
	private AlertDialog dlgNetError;
	private HandlerSplashAct handler;
	
	static private int millsecIntervalStartMainAct = 300;
	
	static private String strTagLog = "SpalshActivity"; 
	
	/**
	 * This class handles the message queue for SplashActivity
	 * @author zhuoy_li
	 *
	 */
    static class HandlerSplashAct extends Handler{
    	
    	public HandlerSplashAct(SplashActivity parent, PROGRESS stat){
    		this.parent = parent;
    		status = stat;
    	}
    	
    	/**
    	 * status Keep track of current progress stage
    	 */
    	private PROGRESS status;
    	
    	/**
    	 * parent Reference to parent activity
    	 */
    	private SplashActivity parent; 

    	/**
    	 * Thread safe method to get current progress stage
    	 * @return
    	 */
    	synchronized PROGRESS getStatus(){
    		return this.status;
    	}

    	/**
    	 * Thread safe method to set current progress stage
    	 * @param stat New stage
    	 */
    	synchronized void setStatus(PROGRESS stat){
    		status = stat;
    	}

    	/**
    	 * Method to handle message
    	 * @param msg
    	 */
    	@Override
        public void handleMessage(Message msg) {
    		switch(msg.what){
    		// if the message is sent from NetErrorDialog
    		case ListenerAlertDlg.ID_MSG_NETERRORDLG:
        		// if the neterrordlg is dismissed by confirm (re-try) or cancel (quit)        		
        		boolean cancelled = msg.arg1==1?false:true;
        		if(cancelled == true) parent.quit();
        		
        		// just send the message and the handler will redo
        		// the previous check-up since the stage indicator has not been change
        		this.sendEmptyMessage(StrNetworker.ID_MSG_STRNETWORKER);
       		break;
       		
       		// if the message is sent from a finished networker thread
       		case StrNetworker.ID_MSG_STRNETWORKER:
        	
       			// check current status
	        	PROGRESS stat = getStatus();
	        	
	        	switch(stat){
	        	// networker to check network connectivity
				case STATUS_CHKCONNECT:
		        	switch(msg.arg1){
		        	// networker succeed
		        	case Networker.NETWORKER_RESULT.SUCCESS:
		        		Networker.Result rv = (Networker.Result) msg.obj;
		        		String rvStr = (String)rv.obj;
		        		
		        		// checker if returned result is as expected
		        		if(Helper.isConnectOK(rv.url, rvStr)){
			        		parent.bar.incrementProgressBy(50);
			        		
			        		//next to get list of available vendors
			            	setStatus( PROGRESS.STATUS_GETVENDORLIST );
			        		parent.getVendorList();
			        		
			        		break;
		        		}else{
		        			// networker failed, we should back-off
		        			setStatus( PROGRESS.STATUS_START );
			        	   	parent.failedNet();
		        			break;
		        		}
		        	case Networker.NETWORKER_RESULT.FAILURE:
		        		
		        		// networker failed, we should back-off
		        		setStatus( PROGRESS.STATUS_START );
		        	   	parent.failedNet();
		        		break;
		        		
		        	default:
		        		// unexpected message!
		        		//TODO
		        	}
		        break;
		        
		        // networker to retrieve list of available vendors
				case STATUS_GETVENDORLIST:
		        	switch(msg.arg1){
		        	
		        	// networker succeed
		        	case Networker.NETWORKER_RESULT.SUCCESS:
		        		
		        		parent.bar.incrementProgressBy(50);
		        		
		        		// get the result (a json string)
		        		parent.jsonVendorList = StrNetworker.getStringResult(msg.obj);
		        		
		        	   	parent.done();
		        		break;
		        	case Networker.NETWORKER_RESULT.FAILURE:
		        	   	
		        		// networker failed, back-off
		        		setStatus( PROGRESS.STATUS_CHKCONNECT );
		        	   	parent.failedNet();
		        		break;
		        	default:
		        		// unexpected message!
		        		//TODO
		        	}
		        break;
		        
		        // firstly startup
				case STATUS_START:
					
					// next to check network connectivity
	        	   	setStatus( PROGRESS.STATUS_CHKCONNECT );
					parent.checkConnect();
		        break;
	
				default:
					// unexpected message!
	        		//TODO
					break;
	        	
	        	}//switch(stat){
	        	
        	break;//switch(msg.what)
    		}
        	
        }
    };
    
    /**
     *  startup progress
     */
    protected void startup(){
    	assertNetwork();
    	handler.sendEmptyMessage(StrNetworker.ID_MSG_STRNETWORKER);
    }
    
    /**
     *  check network connectivity
     */
    protected void checkConnect(){
    	txtStatus.setText(R.string.txt_msgchkserver);
    	new StrNetworker(handler,Helper.prepUrlCheckConnect()).start();
    }
    
    /**
     *  retrieve list of available vendors
     */
    protected void getVendorList(){
    	
     	handler.setStatus(PROGRESS.STATUS_GETVENDORLIST);
     	
     	txtStatus.setText(R.string.txt_msgloadvendors);
     	String url = Helper.getQueryVendorList();
		new StrNetworker(handler,url).start();
    }
    
    /**
     * all stages were cleared, move to main activity
     */
    private void done(){
    	txtStatus.setText(R.string.txt_msgchkpass);
    	
    	// we use a bg runnable to start the main activity
    	handler.postDelayed(new Runnable(){
    		public void run(){
		    	Intent i = new Intent(SplashActivity.this, MainActivity.class);
				i.putExtra("jsonVendorList", jsonVendorList);
				SplashActivity.this.startActivity(i);
				SplashActivity.this.finish();
    		}
    	}, millsecIntervalStartMainAct);
    }
    
    /**
     * Network failed, either connectivity check or net operation
     */
    private void failedNet(){
    	showNetRetryDlg();
    }
    
    /**
     * Quit the activity
     */
    private void quit(){
    	SplashActivity.this.finish();
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// main init part
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_splash);
		
		// init views
		bar = (ProgressBar) findViewById(R.id.bar);
		txtStatus = (TextView) findViewById(R.id.txtChkStatus);
		
		// init others
		handler = new HandlerSplashAct(this, PROGRESS.STATUS_START);
		dlgNetError = NetErrorDialog.getNetErrorDlg(SplashActivity.this, handler);

		// we need to put aside the app version for reference
		try {
			Helper.setVersionApp(this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA).versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
		}
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		startup();
	}
	
	@Override
	protected void onDestroy(){
		// we need to clear the neterrordlg to prevent leak
		// in that we have associated listener with reference
		NetErrorDialog.clearNetErrorDlg(dlgNetError);
		super.onDestroy();
	}
	
	/**
	 * check network connectivity
	 */
	protected void assertNetwork(){
		if(Helper.isNetworkReady(SplashActivity.this) == false){
			showNetRetryDlg();
		}
	}

	/**
	 * show alertdialog to prompt networking failure
	 * and allow users to choose re-try or abort
	 */
	protected void showNetRetryDlg(){
		dlgNetError.show();
	}
}
