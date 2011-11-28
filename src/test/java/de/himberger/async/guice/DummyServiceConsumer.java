package de.himberger.async.guice;

import com.google.inject.Inject;


public class DummyServiceConsumer {

	private final DummyServiceAsync dummyService;
	
	@Inject
	public DummyServiceConsumer(DummyServiceAsync dummyService) {
		super();
		this.dummyService = dummyService;
	}
	
	public String doSomething() {
		long num = 0;
		try {
			num = dummyService.getNumber().get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return "Your lucky number is " + num;
	}
	
	
}
