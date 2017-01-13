package fr.guiet.automationserver;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import java.util.Locale;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import fr.guiet.automationserver.business.*;
import fr.guiet.automationserver.dto.SMSDto;

/**
 * Main class : in charge of home IoT management
 * 
 * Creation date : 201602
 * 
 * @author guiet
 *
 */
public class AutomationServer implements Daemon {

	private Thread _mainThread = null; // Thread principal
	private TeleInfoService _teleInfoService = null; // service de teleinfo
	private RoomService _roomService = null; // service de room service
	private WaterHeater _waterHeater = null; // service de gestion du
												// chauffe-eau
	private boolean _isStopped = false;
	private Thread _roomServiceThread = null;
	private Thread _teleInfoServiceThread = null;
	private Thread _waterHeaterServiceThread = null;
	private SMSGammuService _smsGammuService = null;

	// Logger
	private static Logger _logger = Logger.getLogger(AutomationServer.class);

	// Init deamon
	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		/*
		 * Construct objects and initialize variables here. You can access the
		 * command line arguments that would normally be passed to your main()
		 * method as follows:
		 */
		// String[] args = daemonContext.getArguments();

		// Initialisation du Thread principal
		_mainThread = new Thread() {

			// Méthode de démarrage du Thread principale
			@Override
			public synchronized void start() {
				AutomationServer.this._isStopped = false;
				super.start(); // Démarrage du Thread principal (Appel de la
								// méthode run)
			}

			// Méthode principale du Thread principal (celle qui boucle)
			@Override
			public void run() {

				try {
					_logger.info("Starting automation server...");

					// Set local to en_GB
					Locale.setDefault(new Locale("en", "GB"));
					
					//SMS Service
					_smsGammuService = new SMSGammuService();

					// Starting teleinfo service
					_teleInfoService = new TeleInfoService(_smsGammuService);
					_teleInfoServiceThread = new Thread(_teleInfoService);
					_teleInfoServiceThread.start();

					// Starting room service
					_roomService = new RoomService(_teleInfoService, _smsGammuService);
					_roomServiceThread = new Thread(_roomService);
					_roomServiceThread.start();

					// Starting warter heater
					_waterHeater = new WaterHeater(_teleInfoService, _smsGammuService);
					_waterHeaterServiceThread = new Thread(_waterHeater);
					_waterHeaterServiceThread.start();

					// TODO : Replace this server by MQTT subscribe
					ServerSocket socket = new ServerSocket(4310);
					_logger.info("Starting messages management queue...");

					SMSDto sms = new SMSDto();
					sms.setMessage("Automation server has started...");
					_smsGammuService.sendMessage(sms, true);
					
					while (!_isStopped) {

						Socket connection = socket.accept();

						AutomationServerThread ast = new AutomationServerThread(connection, _roomService,
								_teleInfoService);
						ast.start();

					}

					try {
						socket.close();
					} catch (IOException e) {
						_logger.error("Error occured when closing message management queue socket...", e);
						socket = null;
					}
				} catch (Exception e) {
					_logger.error("Error occured in automation server...", e);

					//try to stop services properly
					stop();
				}
			}
		};
	}

	// Méthode start de jsvc (Classe Deamon)
	@Override
	public void start() throws Exception {
		_mainThread.start(); // Démarrage du thread principal
	}

	
	/* (non-Javadoc)
	 * @see org.apache.commons.daemon.Daemon#stop()
	 */
	@Override
	public void stop() throws Exception {

		try {
			_logger.info("Stopping automation server...");
			
			// Stopping all services
			_teleInfoService.StopService();
			_roomService.StopService();
			_waterHeater.StopService();

			_isStopped = true;

			_mainThread.join(5000); // Attend la fin de l'exécution du Thread
									// principal (5 secondes)
					
			_logger.info("Bye bye automation server stopped...");
			
		} catch (Exception e) {
			_logger.error("Automation server has not stopped properly", e);
		}
		finally {
			SMSDto sms = new SMSDto();
			sms.setMessage("Automation server has stopped...");
			_smsGammuService.sendMessage(sms, true);
		}
	}

	// Destroy objects
	@Override
	public void destroy() {
		_mainThread = null;
		_teleInfoService = null;
		_roomService = null;
		_waterHeater = null;
	}

}