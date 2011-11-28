package de.himberger.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import de.himberger.async.AsyncServiceBuilder;

public class AsyncServiceBuilderTestCase {

	private static final String HELLO_GREETING = "Hola World";
	
	public static class HelloService {
		
		public String greet() {
			return HELLO_GREETING;
		}
		
		public String echo(String txt) {
			return txt;
		}
		
	}
	
	public static interface HelloServiceAsync {
		
		public Future<String> greet();
		
	}
	
	public static interface EchoServiceAsync {
		
		public Future<String> echo(String txt);
		
	}
	
	private AsyncServiceBuilder serviceBuilder;
	
	@Before
	public void setUp() {
		serviceBuilder = new AsyncServiceBuilder();		
	}
	
	
	@Test
	public void getDefaultImplementationName() {
		assertEquals("HelloServiceAsyncAutoImpl",serviceBuilder.getDefaultImplementationName(HelloServiceAsync.class));
	}
	
	@Test
	public void asyncServiceCreationWithoutArgs() throws InterruptedException, ExecutionException, TimeoutException {
		HelloServiceAsync helloService = serviceBuilder.asyncInstanceOf(HelloServiceAsync.class,new HelloService());
		assertNotNull(helloService);
		assertNotNull(helloService.greet());
		assertEquals(HELLO_GREETING,helloService.greet().get(10,TimeUnit.MILLISECONDS));
		
	}
	
	@Test
	public void asyncServiceCreationWithArgs() throws InterruptedException, ExecutionException, TimeoutException {
		EchoServiceAsync echoService = serviceBuilder.asyncInstanceOf(EchoServiceAsync.class,new HelloService());
		assertNotNull(echoService);
		assertNotNull(echoService.echo("Hello"));
		assertEquals("Hello",echoService.echo("Hello").get(10,TimeUnit.MILLISECONDS));
		
	}	
	
}
