package de.himberger.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;

public class AsyncServiceBuilder {

	private static final String PARAM_PREFIX = "param";

	private static final String MSG_QUEUE = "msgQueue";

	private static final String SERVICE_FIELD = "service";

	private final ClassPool pool;
	
	private final ClassLoader classLoader;
	
	public AsyncServiceBuilder() {
		classLoader = getClass().getClassLoader();
		pool = ClassPool.getDefault();
		pool.importPackage("java.util.concurrent");
		pool.importPackage("com.google.inject");
		pool.importPackage("com.google.common.util.concurrent");
		pool.importPackage("de.himberger.git.server.async");
	}
	
	public String getDefaultImplementationName(Class<?> api) {
		String apiName = api.getSimpleName() + "AutoImpl";
		return apiName;
	}

	//TODO: Hack
	@SuppressWarnings("unchecked")
	public <T> Class<? extends Provider<T>> asyncProviderFor(Class<T> api,Class<?> clazz) {
		
		String className = api.getSimpleName() + "$Provider";
		CtClass providerClass = pool.makeClass(className);
		try {
			providerClass.addInterface(pool.get(Provider.class.getName()));
			
			CtField implField = CtField.make("private " + clazz.getName() + " impl;", providerClass);
			providerClass.addField(implField);			

			//TODO: Hack
			CtField sbField = CtField.make("public static " + this.getClass().getName() + " sb;", providerClass);
			providerClass.addField(sbField);				
			
			ConstPool constPool = providerClass.getClassFile().getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
			Annotation annoWebService = new Annotation(constPool, pool.get("com.google.inject.Inject"));
			attr.setAnnotation(annoWebService);			
			
			CtConstructor constructor = CtNewConstructor.make("public " + className + "(" + clazz.getName() + " impl) { this.impl = impl; }", providerClass);
			constructor.getMethodInfo().addAttribute(attr);
			providerClass.addConstructor(constructor);			
			
			CtMethod provideMethod = CtMethod.make("public Object get() { return ("+ api.getName() + ") sb.asyncInstanceOf(" + api.getName() + ".class,this.impl); }", providerClass);
			providerClass.addMethod(provideMethod);

			//CtMethod provideMethod2 = CtMethod.make("public Object get() { return ("+ api.getName() + ") sb.asyncInstanceOf(" + api.getName() + ".class,this.impl); }", providerClass);
			//providerClass.addMethod(provideMethod2);	
			
			Class cl = (Class<? extends Provider<T>>) providerClass.toClass(classLoader);
			
			cl.getField("sb").set(null,this);
			
			return cl;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T asyncInstanceOf(Class<T> api, Object implementor) {
		try {
			String asyncClassName = getDefaultImplementationName(api);
			List<Method> apiMethods = getApiMethods(api, implementor);
			Map<String,Method> messageMethods = generateMessageClasses(asyncClassName,apiMethods);
			CtClass serviceClass = generateSkeletonClass(api);
			serviceClass.addInterface(pool.get(api.getName()));
			
			CtField serviceField = CtField.make("private " + implementor.getClass().getName() + " service;", serviceClass);
			serviceClass.addField(serviceField);
			CtMethod runMethod = CtMethod.make("public void run() { " + CodeGenerator.generateRunMethodBody(messageMethods, MSG_QUEUE, PARAM_PREFIX, SERVICE_FIELD) + "}", serviceClass);
			serviceClass.addMethod(runMethod);
			
			CtConstructor constructor = CtNewConstructor.make("public " + asyncClassName + "(" + implementor.getClass().getName() + " impl) { this.service = impl; }", serviceClass);
			serviceClass.addConstructor(constructor);
			
			generateAsyncMethods(serviceClass,messageMethods);
			
			Object service = serviceClass.toClass().getConstructor(implementor.getClass()).newInstance(implementor);
			((Service) service).start();
			
			return (T) service;
		} catch (NotFoundException e) {
			throw new ServiceCreationException(e);
		} catch (InstantiationException e) {
			throw new ServiceCreationException(e);
		} catch (IllegalAccessException e) {
			throw new ServiceCreationException(e);
		} catch (CannotCompileException e) {
			throw new ServiceCreationException(e);
		} catch (ClassNotFoundException e) {
			throw new ServiceCreationException(e);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServiceCreationException(e);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServiceCreationException(e);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServiceCreationException(e);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServiceCreationException(e);
		}
	}

	private void generateAsyncMethods(CtClass serviceClass,Map<String,Method> messageMethods) throws ClassNotFoundException, CannotCompileException {
		for (String msgClassName : messageMethods.keySet()) {
			Method implMethod = messageMethods.get(msgClassName);
			Class<?> msgClass = Class.forName(msgClassName);
			StringBuilder sb = new StringBuilder();
			
			// Generate signature
			sb.append("public ").append("Future ")
			  .append(implMethod.getName()).append("(").append(CodeGenerator.generateParameterList(implMethod, PARAM_PREFIX)).append(")");
			
			//sb.append("{ return null; }");
			
			// Generate body
			sb.append(" {\n").append(CodeGenerator.generateAsyncMethodBody(implMethod,msgClass,MSG_QUEUE,PARAM_PREFIX)).append("}");
			
			CtMethod method = CtMethod.make(sb.toString(),serviceClass);
			serviceClass.addMethod(method);
		}
	}
	
	private List<Method> getApiMethods(Class<?> api, Object implementor) throws CannotCompileException {
		List<Method> apiMethods = new ArrayList<Method>();
		Method[] methods = api.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (ClassUtils.isApiMethod(method)) {
				Method implMethod = ClassUtils.getImplementationMethod(method, implementor);
				if (implMethod == null) {
					// The implementation is missing the right method
					throw new ServiceCreationException("No implementation found for " + method.getName() + " in " + implementor.getClass().getSimpleName());
				}
				apiMethods.add(implMethod);
			}
		}
		return apiMethods;
	}
	
	private Map<String,Method> generateMessageClasses(String prefix,List<Method> methods) throws CannotCompileException {
		int currentIndex = 0;
		Map<String,Method> messageMethods = new HashMap<String,Method>();
		for (Method method : methods) {
			String className = prefix + "$" + method.getName() + "$" + currentIndex++;
			// Generate source for message class
			CtClass msgClass = pool.makeClass(className); 
			
			// First add the field to hold the future
			CtField futureField = CtField.make("public Future future;", msgClass);
			msgClass.addField(futureField);			
			
			// Now add members for all method params
			int paramIndex = 0;
			for (Class<?> paramType : method.getParameterTypes()) {
				CtField paramField = CtField.make("public " + paramType.getName() + " param" + paramIndex++ + ";", msgClass);
				msgClass.addField(paramField);
			}
			
			msgClass.toClass(classLoader);
			messageMethods.put(className, method);
			
		}
		return messageMethods;
	}
	
	private CtClass generateSkeletonClass(Class<?> api) throws NotFoundException, CannotCompileException {
		CtClass superClass = pool.get(AbstractExecutionThreadService.class.getName());
		CtClass sc = pool.makeClass(getDefaultImplementationName(api),superClass);
		CtField queueField = CtField.make("private LinkedBlockingQueue msgQueue = new LinkedBlockingQueue();", sc);
		sc.addField(queueField);
		return sc;
	}
	

	

}
