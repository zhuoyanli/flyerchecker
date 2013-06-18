package mb.android.flyerchecker.cache;

import java.util.Calendar;

import android.graphics.Bitmap;

/**
 * Cacher interface definition
 * @author zhuoy_li
 *
 */
public interface FlyerCacher {
	
	public boolean isUrlCached(String url, Calendar date);
	
	public Bitmap getCachedUrlPage(String url);
	
	public Calendar getCurrentDate();
	
	public boolean cacheFlyerPage(String url, Bitmap page);
	
	public boolean cleanCache();
	
	public boolean cleanCache(Calendar date);
}
