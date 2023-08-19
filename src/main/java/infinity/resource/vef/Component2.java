package infinity.resource.vef;

import infinity.resource.AbstractStruct;

@SuppressWarnings("serial")
public class Component2 extends CompBase 
{
	public Component2() throws Exception 
	{
		super("Component2");
	}

	public Component2(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception 
	{
		super(superStruct, buffer, offset, "Component2");
	}
}
