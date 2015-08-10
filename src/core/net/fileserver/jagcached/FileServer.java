package core.net.fileserver.jagcached;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import core.Configuration;
import core.game.util.Misc;
import core.game.util.log.CustomLogger;
import core.net.NetworkConstants;
import core.net.fileserver.jagcached.dispatch.RequestWorkerPool;
import core.net.fileserver.jagcached.net.FileServerHandler;
import core.net.fileserver.jagcached.net.HttpPipelineFactory;
import core.net.fileserver.jagcached.net.JagGrabPipelineFactory;

/**
 * The core class of the file server.
 * @author Graham
 */
public final class FileServer {
	
	/**
	 * The logger for this class.
	 */
	private static final Logger logger = Logger.getLogger(FileServer.class.getName());

	/**
	 * The entry point of the application.
	 * @param args The command-line arguments.
	 */
	public static void main(String[] args) {
		try {
			new FileServer().start();
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Error starting server.", t);
		}
	}
	
	/**
	 * The executor service.
	 */
	private final ExecutorService service = Executors.newCachedThreadPool();
	
	/**
	 * The request worker pool.
	 */
	private final RequestWorkerPool pool = new RequestWorkerPool();
	
	/**
	 * The file server event handler.
	 */
	private final FileServerHandler handler = new FileServerHandler();
	
	/**
	 * The timer used for idle checking.
	 */
	private final Timer timer = new HashedWheelTimer();
	
	/**
	 * Starts the file server
	 * @throws Exception if an error occurs.
	 */
	public void start() throws Exception {
		System.setOut(new CustomLogger(System.out));
		System.out.println("Initializing file server...");
		System.out.println("Starting workers...");
		pool.start();
		
		System.out.println("Starting services...");
		try {
			start("HTTP", new HttpPipelineFactory(handler, timer), NetworkConstants.HTTP_PORT);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Failed to start HTTP service.", t);
			logger.warning("HTTP will be unavailable. JAGGRAB will be used as a fallback by clients but this isn't reccomended!");
		}
		start("JAGGRAB", new JagGrabPipelineFactory(handler, timer), NetworkConstants.JAGGRAB_PORT);
		//start("ondemand", new OnDemandPipelineFactory(handler, timer), NetworkConstants.SERVICE_PORT);
		
		System.out.println("Ready for connections.");
	}

	/**
	 * Starts the specified service.
	 * @param name The name of the service.
	 * @param pipelineFactory The pipeline factory.
	 * @param port The port.
	 */
	private void start(String name, ChannelPipelineFactory pipelineFactory, int port) {
		SocketAddress address = new InetSocketAddress(port);
		
		System.out.println("Binding " + name + " service to " + address + "...");
		
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.setFactory(new NioServerSocketChannelFactory(service, service));
		bootstrap.setPipelineFactory(pipelineFactory);
		bootstrap.bind(address);
	}

}
