package cd4017be.lib.render.model;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import cd4017be.lib.script.Context;
import cd4017be.lib.script.Function;
import cd4017be.lib.script.Module;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import cd4017be.lib.Lib;
import cd4017be.lib.script.Compiler;
import cd4017be.lib.util.Stack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author CD4017BE
 */
public class ModelContext extends Context {

	private static class State {
		Matrix4d matrix;
		double oU, oV, sU, sV;
		double[] sColor;
		int texOfs, orient;
		State copy(){
			State state = new State();
			state.matrix = new Matrix4d(matrix);
			state.oU = oU;
			state.oV = oV;
			state.sU = sU;
			state.sV = sV;
			state.sColor = sColor.clone();
			state.texOfs = texOfs;
			state.orient = orient;
			return state;
		}

		void mulMat(Matrix4d mat) {
			matrix.mul(mat);
		}

		Quad transform(Quad quadi) {
			Quad nQuad = new Quad();
			nQuad.tex = quadi.tex + texOfs;
			int i = 0;
			for (double[] vertex : quadi.vertices) {
				double[] vert2 = nQuad.vertices[i++];
				Point3d vec = new Point3d(vertex);
				matrix.transform(vec);
				vert2[0] = vec.x;
				vert2[1] = vec.y;
				vert2[2] = vec.z;
				vert2[3] = vertex[3] * sU + oU;
				vert2[4] = vertex[4] * sV + oV;
				for (int k = 0, j = 5; k < 4; k++, j++)
					vert2[j] = vertex[j] * sColor[k];
			}
			nQuad.cullFace = orient >> (quadi.cullFace << 2) & 15;
			return nQuad;
		}
	}

	private static class AdvancedState extends State {
		Matrix4d secMat;
		Point3d origin, dir;

		AdvancedState(State state) {
			matrix = new Matrix4d(state.matrix);
			oU = state.oU;
			oV = state.oV;
			sU = state.sU;
			sV = state.sV;
			sColor = state.sColor.clone();
			texOfs = state.texOfs;
		}

		@Override
		State copy() {
			AdvancedState state = new AdvancedState(this);
			state.secMat = new Matrix4d(secMat);
			state.origin = new Point3d(origin);
			state.dir = new Point3d(dir);
			return super.copy();
		}

		@Override
		void mulMat(Matrix4d mat) {
			matrix.mul(mat);
			secMat.mul(mat);
		}

		@Override
		Quad transform(Quad quadi) {
			Quad nQuad = new Quad();
			nQuad.tex = quadi.tex + texOfs;
			int i = 0;
			for (double[] vertex : quadi.vertices) {
				double[] vert2 = nQuad.vertices[i++];
				Point3d vec = new Point3d(vertex);
				Point3d vec1 = new Point3d(vec);
				matrix.transform(vec);
				secMat.transform(vec1);
				double d = (vec.x - origin.x) * dir.x + (vec.y - origin.y) * dir.y + (vec.z - origin.z) * dir.z;
				vert2[0] = vec.x + vec1.x * d;
				vert2[1] = vec.y + vec1.y * d;
				vert2[2] = vec.z + vec1.z * d;
				vert2[3] = vertex[3] * sU + oU;
				vert2[4] = vertex[4] * sV + oV;
				for (int k = 0, j = 5; k < 4; k++, j++)
					vert2[j] = vertex[j] * sColor[k];
			}
			return nQuad;
		}
	}

	static class Quad implements IOperand {
		//x, y, z, u, v, r, g, b, a
		double[][] vertices = new double[4][9];
		int tex, cullFace;

		@Override
		public boolean asBool() throws Error {return false;}
		@Override
		public Object value() {return this;}
	}

	private static final double[] defaultVertex = new double[]{0, 0, 0, 0, 0, 1, 1, 1, 1};
	private static final String defaultFormat = "xyzuvrgba";
	private static final int maxStates = 8;
	private static final String cfNames = " BTNSWE", axes = "YyZzXxBTNSWE";
	private static final boolean[][] rect = {{false, false, true, true}, {false, true, true, false}};

