package de.himberger.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

import de.himberger.async.CodeGenerator;

public class CodeGeneratorTestCase {

	public static class TestClass {
		
		public Double setNameAndAge(String name ,int age) {
			return new Double(0);
		}
		
	}
	
	@Test
	public void generateParameterList() throws SecurityException, NoSuchMethodException {
		Method m = TestClass.class.getMethod("setNameAndAge",String.class,int.class);
		assertNotNull(m);
		
		String paramList = CodeGenerator.generateParameterList(m, "p");
		assertEquals("java.lang.String p0,int p1",paramList);
	}
	
	@Test
	public void generateAsyncMethodBody() throws SecurityException, NoSuchMethodException {
		Method m = TestClass.class.getMethod("setNameAndAge",String.class,int.class);		
		assertNotNull(m);
		
		String methodBody = CodeGenerator.generateAsyncMethodBody(m, TestClass.class, "queue", "p");
		String testClassType = TestClass.class.getName();
		String expectedCode = ""+
				"Future future = SettableFuture.create();\n" +
				testClassType + " message = new " + testClassType + "();\n" +
				"message.future = future;\n" +
				"message.p0 = p0;\n" +
				"message.p1 = p1;\n" +
				"queue.put(message);\n" +
				"return future;\n";
		assertEquals(expectedCode,methodBody);
	}
	
	
}
