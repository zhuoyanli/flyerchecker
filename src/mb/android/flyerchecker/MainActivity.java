package mb.android.flyerchecker;

import java.util.ArrayList;
import java.util.HashMap;

import mb.android.flyerchecker.dialog.NetErrorDialog;
import mb.android.flyerchecker.dialog.NetErrorDialog.ListenerAlertDlg;
import mb.android.flyerchecker.exception.IndexException;
import mb.android.flyerchecker.net.Networker;
import mb.android.flyerchecker.net.StrNetworker;
import mb.android.flyerchecker.utils.Helper;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;


public class MainActivity extends Activity {
	
	ArrayList<ArrayList<String>> lstOfLstVendorFlyerUrls ;
	ArrayList<String> lstVendorNames ;
    Intent intentFlyer;
    boolean titleChanged = false;
    
    HandlerMainActivity handler;
    AlertDialog dlgNetError ;
    
    int currentCmd ;

    final static int ID_MSG_CMDMAINACT = R.id.id_msg_cmdmain;
    final static int MSG_CMD_LOADVENDORJSON = R.id.id_msg_cmdmain_loadvendorjson;
    final static int MSG_CMD_LOADFLYERACT = R.id.id_msg_cmdmain_startflyeract;

    final private static int IDX_CONTEXTMENUITEM_BTNVIEW = 0;
    final private static int IDX_CONTEXTMENUITEM_BTNDISMISS = 1;

    static private String strTagLog = "MainActivity";
    
    /**
     * AsyncTask to load FlyerActivity in background.
     * Here we use a progressdialog to prompt users to wait
     * @author zhuoy_li
     *
     */
	class TaskLoadVendorUrlsAsync extends AsyncTask<Void, Void, Void> {

		Intent intent2Run;
		public TaskLoadVendorUrlsAsync(Intent intent){
			this.intent2Run = intent;
		}
		
		private ProgressDialog pd;

		@Override
		protected void onPreExecute() {

			pd = new ProgressDialog(MainActivity.this);
			pd.setTitle("Loading Flyer...");
			pd.setMessage("Please wait.");
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.show();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			startActivity(this.intent2Run);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();
		}

	};

	/**
	 * Message handler
	 * @author zhuoy_li
	 *
	 */
	static class HandlerMainActivity extends Handler{
		MainActivity parent;
		
		public HandlerMainActivity(MainActivity parent) {
			super();
			this.parent = parent;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			// from where the message is sent
        	switch(msg.what){
        	
        	// from neterrordialog
        	case ListenerAlertDlg.ID_MSG_NETERRORDLG:
        		// if the neterrordlg is dismissed by confirm (re-try) or cancel (quit)
        		boolean cancelled = msg.arg1==1?false:true;
        		
        		if(cancelled == false){
        			// user wants to re-try
        			Message msgCmd = new Message();
        			msgCmd.what = MainActivity.ID_MSG_CMDMAINACT;
        			msgCmd.arg1 = parent.currentCmd;
        			sendMessage(msg);
        			
        		}else{
        			// we just abort last operation
        		}
        	break;
			
        	// message to issue a operation command
			case ID_MSG_CMDMAINACT:
				
				parent.currentCmd = msg.arg1;
				
				switch(msg.arg1){
				// command to load the flyer activity
				case MSG_CMD_LOADFLYERACT:
					String strJson = (String)msg.obj;
					parent.loadFlyerActivity(strJson);
					break;
					
				// command to load json of vendor's flyer info
				case MSG_CMD_LOADVENDORJSON:
					int idxVendor = msg.arg2;
					parent.loadVendorJson(idxVendor);
					break;
				}
				
			break;
				
			// message from a networker thread
			case StrNetworker.ID_MSG_STRNETWORKER:
				switch (msg.arg1) {
				// networker succeed
				case Networker.NETWORKER_RESULT.SUCCESS:
					
					// get the json result
					String strJson = StrNetworker.getStringResult(msg.obj);
					
					// send the start flyeractivity command
					Message msgLoadAct = new Message();
					msgLoadAct.what = ID_MSG_CMDMAINACT;
					msgLoadAct.arg1 = MSG_CMD_LOADFLYERACT;
					msgLoadAct.obj = strJson;
					
					sendMessage(msgLoadAct);
					
					break;
					
				// networker failed
				case Networker.NETWORKER_RESULT.FAILURE:
					// show the neterrordialog
					parent.showNetErrorDlg();
					break;
				}
			break;
			}
		}
		
	}
    
	/**
	 * Load the vendon's flyer json info
	 * @param idxVendor Index of the vendor
	 */
	public void loadVendorJson(int idxVendor){
		try {
			assertVendorIdx(idxVendor);
		} catch (IndexException e) {
			//TODO
			return;
		}
		
		String url = Helper.getQueryFlyerUrls(idxVendor);
		loadVendorJson(url);
	}
	
	/**
	 * Load the vendon's flyer json info
	 * this method is necessary
	 * in that we might need to reload VendorJson again based
	 * on returned message, at the time we won't have vendor index
	 * 
	 * @param url Url of the vendon's flyer json info
	 */
	public void loadVendorJson(String url){
		new StrNetworker(handler, url).start();
	}
	
	/**
	 * Assert the index of vendor
	 * @param idxVendor
	 * @throws IndexException
	 */
	public void assertVendorIdx(int idxVendor) throws IndexException{
		if(idxVendor<0 || idxVendor >= lstVendorNames.size())
			throw new IndexException();
	}
	
