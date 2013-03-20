package eu.compassresearch.core.interpreter.values;

import org.overture.interpreter.values.SetValue;
import org.overture.interpreter.values.Value;


public class AbstractValueInterpreter {

	public static Value meet(Value val1, Value val2)
	{
		//if both are undefined we cannot come any close
		if(isAnyValue(val1) && isAnyValue(val2))
			return val1;
		else if(isAnyValue(val1))
			return val2;
		else if(isAnyValue(val2))
			return val1;
		else
			return null;
	}
	
	public static boolean isValuePrecise(Value value)
	{
		//TODO there should be a range/interval value
		return value != null && !isAnyValue(value);// && !(value instanceof SetValue);
	}
	
	public static boolean isMorePrecise(Value val1, Value val2)
	{
		//if both are undefined we 
		if(isAnyValue(val1) && isAnyValue(val2))
			return false;
		else if(isAnyValue(val1))
			return false;
		else if(isAnyValue(val2))
			return true;
		else
			return false;
	}
	
	public static boolean isAnyValue(Value value)
	{
		return value instanceof AnyValue;
	}
}