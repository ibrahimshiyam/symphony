package eu.compassresearch.core.interpreter.debug;

import org.overture.ast.intf.lex.ILexLocation;
import org.overture.interpreter.debug.DBGPReader;
import org.overture.interpreter.debug.DBGPReason;
import org.overture.interpreter.runtime.Breakpoint;
import org.overture.interpreter.runtime.Context;
import org.overture.interpreter.runtime.ContextException;
import org.overture.interpreter.runtime.Interpreter;
import org.overture.interpreter.values.CPUValue;

import eu.compassresearch.core.interpreter.api.CmlInterpreter;

public class CmlDBGPReader extends DBGPReader
{

	private CmlInterpreter interpreter;

	public CmlDBGPReader(String host, int port, String ideKey,
			Interpreter interpreter, String expression, CPUValue cpu)
	{
		super(host, port, ideKey, interpreter, expression, cpu);
	}

	public CmlDBGPReader(CmlInterpreter interpreter)
	{
		super("", 0, "", null, "", null);
		this.interpreter = interpreter;
	}

	@Override
	public void stopped(Context ctxt, Breakpoint bp)
	{
		System.out.println("Stopped as " + bp + " location: " + bp.location);
		try
		{
			interpreter.setCurrentDebugContext(ctxt, bp.location);
			interpreter.suspend();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopped(Context ctxt, ILexLocation location)
	{
		System.out.println("Stopped as " + location);
	}

	@Override
	public void complete(DBGPReason reason, ContextException ctxt)
	{
	}

}
