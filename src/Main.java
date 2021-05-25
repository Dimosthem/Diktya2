import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.sampled.LineUnavailableException;

public class Main {

	public static void main(String[] args) throws Exception {
		Tools.clientPort = 48032;
		Tools.serverPort = Tools.clientPort -10000;
		
		Tools.Echo("E6954", 5, true);
		//Tools.imageReceive("M2432CAM=PTZ");
		//Tools.receiveSound("A6209AQF999");
		
		//for(int i=0; i<9999; i++)
		//	Tools.copter("Q0263");
		
		String vehicleCode = "V7646";
		for (int i=0; i<99999; i++) {
			Tools.vehicle(vehicleCode + "OBD=01 1F");
			Thread.sleep(50);
			Tools.vehicle(vehicleCode + "OBD=01 0F");
			Thread.sleep(50);
			Tools.vehicle(vehicleCode + "OBD=01 11");
			Thread.sleep(50);
			Tools.vehicle(vehicleCode + "OBD=01 0C");
			Thread.sleep(50);
			Tools.vehicle(vehicleCode + "OBD=01 0D");
			Thread.sleep(50);
			Tools.vehicle(vehicleCode + "OBD=01 05");
			Thread.sleep(50);
		}
	}

}
