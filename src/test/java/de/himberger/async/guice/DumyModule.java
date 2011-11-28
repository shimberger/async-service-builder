package de.himberger.async.guice;

import com.google.inject.AbstractModule;

import de.himberger.async.AsyncServiceBuilder;

public class DumyModule extends AbstractModule {

	@Override
	protected void configure() {
		AsyncServiceBuilder sb = new AsyncServiceBuilder();
		bind(DummyServiceAsync.class).toProvider(sb.asyncProviderFor(DummyServiceAsync.class, DummyService.class));
	}

}
