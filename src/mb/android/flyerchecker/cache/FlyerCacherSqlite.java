package mb.android.flyerchecker.cache;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.graphics.Bitmap;

/**
 * A Cacher implementation using Sqlite
 * @author zhuoy_li
 *
 */

public class FlyerCacherSqlite implements FlyerCacher{
	
	protected FlyerCacherSqlite(){
		
	}
	
	@Override
	public boolean isUrlCached(String url, Calendar date){
		return false;
	}
	
	@Override
	public Bitmap getCachedUrlPage(String url){
		return null;
	}
	
	@Override
	public Calendar getCurrentDate(){
		return new GregorianCalendar();
	}
	
	@Override
	public boolean cacheFlyerPage(String url, Bitmap page){
		return true;
	}
	
	@Override
	public boolean cleanCache(){
		return cleanCache(getCurrentDate());
	}
	
	@Override
	public boolean cleanCache(Calendar date){
		return true;
	}
}
