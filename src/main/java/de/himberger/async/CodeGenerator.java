package de.himberger.async;

import java.lang.reflect.Method;
import java.util.Map;

class CodeGenerator {

	/**
	 * Generates the parameter list for the specified method.
	 * 
	 * @param method The method to generate parameter list for
	 * @param paramPrefix The parameter name prefix
	 * @return The parameter list declaration
	 */
	static String generateParameterList(Method method, String paramPrefix) {
		StringBuilder sb = new StringBuilder();
		int paramIndex = 0;
		Class<?>[] paramTypes = method.getParameterTypes();
		for (Class<?> paramType : paramTypes) {
			// append parameter type and name
			sb.append(paramType.getName()).append(" ").append(paramPrefix).append(paramIndex++);
			if (paramIndex < paramTypes.length) {
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	static String generateRunMethodBody(Map<String,Method> messageMethods, String queueField, String paramPrefix, String implementorField) {
		StringBuilder sb = new StringBuilder();
		sb.append("while (isRunning()) { \n");
		sb.append("Object msg = null;");
		sb.append(" try { msg = ").append(queueField).append(".poll(500l,TimeUnit.MILLISECONDS); } catch (InterruptedException e) {} \n");
		for (String msgClass : messageMethods.keySet()) {
			Method implMetod = messageMethods.get(msgClass);
			// Check if message is of msg class, cast it and call implementors method
			sb.append("if (msg != null && msg instanceof ").append(msgClass).append(" ) {\n");
			sb.append("  ").append(msgClass).append(" castMsg = (").append(msgClass).append(") msg;\n");
			StringBuilder argString = new StringBuilder();
			for (int i = 0; i < implMetod.getParameterTypes().length; i++) {
				if (i > 0) {
					argString.append(",");
				}
				argString.append("castMsg.").append(paramPrefix).append(i);
			}
			if (implMetod.getReturnType() != null) {
				sb.append(implMetod.getReturnType().getName()).append(" retVal = ").append(implementorField).append(".").append(implMetod.getName()).append("(").append(argString).append(");\n");
				sb.append("((SettableFuture)castMsg.future).set(retVal);\n");
			}
			sb.append("}\n");
		}
		sb.append("}\n");
		
		return sb.toString();
	}	
	
	/**
	 * Generates the method body (without curly braces) to populate
	 * the message object with the parameters, post the message to
	 * the message queue and return an appropriate Future object.
	 * 
	 * @param implMethod The implementation method to generate async code for
	 * @param msgClass The message class
	 * @param queueMember The member name of the queue
	 * @param paramPrefix The prefix for parameters
	 * 
	 * @return The code for the message body
	 */
	static String generateAsyncMethodBody(Method implMethod, Class<?> msgClass, String queueMember, String paramPrefix) {
		StringBuilder sb = new StringBuilder();
		String msgClassName = msgClass.getName();
		
		// Instantiate the future
		sb.append("Future future = ").append("SettableFuture.create();\n");
		
		// Instantiate new message
		sb.append(msgClassName).append(" ").append("message = new ").append(msgClassName).append("();\n");
		
		// Assign future to the message
		sb.append("message.future = future;\n");
		
		// Assign method parameters to message object members
		for (int i = 0; i < implMethod.getParameterTypes().length; i++) {
			// TODO: The members should be immutable and a constructor should be used
			sb.append("message.").append(paramPrefix).append(i).append(" = ").append(paramPrefix).append(i).append(";\n");
		}
		
		// Add message to the queue
		sb.append(queueMember).append(".put(message);\n");
		
		// Return the future to the caller
		sb.append("return future;\n");
		
		return sb.toString();
	}
	
	
}
