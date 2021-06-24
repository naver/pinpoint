package com.navercorp.pinpoint.plugin.ice.methodfilter;

import java.lang.reflect.Modifier;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilter;

public class IceMethodFilter implements MethodFilter{
	
      @Override
      public boolean accept(InstrumentMethod method) {
    	 String[] args = method.getParameterTypes();
    	 if(null==args||args.length==0)
    	 {
    		 return false;
    	 }
    	 int length = args.length;
    	 if(!args[length-1].equals("boolean")||!args[length-2].equals("java.util.Map"))
    	 {
    		 return false;
    	 }
    	 final int modifiers = method.getModifiers();
    	 if(!Modifier.isPrivate(modifiers))
    	 {
    		 return false;
    	 }
    	 return true;
      }
}
