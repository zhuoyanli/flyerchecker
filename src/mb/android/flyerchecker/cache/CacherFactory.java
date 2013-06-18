package mb.android.flyerchecker.cache;

/**
 * A factory class to encapsulate cacher implementation
 * @author zhuoy_li
 *
 */
public class CacherFactory {
	private static FlyerCacher cacher = new FlyerCacherSqlite();
	
	protected CacherFactory(){}
	
	static public FlyerCacher getCacher(){
		return cacher;
	}
}