	Stack<State> states = new Stack<State>(maxStates);
	List<Quad>[] quads;
	ResourceLocation loadPath;

	@SuppressWarnings("unchecked")
	public ModelContext(ResourceLocation loadPath) {
		super(Lib.LOG);
		this.loadPath = loadPath;
		defFunc.put("add", (p) -> {
			State state = states.get();
			for (Object o : p.getArrayOrAll()) {
				Quad quadi = state.transform((Quad)o);
				quads[quadi.cullFace].add(quadi);
			}
			return null;
		});
		defFunc.put("push", (p) -> {
			if (states.isFull()) throw new IllegalStateException("can't push anymore: max state depth reached!");
			states.add(states.get().copy());
			return null;
		});
		defFunc.put("pop", (p) -> {
			if (states.getPos() <= 0) throw new IllegalStateException("can't pop anymore: already at origin state!");
			states.rem();
			return null;
		});
		defFunc.put("orient", (p) -> {
			String or = p.getString(0);
			double[] center = p.getVectorOrAll(1);
			State state = states.get();
			Matrix4d mat = new Matrix4d();
			mat.setColumn(3, center[0], center[1], center[2], 1);
			int o = 0;
			for (int i = 0; i < 3; i++) {
				int a = axes.indexOf(or.charAt(i)) % 6, j = ((a >> 1) + 1) % 3;
				double d = -1 + (a << 1 & 2);
				mat.setElement(j, 3, mat.getElement(j, 3) - center[i] * d);
				mat.setElement(j, i, d);
				a <<= 2;
				a = state.orient >> (a ^ 4) & 0xf0
				  | state.orient >> a << 4 & 0xf00;
				o |= a << ((i+2) % 3 << 3);
			}
			mat.setElement(3, 3, 1);
			state.mulMat(mat);
			state.orient = o;
			return null;
		});
		defFunc.put("rotate", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			vec[3] = Math.toRadians(vec[3]);
			Matrix4d mat = new Matrix4d();
			mat.set(new AxisAngle4d(vec));
			state.mulMat(mat);
			return null;
		});
		defFunc.put("translate", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			Matrix4d mat = new Matrix4d();
			mat.set(new Vector3d(vec));
			state.mulMat(mat);
			return null;
		});
		defFunc.put("scale", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			Matrix4d mat = new Matrix4d();
			mat.setM00(vec[0]);
			mat.setM11(vec[1]);
			mat.setM22(vec[2]);
			mat.setM33(1);
			state.mulMat(mat);
			return null;
		});
		defFunc.put("offsetUV", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			state.oU += vec[0] * state.sU;
			state.oV += vec[1] * state.sV;
			return null;
		});
		defFunc.put("scaleUV", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			state.sU *= vec[0];
			state.sV *= vec[1];
			return null;
		});
		defFunc.put("color", (p) -> {
			double[] vec = p.getVectorOrAll();
			State state = states.get();
			for (int j = 0; j < vec.length; j++) state.sColor[j] = vec[j];
			return null;
		});
		defFunc.put("texIdx", (p) -> {
			int val = p.getIndex(0);
			State state = states.get();
			state.texOfs += val;
			return null;
		});
		defFunc.put("quad", (p) -> {
			Quad quad = new Quad();
			String format = p.getString(4);
			quad.tex = p.param.length > 5 ? (int)p.getNumber(5) : 0;
			if (format == null) format = defaultFormat;
			quad.cullFace = cfNames.indexOf(format.charAt(format.length() - 1));
			for (int i = 0; i < 4; i++) {
				double[] vec = p.getVector(i);
				double[] vert = defaultVertex.clone();
				for (int j = 0, n = 0; j < format.length(); j++) {
					int q = defaultFormat.indexOf(format.charAt(j));
					if (q >= 0) vert[q] = vec[n++];
				}
				quad.vertices[i] = vert;
			}
			return quad;
		});
		defFunc.put("rect", (p) -> {
			Quad quad = new Quad();
			String format = p.getString(2);
			quad.tex = p.param.length > 3 ? (int)p.getNumber(3) : 0;
			quad.cullFace = format.length() <= 3 ? 0 : cfNames.indexOf(format.charAt(3));
			int t;
			int tN = (t = format.indexOf('-')) < 0 ? format.indexOf('+') + 3 : t;
			boolean inv = tN >= 3; tN %= 3;
			int tU = (t = format.indexOf('u')) < 0 ? format.indexOf('U') + 3 : t;
			int tV = (t = format.indexOf('v')) < 0 ? format.indexOf('V') + 3 : t;
			double[] pos = p.getVector(0), tex = p.getVector(1);
			for (int i = 0; i < 4; i++) {
				double[] vert = quad.vertices[inv ? i : 3 - i];
				vert[0] = pos[(tN == 0 ? inv : rect[tN - 1][i]) ? 3 : 0];
				vert[1] = pos[(tN == 1 ? inv : rect[(tN + 1) % 3][i]) ? 4 : 1];
				vert[2] = pos[(tN == 2 ? inv : rect[tN][i]) ? 5 : 2];
				vert[3] = tex[(tU >= 3 ^ rect[(5 - tU + tN) % 3][i]) ? 2 : 0];
				vert[4] = tex[(tV >= 3 ^ rect[(5 - tV + tN) % 3][i]) ? 3 : 1];
				vert[5] = 1; vert[6] = 1; vert[7] = 1; vert[8] = 1;
			}
			return quad;
		});
		quads = new List[] {
			new ArrayList<Quad>(),
			new ArrayList<Quad>(),
			new ArrayList<Quad>(),
			new ArrayList<Quad>(),
			new ArrayList<Quad>(),
			new ArrayList<Quad>(),
			new ArrayList<Quad>()
		};
	}

	@Override
	public void reset() {
		super.reset();
		for (List<Quad> list : quads) list.clear();
		states.setPos(-1);
		State state = new State();
		state.matrix = new Matrix4d();
		state.matrix.setIdentity();
		state.sU = 1.0;
		state.sV = 1.0;
		state.sColor = new double[] {1.0, 1.0, 1.0, 1.0};
		state.orient = 0x6543210;
		states.add(state);
	}

	public Module getOrLoad(String name, IResourceManager manager) throws Exception {
		Module m = modules.get(name);
		if (m != null) return m;
		IResource res = manager.getResource(new ResourceLocation(loadPath.toString() + name.replace('.', '/') + ".rcp"));
		Script script = Compiler.compile(this, name, new InputStreamReader(res.getInputStream()));
		IOperand var = script.variables.remove("dependencies");
		if (var instanceof Array) try {
			for (Object o : (Object[])var.value())
				if (o instanceof String) 
					getOrLoad((String)o, manager);
		} catch (Exception e) {
			throw (ScriptException) new ScriptException("failed loading dependency for script:", name, 0).initCause(e);
		}
		Function func = script.methods.remove("init");
		if (func != null) func.apply(new Parameters());
		return script;
	}

	public void run(Module script, String cmd) throws NoSuchMethodException, ScriptException {
		int p = cmd.indexOf('(');
		Parameters param;
		if (p >= 0) {
			int q = cmd.indexOf(')', p);
			if (q < 0) q = cmd.length();
			param = parseParam(cmd.substring(p + 1, q), script);
			cmd = cmd.substring(0, p);
		} else param = new Parameters();
		reset();
		script.invoke(cmd, param);
	}

	private Parameters parseParam(String s, Module m) {
		s = s.trim();
		if (s.isEmpty()) return new Parameters();
		String[] args = s.split(",");
		IOperand[] arr = new IOperand[args.length];
		for (int i = 0; i < args.length; i++)
			try {
				arr[i] = new Number(Double.parseDouble(args[i]));
			} catch (NumberFormatException e) {
				arr[i] = m.read(args[i]);
			}
		return new Parameters(arr);
	}

}
