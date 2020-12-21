package com.JJ.multicastcompat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.cafbit.netlib.NetUtil;
import com.cafbit.netlib.Packet;
import com.cafbit.netlib.dns.DNSMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Set;

class MulticastSocket extends Thread {
	public static final String TAG = MulticastSocket.class.getSimpleName();
	
	private static final byte[] MDNS_ADDR = {(byte) 224, (byte) 0, (byte) 0, (byte) 251};
	private static final int MDNS_PORT = 5353;
	private static final int BUFFER_SIZE = 4096;
	private static final int TTL = 2;
	private static final int REQUERY_TIMEOUT = 20;
	private java.net.MulticastSocket multicastSocket;
	private NetworkInterface networkInterface;
	private InetAddress groupAddress;
	private NetUtil netUtil;
	private String queryHostname = "";
	private boolean stop = false;
	private MulticastListener multicastListener;
	
	MulticastSocket(Context context, MulticastListener listener) {
		netUtil = new NetUtil(context);
		setListener(listener);
		
	}
	
	public void setListener(MulticastListener listener) {
		this.multicastListener = listener;
	}
	
	@Override
	public void run() {
		super.run();
		Log.v(TAG, "starting network thread");
		
		Set<InetAddress> localAddresses = NetUtil.getLocalAddresses();
		WifiManager.MulticastLock multicastLock = null;
		
		// initialize the network
		try {
			networkInterface = netUtil.getFirstWifiOrEthernetInterface();
			if (networkInterface == null) {
				throw new IOException("Your WiFi is not enabled.");
			}
			groupAddress = InetAddress.getByAddress(MDNS_ADDR);
			
			multicastLock = netUtil.getWifiManager().createMulticastLock("unmote");
			multicastLock.acquire();
			//Log.v(TAG, "acquired multicast lock: "+multicastLock);
			
			openSocket();
		} catch (IOException e) {
			//TODO: escalate error
			Log.e(TAG, "run: SOCKET INITIALIZATION FAILED", e);
			return;
		}
		
		// set up the buffer for incoming packets
		byte[] responseBuffer = new byte[BUFFER_SIZE];
		DatagramPacket response = new DatagramPacket(responseBuffer, BUFFER_SIZE);
		
		long lastquery = 0;
		
		Log.d(TAG, "run: now accepting multicast responses");
		
		while (!this.stop) {
			// zero the incoming buffer for good measure.
			java.util.Arrays.fill(responseBuffer, (byte) 0); // clear buffer
			
			// check if re-query is necessary
			if (lastquery + REQUERY_TIMEOUT < getUTC()) {
				
				// reopen the socket
				try {
					openSocket();
				} catch (IOException e1) {
					//TODO: handle cant reopen socket exception
					Log.e(TAG, "run: could not open socket", e1);
					return;
				}
				
				// process commands
				try {
					query(queryHostname);
				} catch (IOException e2) {
					//TODO: handle exception
					Log.e(TAG, "run: Could not query hostname", e2);
				}
				
				lastquery = getUTC();
				
				continue;
			}
			
			// receive a packet (or process an incoming command)
			try {
				multicastSocket.receive(response);
			} catch (IOException e) {
				Log.e(TAG, "run: recieve error", e);
			}
			
            /*
            Log.v(TAG, String.format("received: offset=0x%04X (%d) length=0x%04X (%d)", response.getOffset(), response.getOffset(), response.getLength(), response.getLength()));
            Log.v(TAG, Util.hexDump(response.getData(), response.getOffset(), response.getLength()));
            */
			
			// ignore our own packet transmissions.
			if (localAddresses.contains(response.getAddress())) {
				continue;
			}
			
			// parse the DNS packet
			DNSMessage message;
			try {
				message = new DNSMessage(response.getData(), response.getOffset(), response.getLength());
			} catch (Exception e) {
				//activity.ipc.error(e);
				//TODO: handle exception
				continue;
			}
			
			// send the packet to the UI
			Packet packet = new Packet(response, multicastSocket);
			packet.description = message.toString().trim();
			Log.d(TAG, "run: packet message is \"" + packet.description + "\"");
			//TODO: parse packet here
			MulticastServiceInfo serviceInfo = new MulticastServiceInfo(message.getHost(), message.getType(), packet.src, packet.srcPort);
			serviceInfo.setAttributes(message.getAttributes());
			multicastListener.onServiceFound(serviceInfo);
			
			//activity.ipc.addPacket(packet);
		}
		
		// release the multicast lock
		multicastLock.release();
		multicastLock = null;
		
		Log.v(TAG, "stopping network thread");
	}
	
	private void openSocket() throws IOException {
		multicastSocket = new java.net.MulticastSocket(MDNS_PORT);
		multicastSocket.setTimeToLive(TTL);
		multicastSocket.setReuseAddress(true);
		multicastSocket.setNetworkInterface(networkInterface);
		multicastSocket.joinGroup(groupAddress);
	}
	
	public void discoverServices(String serviceType) throws IOException {
		this.queryHostname = serviceType;
	}
	
	private void query(String hostname) throws IOException {
		hostname += "local";
		byte[] requestData = (new DNSMessage(hostname)).serialize();
		DatagramPacket request =
				new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_PORT);
		Log.d(TAG, "query: quering for " + hostname);
		multicastSocket.send(request);
	}
	
	void end() {
		this.stop = true;
	}
	
	private long getUTC() {
		return System.currentTimeMillis() / 1000;
	}
	
	String getQueryHostname() {
		return this.queryHostname;
	}
	
	void setQueryHostname(String hostname) {
		this.queryHostname = hostname;
	}
	
	public interface MulticastListener {
		void onServiceFound(MulticastServiceInfo serviceInfo);
	}
}
