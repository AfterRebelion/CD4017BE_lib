package cd4017be.api.recipes;

import java.util.HashMap;
import javax.script.ScriptException;

import cd4017be.lib.script.Context;
import cd4017be.lib.script.Module;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.obj.IOperand;


/**
 * @author CD4017BE
 *
 */
public class RecipeModule implements Module {

	public final HashMap<String, Handler> methods;
	private final String id;

	public RecipeModule(String id) {
		this.methods = new HashMap<String, Handler>();
		this.id = id;
	}

	@Override
	public IOperand invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException {
		Handler h = methods.get(name);
		if (h == null) throw new NoSuchMethodException(name);
		try {
			return h.handle(args);
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public void assign(String name, IOperand val) {
	}

	@Override
	public IOperand read(String name) {
		return null;
	}

	@Override
	public String addToContext(Context cont) {
		return id;
	}

	@FunctionalInterface
	public interface Handler {
		IOperand handle(Parameters param) throws Exception;
	}

}
