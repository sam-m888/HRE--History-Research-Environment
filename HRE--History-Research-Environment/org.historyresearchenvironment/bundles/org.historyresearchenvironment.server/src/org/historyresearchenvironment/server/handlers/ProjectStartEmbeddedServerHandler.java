package org.historyresearchenvironment.server.handlers;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.osgi.service.prefs.Preferences;

/**
 * Start the embedded Jetty server
 * 
 * @version 2018-07-02
 * @author Michael Erichsen, &copy; History Research Environment Ltd.
 *
 */
public class ProjectStartEmbeddedServerHandler {
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private final Preferences preferences = InstanceScope.INSTANCE.getNode("org.historyresearchenvironment");
	@Inject
	private IEventBroker eventBroker;

	/**
	 * Start embedded Jetty server
	 */
	@Execute
	public void execute() {
		Runnable runnable = new Runnable() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {

				Server server = new Server(preferences.getInt("SERVERPORT", 8000));

				try {
					server.getConnectors()[0].getConnectionFactory(HttpConnectionFactory.class);
					server.setHandler(new HreHttpRequestHandler());
					server.setStopAtShutdown(true);
					server.start();

					LOGGER.info("The server is running at " + server.getURI());
					eventBroker.post("MESSAGE", "The server is running at " + server.getURI());

					server.join();
				} catch (Exception e) {
					e.printStackTrace();
					LOGGER.severe(
							e.getClass() + ": " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
				}
			}
		};
		new Thread(runnable).start();
	}
}