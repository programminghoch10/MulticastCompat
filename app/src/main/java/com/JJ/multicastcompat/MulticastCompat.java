package com.JJ.multicastcompat;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;

@SuppressWarnings("unused")
public class MulticastCompat {
	
	private static final String TAG = MulticastCompat.class.getSimpleName();
	private final NsdManager nsdManager;
	public boolean notifyOnUpdate = false;
	private LinkedHashMap<InetAddress, MulticastServiceInfo> map = new LinkedHashMap<>();
	private NsdManager.DiscoveryListener discoveryListener;
	private NsdManager.ResolveListener resolveListener;
	private DiscoveryListener relayDiscoveryListener;
	private MulticastSocket multicastSocket;
	
	private boolean enableNsd = true;
	private boolean enableSocket = true;
	
	/**
	 * This creates a new MulticastCompat object.
	 * An Thread for receiving messages will be opened for future use through discoverServices()
	 *
	 * @param context The context is needed to acquire the NsdManager system service
	 */
	public MulticastCompat(Context context) {
		this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
		setupNsdManagerProxy();
		multicastSocket = new MulticastSocket(context, new MulticastSocket.MulticastListener() {
			@Override
			public void onServiceFound(MulticastServiceInfo serviceInfo) {
				checkOnServiceFound(serviceInfo);
			}
		});
		multicastSocket.start();
	}
	
	public MulticastCompat(Context context, boolean enableNsd, boolean enableSocket) {
		if (enableNsd) {
			this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
			setupNsdManagerProxy();
		} else {
			this.nsdManager = null;
			this.enableNsd = false;
		}
		if (enableSocket) {
			multicastSocket = new MulticastSocket(context, new MulticastSocket.MulticastListener() {
				@Override
				public void onServiceFound(MulticastServiceInfo serviceInfo) {
					checkOnServiceFound(serviceInfo);
				}
			});
			multicastSocket.start();
		} else {
			this.enableSocket = false;
		}
		if (!enableNsd && !enableSocket) throw new IllegalStateException("all multicast methods disabled");
	}
	
	/**
	 * This checks and updates the service tracking list.
	 *
	 * @param serviceInfo the found service
	 */
	public void checkOnServiceFound(MulticastServiceInfo serviceInfo) {
		if (serviceInfo == null) return;
		if (serviceInfo.host == null) return;
		if (map.containsKey(serviceInfo.host)) {
			map.put(serviceInfo.host, MulticastServiceInfo.merge(map.get(serviceInfo.host), serviceInfo));
			if (notifyOnUpdate) relayDiscoveryListener.onServiceFound(map.get(serviceInfo.host));
		} else {
			map.put(serviceInfo.host, serviceInfo);
			relayDiscoveryListener.onServiceFound(serviceInfo);
		}
	}
	
	/**
	 * This configures the NsdManager Proxy for recieving and handling events.
	 */
	private void setupNsdManagerProxy() {
		discoveryListener = new NsdManager.DiscoveryListener() {
			@Override
			public void onStartDiscoveryFailed(String s, int i) {
				if (i == NsdManager.FAILURE_INTERNAL_ERROR) {
					//probably an issue with mDNS daemon
					// we can try with multicast socket anyways
					//TODO: only escalte internal error when both ways failed
				}
				relayDiscoveryListener.onStartDiscoveryFailed(s, i);
			}
			
			@Override
			public void onStopDiscoveryFailed(String s, int i) {
				relayDiscoveryListener.onStopDiscoveryFailed(s, i);
			}
			
			@Override
			public void onDiscoveryStarted(String s) {
				relayDiscoveryListener.onDiscoveryStarted(s);
			}
			
			@Override
			public void onDiscoveryStopped(String s) {
				relayDiscoveryListener.onDiscoveryStopped(s);
			}
			
			@Override
			public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
				checkOnServiceFound(new MulticastServiceInfo(nsdServiceInfo));
				resolveService(nsdServiceInfo);
				relayDiscoveryListener.onServiceFound(new MulticastServiceInfo(nsdServiceInfo));
			}
			
			@Override
			public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
				map.remove(nsdServiceInfo.getHost());
				relayDiscoveryListener.onServiceLost(new MulticastServiceInfo(nsdServiceInfo));
			}
		};
		resolveListener = new NsdManager.ResolveListener() {
			@Override
			public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
				relayDiscoveryListener.onResolveFailed(new MulticastServiceInfo(nsdServiceInfo), i);
			}
			
			@Override
			public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
				checkOnServiceFound(new MulticastServiceInfo(nsdServiceInfo));
			}
		};
	}
	
	public void setDiscoveryListener(DiscoveryListener discoveryListener) {
		this.relayDiscoveryListener = discoveryListener;
	}
	
	public void discoverServices(String serviceType) {
		if (relayDiscoveryListener == null) throw new NullPointerException();
		if (this.enableNsd) {
			nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this.discoveryListener);
		}
		if (this.enableSocket) {
			try {
				multicastSocket.discoverServices(serviceType);
			} catch (IOException e) {
				//TODO: only escalate internal error when both failed
				relayDiscoveryListener.onStartDiscoveryFailed(serviceType, NsdManager.FAILURE_INTERNAL_ERROR);
			}
		}
	}
	
	public void stopServiceDiscovery() {
		//nsdManager.stopServiceDiscovery(this.discoveryListener);
	}
	
	public void resolveService(NsdServiceInfo serviceInfo) {
		if (relayDiscoveryListener == null) throw new NullPointerException();
		nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
			@Override
			public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
				resolveListener.onResolveFailed(nsdServiceInfo, i);
			}
			
			@Override
			public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
				resolveListener.onServiceResolved(nsdServiceInfo);
			}
		});
	}
	
	public LinkedHashMap<InetAddress, MulticastServiceInfo> getDiscoveredServices() {
		return this.map;
	}
	
	public interface DiscoveryListener {
		void onStartDiscoveryFailed(String serviceType, int errorCode);
		
		void onStopDiscoveryFailed(String serviceType, int errorCode);
		
		void onDiscoveryStarted(String serviceType);
		
		void onDiscoveryStopped(String serviceType);
		
		void onResolveFailed(MulticastServiceInfo serviceInfo, int errorcode);
		
		void onServiceFound(MulticastServiceInfo serviceInfo);
		
		void onServiceLost(MulticastServiceInfo serviceInfo);
	}
}
