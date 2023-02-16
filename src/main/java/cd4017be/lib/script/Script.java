package cd4017be.lib.script;

import java.util.HashMap;

import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;

/**
 * 
 * @author CD4017BE
 */
public class Script implements Module {

	public final HashMap<String, Function> methods;
	public final HashMap<String, IOperand> variables;
	public final String fileName;
	public Context context;
	public long editDate;
	public int version;

	public Script(String name, HashMap<String, Function> methods, HashMap<String, IOperand> vars) {
		this.fileName = name;
		this.methods = methods;
		this.variables = vars;
		for (Function f : methods.values()) f.script = this;
	}

	@Override
	public IOperand invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException {
		Function f = methods.get(name);
		if (f != null) return f.apply(args);
		java.util.function.Function<Parameters, IOperand> f1 = context.defFunc.get(name);
		if (f1 != null) return f1.apply(args);
		throw new NoSuchMethodException();
	}

	@Override
	public void assign(String name, IOperand val) {
		if (val == Nil.NIL)
			variables.remove(name);
		else variables.put(name, val);
	}

	@Override
	public IOperand read(String name) {
		IOperand var = variables.get(name);
		return var == null ? Nil.NIL : var;
	}

	@Override
	public String addToContext(Context cont) {
		this.context = cont;
		return fileName;
	}

}
