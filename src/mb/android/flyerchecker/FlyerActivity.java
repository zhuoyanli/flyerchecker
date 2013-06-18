package mb.android.flyerchecker;

import java.util.ArrayList;

import mb.android.flyerchecker.cache.CacherFactory;
import mb.android.flyerchecker.dialog.NetErrorDialog;
import mb.android.flyerchecker.net.ImgNetworker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FlyerActivity extends Activity {
	
	ImageView imgFlyerPage = null;
    Button btnBak;
    Button btnFwd;

	TextView txtFlyerPageIdx;

	ProgressDialog pdLoading = null;
	AlertDialog dlgNetError = null;
	
	HandlerFlyerAct handler;
	
	ArrayList<String> lstFlyerPageUrls ;
    int curFlyerPageIdx ;
    int pendingFlyerPageIdx;
    
    AtaskNetLoaderFlyerAct taskNetloaderAsync = null;

    static private String strTagLog = "FlyerActivity";
    
    final static int ID_MSG_CMDFLYERACT = R.id.id_msg_cmdflyer;
    final static int MSG_CMD_LOADFLYERPAGE = R.id.id_msg_cmdflyer_loadflyerpage;
    final static int MSG_CMD_RESETIMAGE = R.id.id_msg_cmdflyer_resetimage;

    /**
     * Handler to handle messages
     * @author zhuoy_li
     *
     */
    static class HandlerFlyerAct extends Handler{
    	private FlyerActivity parent;

		public HandlerFlyerAct(FlyerActivity parent){
    		this.parent = parent;
    	}

    	public void handleMessage(Message msg){
    		// from where the message is sent
    		switch( msg.what ){
    		
    		// as a command
    		case ID_MSG_CMDFLYERACT :
    			switch(msg.arg1){
    			
    			// to load flyer page
    			case MSG_CMD_LOADFLYERPAGE:
    				// get the index of page to load
    				int idxFlyerPage = msg.arg2;
    				parent.loadFlyerPage(idxFlyerPage);
    			break;
    			case MSG_CMD_RESETIMAGE:
    				// get the index of page to load
    				parent.resetImage();
    			break;
    			}
    		break;
    			
    		// from a networker thread
    		case ImgNetworker.ID_MSG_IMGNETWORKER :
    		
    			switch(msg.arg1){
    				// networker succeed
		    		case ImgNetworker.NETWORKER_RESULT.SUCCESS:
		    			ImgNetworker.Result rv = (ImgNetworker.Result) msg.obj;
		
		    			// get the result, which should be a bitmap
		    			Bitmap bmp = (Bitmap)rv.obj;
		    			parent.loadFlyerImage(bmp);
		    			
		    			@SuppressWarnings("unused")
						LinearLayout.LayoutParams lpImgFlyerPage = 
		    					(LinearLayout.LayoutParams)parent.imgFlyerPage.getLayoutParams();
		    			
		    			// we only update the textview of current page index
		    			// after the page is successfully loaded, which is NOW
		    			parent.curFlyerPageIdx = parent.pendingFlyerPageIdx;
		    			parent.pendingFlyerPageIdx = -1;
		    			parent.setTxtWithFlyerPos();
		    			parent.postTaskAsync();
		    			
		    			// cache the page
		    			CacherFactory.getCacher().cacheFlyerPage(rv.url, bmp);
		    			
		    			break;
		    			
		    		// networker thread failed
		    		case ImgNetworker.NETWORKER_RESULT.FAILURE:
		    			
		    			// show the neterrordialog
		    			parent.showNetErrorDlg();
		    			break;
		    			
		    		default:
		    			// unexpected message
		    			// TODO
		    			break;
    			}//switch(msg.arg1)
    		break;
    		
    		default:
    			// unexpected message
    			//TODO
    		break;
    		}//switch( msg.what ){
    	}
    }
    
    /**
     * AsyncTask to load flyer page
     * @author zhuoy_li
     *
     */
	class AtaskNetLoaderFlyerAct extends AsyncTask<Void, Void, Void> {

		String url ;
		public AtaskNetLoaderFlyerAct(String url){
			this.url = url;
			pd = null;
		}
		
		public ProgressDialog pd;

		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(FlyerActivity.this);
			pd.setTitle("Loading Flyer Image...");
			pd.setMessage("Please wait.");
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.show();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			
			if(CacherFactory.getCacher().isUrlCached(url, CacherFactory.getCacher().getCurrentDate())){
				Bitmap bmp = CacherFactory.getCacher().getCachedUrlPage(url);
				imgFlyerPage.setImageBitmap(bmp);
			}else{
				new ImgNetworker(handler,url).start();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();
			pd = null;
		}

	};


	/**
	 * Handle the user finger touch
	 * @author zhuoy_li
	 *
	 */
	enum MOD_TOUCH {NONE, DBCLK, DRAG, ZOOM};
	class TouchListenerFlyerPage  
		implements OnTouchListener{
		
	    private static final String TAG_MTLTNR = "Touch";
	    @SuppressWarnings("unused")
	    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;

	    // These matrices will be used to scale points of the image
	    Matrix matrix = new Matrix();
	    Matrix savedMatrix = new Matrix();

	    // The 3 states (events) which the user is trying to perform
	    
	    MOD_TOUCH mode = MOD_TOUCH.NONE;

	    // these PointF objects recording the point(s) the user is touching
	    PointF pfStart = new PointF();
	    PointF pfMid = new PointF();
	    float fOldDist = 1f;

	    float fScaleZoomIn = 1.2f;
	    float fScaleZoomOut = 0.7f;
	    
	    Long timeLastTap ;
	    
	    public TouchListenerFlyerPage(){
	    	timeLastTap = 0l;
	    }
	    
	    public View.OnClickListener listenerZoomIn = new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				matrix.set(savedMatrix);
				matrix.postScale(fScaleZoomIn, fScaleZoomIn, pfMid.x, pfMid.y);
				imgFlyerPage.setImageMatrix(matrix);
			}
	    	
	    };

	    public View.OnClickListener listenerZoomOut = new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				matrix.set(savedMatrix);
				matrix.postScale(fScaleZoomOut, fScaleZoomOut, pfMid.x, pfMid.y);
				imgFlyerPage.setImageMatrix(matrix);
			}
	    	
	    };
	    
	    /** Called when the activity is first created. */
	    @Override
	    public boolean onTouch(View v, MotionEvent event) 
	    {
//	    	if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP)
//	    		return true;
	    	
	        ImageView img = (ImageView) v;
	        img.setScaleType(ImageView.ScaleType.MATRIX);
	        float scale;

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: // first finger down only
//				
				long timeClk = System.currentTimeMillis();
				if(timeLastTap>0){
					
					long intvlDbclk = timeClk - timeLastTap ;
					timeLastTap = timeClk;
					
					Log.d(TAG_MTLTNR, "Interval between last tap "+intvlDbclk);
					if(intvlDbclk < R.integer.millsec_dbclk_flyerimagereset_maxinterval){
						// considered double-click
						mode = MOD_TOUCH.DBCLK;
						
						Log.d(TAG_MTLTNR, "event=considered dbclick"); // write to LogCat
						Log.d(TAG_MTLTNR, "mode=DBCLK"); // write to LogCat
						break;
					}
				}else{
					timeLastTap = timeClk;
				}
				
				
				savedMatrix.set(matrix);
				pfStart.set(event.getX(), event.getY());
				Log.d(TAG_MTLTNR, "event=first finger down"); // write to LogCat
				Log.d(TAG_MTLTNR, "mode=DRAG"); // write to LogCat
				mode = MOD_TOUCH.DRAG;
				
				break;
				
			case MotionEvent.ACTION_UP: // first finger lifted
				// we just do nothing
				Log.d(TAG_MTLTNR, "event=first finger up");
				if(mode == MOD_TOUCH.DBCLK){
					
//					Message msg = new Message();
//					msg.what = ID_MSG_CMDFLYERACT;
//					msg.arg1 = MSG_CMD_RESETIMAGE;
//					handler.sendMessage(msg);
					
					resetImage();
					mode = MOD_TOUCH.NONE;
					return true;
				}

				break;
//			case MotionEvent.ACTION_CANCEL: // gesture cancelled
//				mode = MOD_TOUCH.NONE;
//				Log.d(TAG_MTLTNR, "event=gesture cancelled");
//				Log.d(TAG_MTLTNR, "mode=NONE");
// 			break;

			case MotionEvent.ACTION_POINTER_UP: // second finger lifted
				mode = MOD_TOUCH.NONE;
				Log.d(TAG_MTLTNR, "event=second finger up");
				Log.d(TAG_MTLTNR, "mode=NONE");
				break;


			case MotionEvent.ACTION_POINTER_DOWN: // first and second finger
													// down

				Log.d(TAG_MTLTNR, "event=second finger down");
				
				fOldDist = spacing(event);
				Log.d(TAG_MTLTNR, "oldDist=" + fOldDist);
				if (fOldDist > 5f) {
					savedMatrix.set(matrix);
					midPoint(pfMid, event);
					mode = MOD_TOUCH.ZOOM;
					Log.d(TAG_MTLTNR, "mode=ZOOM");
				}
				break;

			case MotionEvent.ACTION_MOVE: // finger moving
				
				Log.d(TAG_MTLTNR, "event=finger moving");
				
				switch(mode){
				case NONE:
					break;
					
				case DRAG: // dragging with one finger
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - pfStart.x, event.getY() - pfStart.y); 
					// create the transformation in the
					// matrix of points
					break;
					
				case ZOOM: // zooming with two fingers
					// pinch zooming
					float newDist = spacing(event);
					Log.d(TAG_MTLTNR, "newDist=" + newDist);
					if (newDist > 5f) {
						matrix.set(savedMatrix);
						scale = newDist / fOldDist; 
						// setting the scaling of the matrix...
						//if scale > 1 zoom in...
						//if scale < 1 means zoom out
						matrix.postScale(scale, scale, pfMid.x, pfMid.y);
						//savedMatrix.set(matrix);
					}
				break;
				case DBCLK:
					break;
				default:
					break;
				}//switch(mode){
					
			break;
			}//switch (event.getAction() & MotionEvent.ACTION_MASK) {

			img.setImageMatrix(matrix); // display the transformation on screen

			return true; // indicate event was handled
	    }

	    
	    // check the space btwn two fingers
	    private float spacing(MotionEvent event) 
	    {
	        float x = event.getX(0) - event.getX(1);
	        float y = event.getY(0) - event.getY(1);
	        return (float)Math.sqrt(x * x + y * y);
	    }

	    // find the middle point
	    private void midPoint(PointF point, MotionEvent event) 
	    {
	        float x = event.getX(0) + event.getX(1);
	        float y = event.getY(0) + event.getY(1);
	        point.set(x / 2, y / 2);
	    }


	}
	
	/**
	 * Show the progressdialog
	 */
	protected void showPDLoading(){
		pdLoading = new ProgressDialog(FlyerActivity.this);
		pdLoading.setTitle("Please wait");
		pdLoading.setMessage("Loading Flyer Page "+String.valueOf(pendingFlyerPageIdx+1));
		pdLoading.setCancelable(false);
		pdLoading.setIndeterminate(true);
		pdLoading.show();
	}
	
	/**
	 * Dismiss the progressdialog
	 */
	protected void closePDLoading(){
		// this design has a bug
		// if the screen is rotated, the view will be invalidated
		// and dismissing the progressdialog will cause a crash
		pdLoading.dismiss();
		pdLoading = null;
	}
	
	protected void prepTaskAsync(){
		this.showPDLoading();
	}
	
	protected void postTaskAsync(){
		this.closePDLoading();
	}

	protected void loadImgUrlAsync(String url){
		// the reason we didn't use AsyncTask here
		// is that we only get to know the end of
		// bg task from the handler
		prepTaskAsync();
		new ImgNetworker(handler,url).start();
	}

	/**
	 * Update the textview about the index of current flyer page
	 */
    protected void setTxtWithFlyerPos(){
    	int size = lstFlyerPageUrls.size();
    	txtFlyerPageIdx.setText( String.format("Page %d of %d", curFlyerPageIdx+1, size));
    }
    
    

	protected void onCreate(Bundle savedInstanceState) {
		// main init part
		super.onCreate(savedInstanceState);
		
		// we remove the title
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_flyer);
		
		taskNetloaderAsync = null;
		pdLoading = null;

		txtFlyerPageIdx = (TextView) findViewById(mb.android.flyerchecker.R.id.textPagePos);
		imgFlyerPage = (ImageView)findViewById(mb.android.flyerchecker.R.id.imgFlyer);
		btnBak = (Button)findViewById(R.id.btnBak);
		btnFwd = (Button)findViewById(R.id.btnFwd);

		// button to navigate backward through pages
		btnBak.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// if we are already in the end
				if(moveFlyerPageIdxBack()==false) return;
				
				// attention: we have to use a pending index
				// in that the operation of loading next designated page
				// might fail! We only update the current page index
				// after the loading succeeds
				String url = lstFlyerPageUrls.get(pendingFlyerPageIdx);
				
				Log.d("FlyerAct", "Loading Idx "+pendingFlyerPageIdx+" "+url);
				
				loadImgUrlAsync(url);
			}
			
		});
		
		//button to navigate forward through pages
		btnFwd.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// can we move forward any further?
				if(moveFlyerPageIdxFwd()==false) return;
				
				// attention: we have to use a pending index
				// in that the operation of loading next designated page
				// might fail! We only update the current page index
				// after the loading succeeds
				String url = lstFlyerPageUrls.get(pendingFlyerPageIdx);

				Log.d("FlyerAct", "Loading Idx "+pendingFlyerPageIdx+" "+url);

				loadImgUrlAsync(url);
			}
			
		});

		handler = new HandlerFlyerAct(FlyerActivity.this);
		dlgNetError = NetErrorDialog.getNetErrorDlg(this, handler);

		TouchListenerFlyerPage touchFlyerImg = new TouchListenerFlyerPage();
		imgFlyerPage.setOnTouchListener(touchFlyerImg);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int size = extras.getInt("size");
		
		lstFlyerPageUrls = new ArrayList<String>(size);
		for(int i=0;i<size;i++){
			lstFlyerPageUrls.add(intent.getStringExtra(String.valueOf(i+1)));
		}
		
		curFlyerPageIdx = 0;
		setTxtWithFlyerPos();
		
	}

	@Override
	protected void onStart(){
		super.onStart();
		
		curFlyerPageIdx = 0;
		
		Message msg = new Message();
		msg.what = ID_MSG_CMDFLYERACT;
		msg.arg1 = MSG_CMD_LOADFLYERPAGE;
		msg.arg2 = curFlyerPageIdx;
		handler.sendMessage(msg);
	}
	
	private void loadFlyerPage(int idxFlyerPage){
		String url = lstFlyerPageUrls.get(idxFlyerPage);
		loadImgUrlAsync(url);		
	}
	
	private boolean moveFlyerPageIdxBack(){
		if(curFlyerPageIdx<=0) return false;
		
		pendingFlyerPageIdx = curFlyerPageIdx - 1;
		
		return true;
	}
	
	private boolean moveFlyerPageIdxFwd(){
		if(curFlyerPageIdx>=lstFlyerPageUrls.size()-1) return false;
		
		pendingFlyerPageIdx = curFlyerPageIdx < lstFlyerPageUrls.size() - 1 ? curFlyerPageIdx+1 : curFlyerPageIdx;
		
		return true;
	}
	
	private void loadFlyerImage(Bitmap bitmap){
		imgFlyerPage.setScaleType(ScaleType.MATRIX);
		imgFlyerPage.setImageBitmap(bitmap);
		
		//imgFlyerPage.setImageBitmap(bitmap);
		setImageCentered();
	}
	
	private void resetImage(){
		setImageCentered();
	}

	private void setImageCentered(){
		Matrix m = imgFlyerPage.getImageMatrix();
		Bitmap bitmap = ((BitmapDrawable)imgFlyerPage.getDrawable()).getBitmap();
		RectF drawableRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
		RectF viewRect = new RectF(0, 0, imgFlyerPage.getWidth(), imgFlyerPage.getHeight());
		m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
		imgFlyerPage.setImageMatrix(m);		
		imgFlyerPage.invalidate();
	}
	
	private void showNetErrorDlg(){
		dlgNetError.setMessage(getResources().getString(R.string.str_msgneterror_flyer));
		dlgNetError.show();
	}
	
	@Override
	protected void onDestroy(){
		if(dlgNetError!=null)
			NetErrorDialog.clearNetErrorDlg(dlgNetError);
		super.onDestroy();
	}
}
