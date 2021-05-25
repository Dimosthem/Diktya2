import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;


import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFileFormat.Type;

public class Tools {
	public static  int clientPort = -1;
	public static int serverPort = -1;
	
	
	public static void Echo(String code, int numOfPackets, boolean delay) throws IOException, InterruptedException{
		final float alpha = 7/8f;
		final float beta = 3/4f;
		final float gamma = 4f;
		if(clientPort ==-1 || serverPort ==-1) {
			System.out.println("You have to set the client port or the server port");
			return;
		}
		DatagramSocket s = new DatagramSocket();
		byte[] txbuffer = code.getBytes();
		byte[] resetBuffer = "E0000".getBytes();
		byte[] hostIp = { (byte)155,(byte)207,(byte)18,(byte)208 };
		
		InetAddress hostAddress = InetAddress.getByAddress(hostIp);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,
		hostAddress,serverPort);
		DatagramPacket reset = new DatagramPacket(resetBuffer, resetBuffer.length, hostAddress, serverPort);
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(10000);
		int packetCounter = 0;
		Queue<Long> queue = new LinkedList<>(); 
		float SRTT = 0;
		float var = 0;
		float RTO =0;
		for (int i=0; i<numOfPackets; i++) {
			long timeStart = System.nanoTime();
			
			if(delay)
				s.send(p);
			else {
				Thread.sleep(20);
				s.send(reset);
			}
			byte[] rxbuffer = new byte[2048];
			DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
			for (;;) {
				try {
					r.receive(q);
					long timeStop = System.nanoTime();
					long throughputEnd = timeStop/1_000_000;
					if(i==0 || (throughputEnd - queue.peek())<8000 ) {
						queue.add(throughputEnd);
					}else if((throughputEnd - queue.peek())>8000) {
						for(;;) {
							queue.remove();
							if((throughputEnd - queue.peek())<8000) {
								queue.add(throughputEnd);
								break;
							}
						}
						
					}
					float throughput = queue.size()/8f; 
					writeToFile("throughput.txt", String.valueOf(throughput));
					
					long duration = timeStop - timeStart;
					duration = duration/1_000_000;
					
					if(i==0) {
						SRTT = duration;
					}else {
						SRTT = alpha*SRTT + (1-alpha)*(float)duration;
						
						var = beta*var + (1-beta)*Math.abs(SRTT-duration);
						RTO = SRTT + gamma*var;
					}
					
					writeToFile("SRTT.txt", String.valueOf(SRTT));
					writeToFile("var.txt", String.valueOf(var));
					writeToFile("RTO.txt", String.valueOf(RTO));
					writeToFile("ping.txt", String.valueOf(duration));
					String message = new String(rxbuffer,0,q.getLength());
					System.out.print(message);
					if(message!="") {
						System.out.println("");
						break;
					}
				} catch (Exception x) {
					System.out.println(x);
					break;
				}
			}
		}
		r.close();
		s.close();
	}
	
	public static void imageReceive(String code) throws IOException{
		if(clientPort ==-1 || serverPort ==-1) {
			System.out.println("You have to set the client port or the server port");
			return;
		}
		DatagramSocket s = new DatagramSocket();
		byte[] txbuffer = code.getBytes();
		
		byte[] hostIp = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIp);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,
		hostAddress,serverPort);
		
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(10000);
		
		
			
		s.send(p);
			
			
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
			
		String arr =""; 
		byte[] dirtyBytesOfImage = new byte[80000];
		byte[] bytesOfImage;
		int offset =0;
		for (;;) { 
			try {
					
					
				r.receive(q);
				//arr = Arrays.toString(rxbuffer);
				//System.out.println(arr);
				byte[] b = copy(rxbuffer, q.getLength());
				int a =createBigArray(dirtyBytesOfImage, b, q.getLength(), offset);
				offset =a;
				System.out.println("The length of the packet is " + q.getLength() + " and the offset is "+offset);
					
					
				if(b[b.length-2]==(byte)0xFF && b[b.length-1]==(byte)0xD9) {
					System.out.println(Arrays.toString(b));
					bytesOfImage = removeZeros(dirtyBytesOfImage);
					imageToFile(bytesOfImage, "xairete.jpg");
						

					break;
				}
			} catch (Exception x) {
					
				System.out.println(x);
				System.out.println(arr);
				break;
			}
		}
		
	}
	public static void receiveSound(String code) throws IOException, LineUnavailableException, InterruptedException {
		int numberOfPackets = -1;
		boolean AQ = false;
		int avg = 0;
		int b=1;
		if(code.length()==5) {
			return;
		}else if(code.length() ==9 || code.length()==12) {
			if(code.length()==9)
				numberOfPackets = Integer.parseInt(code.substring(6,9));
			else
				numberOfPackets = Integer.parseInt(code.substring(6+3,9+3));
		}else if(code.length() == 11 || code.length()==14) {
			if(code.length()==11)
				numberOfPackets = Integer.parseInt(code.substring(8,11));
			else
				numberOfPackets = Integer.parseInt(code.substring(8+3,11+3));
			AQ = true;
		}
		
		
		System.out.println(numberOfPackets + " <---");
		if(clientPort ==-1 || serverPort ==-1) {
			System.out.println("You have to set the client port or the server port");
			return;
		}
		DatagramSocket s = new DatagramSocket();
		byte[] txbuffer = code.getBytes();
		
		byte[] hostIp = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIp);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,
		hostAddress,serverPort);
		
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(10000);
		s.send(p);
		
		String arr="";
		byte[] rxbuffer ;
		byte[] soundBytes;
		if(AQ) {
			rxbuffer = new byte[132];
			soundBytes = new byte[132*numberOfPackets];
		}else {
			rxbuffer = new byte[128];
			soundBytes = new byte[128*numberOfPackets];
		}
		
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		for (int i=0; i<numberOfPackets; i++) { 
			try {
					
					
				r.receive(q);
				arr = Arrays.toString(rxbuffer);
			//	System.out.println(rxbuffer.length + " " + q.getLength());
				createBigArray(soundBytes,rxbuffer,q.getLength(),i*q.getLength());
					
					
				
			} catch (Exception x) {
					
				System.out.println(x);
				System.out.println(i);
				break;
			}
		}
		byte[] finalSoundBytes = demodulate(soundBytes,AQ,numberOfPackets);
		
		
		InputStream data_stream = new ByteArrayInputStream(finalSoundBytes);
		//InputStream data_stream = (InputStream) Arrays.stream(finalSoundBytes);
        int bps =8;
        boolean BigEndian =false;
        if (AQ) {
        	bps =16;
        	BigEndian = true;
        }
        AudioFormat format = new AudioFormat(8000, bps, 1, true, BigEndian);
        AudioInputStream stream = new AudioInputStream(data_stream , format,
                		finalSoundBytes.length); //+44 for header
        File file = new File("file.wav");
        AudioSystem.write(stream, Type.WAVE, file);
              
		
	}
	
	public static void copter(String code) throws Exception {
		if(clientPort ==-1 || serverPort ==-1) {
			System.out.println("You have to set the client port or the server port");
			return;
		}
		
		
		DatagramSocket s = new DatagramSocket();
		byte[] txbuffer = code.getBytes();
		
		byte[] hostIp = { (byte)155,(byte)207,(byte)18,(byte)208 };
		
		InetAddress hostAddress = InetAddress.getByAddress(hostIp);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,
		hostAddress,48078);
		
		DatagramSocket r = new DatagramSocket(48078);
		r.setSoTimeout(10000);
		
			
		//s.send(p);
			
			
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		for (;;) {
			try {
				r.receive(q);
				String message = new String(rxbuffer,0,q.getLength());
				
				int LMotor;
				int RMotor;
				int altitude;
				float temperature;
				float pressure;
				LMotor = Integer.parseInt(message.substring(40, 43));
				RMotor = Integer.parseInt(message.substring(51, 54));
				altitude =  Integer.parseInt(message.substring(64, 67));
				temperature= Float.parseFloat(message.substring(81, 86));
				pressure = Float.parseFloat(message.substring(96,104));
				writeToFile("LMotor.txt", String.valueOf(LMotor));
				writeToFile("RMotor.txt", String.valueOf(RMotor));
				writeToFile("altitude.txt", String.valueOf(altitude));
				writeToFile("temperature.txt", String.valueOf(temperature));
				writeToFile("pressure.txt", String.valueOf(pressure));
				System.out.print(message);
				if(message!="") {
					System.out.println("");
					break;
				}
			} catch (Exception x) {
				System.out.println(x);
				break;
			}
		}
		r.close();
		s.close();
	}
	public static void vehicle(String code) throws Exception {
		if(clientPort ==-1 || serverPort ==-1) {
			System.out.println("You have to set the client port or the server port");
			return;
		}
		String PID = code.split(" ")[1];
		
		DatagramSocket s = new DatagramSocket();
		byte[] txbuffer = code.getBytes();
		
		byte[] hostIp = { (byte)155,(byte)207,(byte)18,(byte)208 };
		
		InetAddress hostAddress = InetAddress.getByAddress(hostIp);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,
		hostAddress,serverPort);
		
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(10000);
		
		Thread.sleep(20);
		s.send(p);
			
			
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		for (;;) {
			try {
				r.receive(q);
				String message = new String(rxbuffer,0,q.getLength());
				String result[] = message.split(" ");
				if(result.length==3) {
					int XX= Integer.parseInt(result[2],16);
					if(PID.contentEquals("0F")) {
						System.out.println("The intake air temperature is " + (XX-40) );
						writeToFile("IntakeAirTemperature.txt", String.valueOf(XX-40));
					}
					else if(PID.contentEquals("11")) {
						System.out.println("Throttle positiion is " + ((XX*100)/255));
						writeToFile("ThrottlePosition.txt", String.valueOf(((XX*100)/255)));
					}
					else if(PID.contentEquals("0D")) {
						System.out.println("Vehicle speed is " + (XX));
						writeToFile("vehicleSpeed.txt", String.valueOf(XX));
					}
					else if(PID.contentEquals("05")) {
						System.out.println("The coolant temperature is " + (XX-40));
						writeToFile("coolantTemperature.txt", String.valueOf(XX-40));
					}
				}
				else if(result.length==4) {
					int XX= Integer.parseInt(result[2],16);
					int YY= Integer.parseInt(result[3],16);
					
					if (PID.contentEquals("1F")) {
						System.out.println("Engine run time is " + (256*XX + YY));
						writeToFile("engineRunTime.txt", String.valueOf((256*XX + YY)));
					}
					else if (PID.contentEquals("0C")) {
						System.out.println("Engine RPM is " + (((XX*256)+YY)/4));
						writeToFile("engineRPM.txt", String.valueOf(((XX*256)+YY)/4));
						
					}
				}
				System.out.print(message);
				if(message!="") {
					System.out.println("");
					break;
				}
			} catch (Exception x) {
				System.out.println(x);
				break;
			}
		}
		s.close();
		r.close();
	}
	
	private static byte[] copy(byte[] b,int size) {
		byte []cb =new byte[size];
		for (int i=0; i<size; i++) {
			cb[i] =b[i];
		}
		return cb;
	}
	private static int createBigArray(byte[] image, byte[] packet, int size, int offset) {
		for (int i=0; i < size; i++) {
			image[offset+i] = packet[i];
		}
		return offset+size;
	}
	private static byte[] removeZeros(byte[] im) {
		int index = -1;
		for (int i=im.length-1; i>=0; i--) {
			if(im[i]==-39) {
				index =i;
				break;
			}
		}
		byte[] bytesOfImage = new byte[index+1];
		for (int i=0; i<=index; i++) {
			bytesOfImage[i] = im[i];
		}
		return bytesOfImage;
	}
	private static byte[] demodulate(byte[] soundBytes,boolean AQ,int numberOfPackets) throws InterruptedException, IOException {
		int avg = 0;
		int b =1;
		System.out.println("The total length is " + soundBytes.length);
		int[] differenceArray = new int[2*numberOfPackets*128];
		for (int i=0; i<soundBytes.length; i++) {
			if((AQ) && (i%132 ==0) ) {
					
					avg = (ByteBuffer.wrap(new byte[] {soundBytes[i+1],soundBytes[i]}).getShort());
					b = (ByteBuffer.wrap(new byte[] {soundBytes[i+3],soundBytes[i+2]}).getShort());
					
					writeToFile("avg.txt", String.valueOf(avg));
					writeToFile("b.txt", String.valueOf(b));
					
					continue;
			}
			else if(i%132>0 && i%132<4 && AQ) {
				continue;
			}
			//System.out.println(soundBytes[i] + " <bytes |highNibble " + soundBytes.length);
			int highNibble = (int) ((soundBytes[i]>>4)&0x0F);
			
			int lowNibble = (int) (soundBytes[i] & 0x0F);
			
			//System.out.println(highNibble);
			lowNibble -=8;
			highNibble -=8;
			lowNibble *=b;
			highNibble *=b;
			lowNibble += avg;
			highNibble +=avg;
			
			writeToFile("diafores.txt", String.valueOf(highNibble));
			writeToFile("diafores.txt", String.valueOf(lowNibble));
			//System.out.println("hignNibble  and low "+ highNibble + " " + lowNibble + " " + i%132);
			if (AQ) {
				if(i%132>=0 && i%132<4) {
					System.out.println(i);}
			//	System.out.println((2*i-8*((i/132)+1)) + " " + (2*i-8*((i/132)+1)+1) + "   " + i%132);
				
				differenceArray[2*i-8*((i/132)+1)] = highNibble;
				differenceArray[2*i-8*((i/132)+1)+1] = lowNibble;
			}else {
				differenceArray[2*i]=highNibble;
				differenceArray[2*i+1]=lowNibble;
			}
			
		}
		byte[] finalBytess;
		if(AQ) {
			short[] finalSoundBytes = new short[differenceArray.length+1];
			finalSoundBytes[0]=0;
			for (int i=0; i<differenceArray.length; i++) {
				int temp =   (differenceArray[i] + finalSoundBytes[i]);
	
				if(temp>32767) {
					finalSoundBytes[i+1]=32767;
					System.out.println("hello there, overflow");
				}else if(temp<-32768) {
					finalSoundBytes[i+1]=-32768;
					System.out.println("hello there, underflow");
				}else {
					finalSoundBytes[i+1]=(short) (differenceArray[i]+finalSoundBytes[i]);
					if(i==0) {
						writeToFile("samples.txt", String.valueOf(0));
					}
					writeToFile("samples.txt", String.valueOf(finalSoundBytes[i+1]));
				}	
				//finalSoundBytes[i+1]=temp;
				//finalSoundBytes[i+1]=(byte) temp;
				//System.out.println(finalSoundBytes[i+1] + " " + (differenceArray[i]+finalSoundBytes[i]) );
			}
			ByteBuffer finalBytes =ByteBuffer.allocate(finalSoundBytes.length*2);
			ShortBuffer sb = finalBytes.asShortBuffer();
			sb.put(finalSoundBytes);
		
			finalBytess = finalBytes.array();
		}
		else {
			finalBytess = new byte[differenceArray.length+1];
			finalBytess[0] =0;
			for (int i=0; i<differenceArray.length; i++) {
				short temp =  (short) (differenceArray[i] + finalBytess[i]);
	
				if(temp>127) {
					finalBytess[i+1]=127;
					System.out.println("hello there, overflow");
					
				}else if(temp<-128) {
					finalBytess[i+1]=-128;
					System.out.println("hello there, underflow");
					
				}else {
					finalBytess[i+1]= (byte) (differenceArray[i]+finalBytess[i]);
					if(i==0) {
						writeToFile("samples.txt", String.valueOf(0));
					}
					writeToFile("samples.txt", String.valueOf(finalBytess[i+1]));
				}	
				
			}
		}
		
		//System.out.println(Arrays.toString(finalBytess));
		return finalBytess;
		
		
	}
	
	private static void imageToFile(byte[] bytesOfImage, String name) throws IOException {
		InputStream in = new ByteArrayInputStream(bytesOfImage);
		BufferedImage bi = ImageIO.read(in);
		File outputfile = new File(name);
		ImageIO.write(bi, "jpg", outputfile);
	}
	private static void writeToFile(String name, String data) throws IOException {
		FileWriter file = new FileWriter(name,true);
		file.write(data + "\n");
		file.close();
	}
}
