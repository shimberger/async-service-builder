package de.himberger.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.junit.Test;

import de.himberger.async.ClassUtils;

public class ClassUtilsTestCase {

	public static interface SimpleInterface {
		
		public Future<String> test();
		
	}
	
	@Test
	public void getGenericReturnType() throws SecurityException, NoSuchMethodException {
		Method m = SimpleInterface.class.getMethod("test",new Class<?>[0]);
		assertNotNull(m);
		assertEquals(String.class,ClassUtils.getGenericFutureReturnType(m));
	}
	
}
