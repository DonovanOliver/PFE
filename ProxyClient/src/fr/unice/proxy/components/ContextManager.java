package fr.unice.proxy.components;

import android.content.Context;

/**
 * This singleton class holds the application contexts. This is useful when we need
 * to access android specific settings (and context is needed) in Non-Activity classes. 
 * @author andrei
 *
 */
public class ContextManager {
	/**
	 * The unique instance of the Singleton
	 */
	private static ContextManager uniqueInstance;
	
	private Context baseContext;
	
	/**
	 * Private Default Constructor. Do not instantiate using constructor. Use <b>getInstance</b> instead.
	 */
	private ContextManager() {}
	
	/**
	 * Static method to get the instance of the ContextManager Singleton. If the instance is null, it will automatically be instantiated
	 * @return
	 */
	public static ContextManager getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new ContextManager();
		}
		return uniqueInstance;
	}

	/**
	 * Gets the base context
	 * @return baseContext
	 */
	public Context getBaseContext() {
		return baseContext;
	}

	/**
	 * Sets the base context using the parameter
	 * @param baseContext
	 */
	public void setBaseContext(Context baseContext) {
		this.baseContext = baseContext;
	}

}
