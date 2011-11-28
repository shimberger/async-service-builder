package de.himberger.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.himberger.async.guice.DummyServiceConsumer;
import de.himberger.async.guice.DumyModule;

public class GuiceIntegrationTestCase {
	
	@Test
	public void guiceProviderWorks() {
		Injector injector = Guice.createInjector(new DumyModule());
		DummyServiceConsumer consumer = injector.getInstance(DummyServiceConsumer.class);
		assertNotNull(consumer);
		assertEquals("Your lucky number is 42",consumer.doSomething());
		
	}
	
}
