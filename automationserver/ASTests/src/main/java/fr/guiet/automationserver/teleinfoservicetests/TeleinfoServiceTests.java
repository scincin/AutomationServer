package fr.guiet.automationserver.teleinfoservicetests;

import fr.guiet.automationserver.business.TeleInfoService;

public class TeleinfoServiceTests {
	public static void main(String args[]) {
		
		TeleInfoService teleInfoService = new TeleInfoService();
		Thread teleInfoServiceThread = new Thread(teleInfoService);
		teleInfoServiceThread.start();
		
		//Test d'une minute
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}