package infinity.resource.vef;

import infinity.resource.AbstractStruct;

@SuppressWarnings("serial")
public class Component1 extends CompBase 
{
	public Component1() throws Exception 
	{
		super("Component1");
	}

	public Component1(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception 
	{
		super(superStruct, buffer, offset, "Component1");
	}
}
