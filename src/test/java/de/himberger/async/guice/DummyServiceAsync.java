package de.himberger.async.guice;

import java.util.concurrent.Future;

public interface DummyServiceAsync {

	public Future<Long> getNumber(); 
	
}
