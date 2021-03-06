package core.game.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.sun.istack.internal.logging.Logger;

import core.Configuration;

public class PluginManager {

	public static final Logger logger = Logger.getLogger(PluginManager.class);

	public static PythonInterpreter python = new PythonInterpreter();
	private static int scriptsLoaded = 0;

	public PluginManager() {
		try {
			loadScripts();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static PyObject getVariable(String variable) {
		try {
			return PluginManager.python.get(variable);
		} catch (PyException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Object callFunc(Class<?> c, String funcName, Object... binds) {
		try {
			PyObject obj = PluginManager.python.get(funcName);
			if (obj != null && obj instanceof PyFunction) {
				PyFunction func = (PyFunction) obj;
				PyObject[] objects = new PyObject[binds.length];
				for (int i = 0; i < binds.length; i++) {
					Object bind = binds[i];
					objects[i] = Py.java2py(bind);
				}
				return func.__call__(objects).__tojava__(c);
			} else
				return null;
		} catch (PyException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static boolean callFunc(String funcName, Object... binds) {
		try {
			PyObject obj = PluginManager.python.get(funcName);
			if (obj != null && obj instanceof PyFunction) {
				PyFunction func = (PyFunction) obj;
				PyObject[] objects = new PyObject[binds.length];
				for (int i = 0; i < binds.length; i++) {
					Object bind = binds[i];
					objects[i] = Py.java2py(bind);
				}
				func.__call__(objects);
				return true;
			} else
				return false;
		} catch (PyException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static void loadScripts() throws IOException {
		logger.info("Loading scripts...");
		PluginManager.python.cleanup();
		File scriptDir = new File(Configuration.DATA_DIR + "plugin/scripts/");		
		if (scriptDir.isDirectory() && !scriptDir.getName().startsWith(".")) {
			File[] children = scriptDir.listFiles();
			for (File child : children)
				if (child.isFile()) {
					if (child.getName().endsWith(".py")) {
						logger.info("\tLoading script: " + child.getPath());
						PluginManager.python
								.execfile(new FileInputStream(child));
						PluginManager.scriptsLoaded++;
					}
				} else
					PluginManager.recurse(child.getPath());
		}
		logger.info("Loaded " + PluginManager.scriptsLoaded + " scripts!");
		PluginManager.scriptsLoaded = 0;
	}

	private static void recurse(String dir) throws IOException {
		File scriptDir = new File(dir);
		if (scriptDir.isDirectory() && !scriptDir.getName().startsWith(".")) {
			File[] children = scriptDir.listFiles();
			for (File child : children)
				if (child.isFile()) {
					if (child.getName().endsWith(".py")) {
						logger.info("\tLoading script: \r" + child.getPath());
						PluginManager.python
								.execfile(new FileInputStream(child));
						PluginManager.scriptsLoaded++;
					}
				} else
					PluginManager.recurse(child.getPath());
		}
	}
}
