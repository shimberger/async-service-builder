package de.himberger.async;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

class ClassUtils {

	static Class<?> getGenericFutureReturnType(Method method) {
		Type type = method.getGenericReturnType();
		if (type instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType) type;
			if (paramType.getRawType().equals(Future.class)) {
				return (Class<?>) paramType.getActualTypeArguments()[0];
			}
		}
		return null;
	}
	
	static boolean isApiMethod(Method methodCanidate) {
		// So far only public methods are considered
		return Modifier.isPublic(methodCanidate.getModifiers());
	}		
	
	static Method getImplementationMethod(Method apiMethod, Object implementor) {
		Class<?> implClass = implementor.getClass();
		String methodName = apiMethod.getName();
		try {
			Method implMethod = implClass.getMethod(methodName,apiMethod.getParameterTypes());
			Class<?> futureType = getGenericFutureReturnType(apiMethod);
			if (implMethod.getReturnType().equals(futureType)) {
				// If return type of implementation matches generic future type
				return implMethod;
			}
		} catch (SecurityException e) {
			throw new ServiceCreationException(e);
		} catch (NoSuchMethodException e) {
			// Do nothing, since we'll return null
		}
		return null;
	}	
	
}
