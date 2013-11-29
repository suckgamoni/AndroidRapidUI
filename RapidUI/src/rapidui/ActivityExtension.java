package rapidui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rapidui.annotation.FullScreen;
import rapidui.annotation.Layout;
import rapidui.annotation.OptionsMenu;
import rapidui.annotation.TitleBar;
import rapidui.annotation.TitleBarType;
import android.app.Activity;
import android.content.res.Resources;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class ActivityExtension extends Extension {
	private SparseArray<Method> menuItemClickHandlers;
	
	public ActivityExtension(Activity activity) {
		super(activity, activity, new ActivityViewFinder(activity));
	}
	
	public void injectActivity() {
		final Resources res = activity.getResources();
		final Window w = activity.getWindow();
		final Class<?> activityClass = activity.getClass();
		
		// NoTitleBar
		
		final TitleBar titleBar = activityClass.getAnnotation(TitleBar.class);
		if (titleBar != null) {
			final TitleBarType type = titleBar.value();
			if (type == TitleBarType.NONE) {
				activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
			} else if (type == TitleBarType.CUSTOM) {
				activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			}
		}
		
		// Layout
		
		final Layout layout = activityClass.getAnnotation(Layout.class);
		if (layout != null) {
			int id = layout.value();
			if (id == 0) {
				final String packageName = activity.getPackageName();
				
				String name = activityClass.getSimpleName();
				if (name.length() > 8 && name.endsWith("Activity")) {
					name = "activity_" + ResourceUtils.toLowerUnderscored(name.substring(0, name.length() - 8));
				} else {
					name = ResourceUtils.toLowerUnderscored(name);
				}

				id = res.getIdentifier(name, "layout", packageName);
			}
			
			activity.setContentView(id);
		}
		
		// Fullscreen
		
		if (activityClass.isAnnotationPresent(FullScreen.class)) {
			w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		// Set layout id when the title bar is set to be customized.
		
		if (titleBar != null && titleBar.value() == TitleBarType.CUSTOM) {
			w.setFeatureInt(Window.FEATURE_CUSTOM_TITLE, titleBar.id());
		}
	}

	public void injectOptionsMenu(MenuInflater inflater, Menu menu) {
		final Resources res = activity.getResources();
		final Class<?> activityClass = activity.getClass();

		final OptionsMenu optionsMenu = activityClass.getAnnotation(OptionsMenu.class);
		if (optionsMenu != null) {
			int id = optionsMenu.value();
			if (id == 0) {
				final String packageName = activity.getPackageName();
				
				String name = activityClass.getSimpleName();
				if (name.length() > 8 && name.endsWith("Activity")) {
					name = ResourceUtils.toLowerUnderscored(name.substring(0, name.length() - 8));
				} else {
					name = ResourceUtils.toLowerUnderscored(name);
				}

				id = res.getIdentifier(name, "menu", packageName);
			}

			inflater.inflate(id, menu);
		}
	}
	
	@Override
	public void registerHostEvent(Object annotation, int type, Object id, Method method) {
		switch (type) {
		case HOST_EVENT_MENU_ITEM_CLICK:
			if (menuItemClickHandlers == null) {
				menuItemClickHandlers = new SparseArray<Method>();
			}
			menuItemClickHandlers.put((Integer) id, method);
			break;
			
		default:
			super.registerHostEvent(annotation, type, id, method);
			break;
		}
	}
	
	private Method getMenuItemClickHandler(int id) {
		return (menuItemClickHandlers == null ? null : menuItemClickHandlers.get(id));
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		if (id != 0) {
			final Method method = getMenuItemClickHandler(id);
			if (method != null) {
				try {
					method.setAccessible(true);
					return (Boolean) method.invoke(memberContainer, item);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		
		return false;
	}
}