	/**
	 * Load the FlyerActivity based on the json info.
	 * The strJson is expected to be a string representation of a json object,
	 * with a property of name being "urls" and value being a json array that holds a set of json
	 * objects, which have a property of name being "url" and value being the url to respective
	 * flyer page.
	 * 
	 * @param strJson
	 */
    public void loadFlyerActivity(String strJson){
    	try{
	    	JSONObject jobjUrls = new JSONObject(strJson);
	    	JSONArray jarrayUrls = jobjUrls.getJSONArray("urls");
	    	
	        Intent intentFlyer = new Intent(MainActivity.this,FlyerActivity.class);
	        
	        // prepare the parameters to be passed
	        Bundle extras = new Bundle();
	        for(int i=0;i<jarrayUrls.length();i++){
	        	JSONObject jobjUrl = jarrayUrls.getJSONObject(i);
	        	extras.putString(String.valueOf(i+1), jobjUrl.getString("url"));
	        }
	        extras.putInt("size",jarrayUrls.length());
	        intentFlyer.putExtras(extras);
	        
	        intentFlyer.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        
	        // load the FlyerActivity with AsyncTask
	        TaskLoadVendorUrlsAsync task = new TaskLoadVendorUrlsAsync(intentFlyer);
			task.execute((Void[]) null);
			
			//startActivity(intentFlyer);
	        //PendingIntent.getActivity(MainActivity.this, 0, intentFlyer, 0);
    	}catch(Exception ex){
    		
    	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// main init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getResources().getString(R.string.title_main));
        
        // init others
        handler = new HandlerMainActivity(MainActivity.this);
        dlgNetError = NetErrorDialog.getNetErrorDlg(this, handler);
        
        String jsonStr = getIntent().getStringExtra("jsonVendorList");
        	
        //bind ListView  
        ListView lvVendors = (ListView) findViewById(R.id.listViewVendors);  
        //dynamic array for list items
        ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();  

        JSONArray jarray = null;
        
        try{

	        jarray = new JSONArray(jsonStr);
	        
	        lstOfLstVendorFlyerUrls = new ArrayList<ArrayList<String>>(jarray.length());
	        lstVendorNames = new ArrayList<String>(jarray.length());
	        
	        for(int i=0;i<jarray.length();i++)  
	        {  
	        	JSONObject jobj = (JSONObject) jarray.get(i);
	            HashMap<String, Object> map = new HashMap<String, Object>();  
	            map.put("itemImage", getImgByIdx(jobj.getInt("imgIdx"))); //id for img res
	            map.put("itemTextChecked", jobj.getString("name"));  
	            //map.put("ItemText", jobj.getString("title"));  
	            
	            lstVendorNames.add(jobj.getString("name"));
	            
	            listItem.add(map);  
	        }  

        
        }catch(Exception ex){
        }
        
          
        // generate the adapter
        SimpleAdapter listItemAdapter = new SimpleAdapter(this,
        	listItem,//data source  
            R.layout.venderlistlayout,//xml for ListItem
            //array and items          
            new String[] {"itemImage","itemTextChecked"},   
            new int[] {R.id.itemImage,R.id.itemTextChecked}  
        );  
         
        lvVendors.setAdapter(listItemAdapter);  
          
       
        // we ask user to long-press a list item to select a vendor to view
        // therefore we add a contextmenu to listview
        // LongPress event  
        lvVendors.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {  
              
            @Override  
            public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
            	// get the name of the vendor from radiobutton text of listview's listitem
            	RadioButton rb = (RadioButton) v.findViewById(R.id.itemTextChecked);
            	String name = (String) rb.getText();
            	menu.setHeaderTitle(getResources().getString(R.string.title_pupselectvendor)+" "+name);
            	
            	
            	menu.add(0, IDX_CONTEXTMENUITEM_BTNVIEW, 0, "View The Flyer");
            	menu.add(0, IDX_CONTEXTMENUITEM_BTNDISMISS, 0, "Cancel");
            }
        });
        
        lvVendors.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				arg1.showContextMenu();
			}	
		});

    }  
    
    
    //LongPress ContextMenu item to confirm/cancel selection  
    @Override  
    public boolean onContextItemSelected(MenuItem item) {
        //use item.getItemId() to access the index of menuitem in context menu
    	
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	int idxVendor = info.position;
    	
    	if(item.getItemId() == IDX_CONTEXTMENUITEM_BTNVIEW){
    		// if user has confirmed the selection
    		
        	titleChanged = true;
        	setTitle("You have chosen "+lstVendorNames.get(idxVendor));
            
        	// send the command to load the FlyerAcvitity
    		Message msg = new Message();
    		msg.what = ID_MSG_CMDMAINACT;
    		msg.arg1 = MSG_CMD_LOADVENDORJSON;
    		msg.arg2 = idxVendor;
    		handler.sendMessage(msg);
    	}
    	
        return super.onContextItemSelected(item);  
    }  
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    /**
     * Get the vendor's thumbnail
     * @param idx
     * @return
     */
    protected int getImgByIdx(int idx){
    	switch(idx){
    	case 1: return R.drawable.iga;
    	case 2: return R.drawable.loblaws;
    	case 3: return R.drawable.maxi;
    	case 4: return R.drawable.metro;
    	case 5: return R.drawable.provigo;
    	case 6: return R.drawable.superc;
    	default: return R.drawable.iga;
    	}
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	// reset the title if it's been changed from last operation
    	if(titleChanged)
    		setTitle(R.string.title_main);
    	
    }
    
    @Override
    protected void onDestroy(){
    	// clear the neterrordlg to prevent leak
    	NetErrorDialog.clearNetErrorDlg(dlgNetError);
		super.onDestroy();    	
    }
        
    /**
     * Show the neterrordlg
     */
    protected void showNetErrorDlg(){
    	dlgNetError.setMessage(getResources().getString(R.string.str_msgneterror_main));
    	dlgNetError.show();
    }
}
