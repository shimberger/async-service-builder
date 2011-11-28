package de.himberger.async;

public class ServiceCreationException extends RuntimeException {

	private static final long serialVersionUID = -7481151554122225223L;

	public ServiceCreationException() {
		super();
	}

	public ServiceCreationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ServiceCreationException(String arg0) {
		super(arg0);
	}

	public ServiceCreationException(Throwable arg0) {
		super(arg0);
	}

}
