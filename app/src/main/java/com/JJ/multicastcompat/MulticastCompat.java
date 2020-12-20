package com.JJ.multicastcompat;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;

@SuppressWarnings("unused")
public class MulticastCompat {
	
	private static final String TAG = MulticastCompat.class.getSimpleName();
	private final NsdManager nsdManager;
	private LinkedHashMap<InetAddress, NsdServiceInfo> map = new LinkedHashMap<>();
	private NsdManager.DiscoveryListener discoveryListener;
	private NsdManager.ResolveListener resolveListener;
	private DiscoveryListener relayDiscoveryListener;
	private MulticastSocket multicastSocket;
	
	public MulticastCompat(Context context) {
		this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
		setupDiscoveryProxy();
		setupResolveProxy();
		multicastSocket = new MulticastSocket(context, new MulticastSocket.MulticastListener() {
			@Override
			public void onServiceFound(MulticastServiceInfo serviceInfo) {
				relayDiscoveryListener.onServiceResolved(serviceInfo);
			}
		});
		multicastSocket.start();
	}
	
	private void setupDiscoveryProxy() {
		discoveryListener = new NsdManager.DiscoveryListener() {
			@Override
			public void onStartDiscoveryFailed(String s, int i) {
				if (i == NsdManager.FAILURE_INTERNAL_ERROR) {
					//probably an issue with mDNS daemon
					// we can try with multicast socket anyways
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
				map.put(nsdServiceInfo.getHost(), nsdServiceInfo);
				resolveService(nsdServiceInfo);
				relayDiscoveryListener.onServiceFound(new MulticastServiceInfo(nsdServiceInfo));
			}
			
			@Override
			public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
				map.remove(nsdServiceInfo.getHost());
				relayDiscoveryListener.onServiceLost(new MulticastServiceInfo(nsdServiceInfo));
			}
		};
	}
	
	private void setupResolveProxy() {
		resolveListener = new NsdManager.ResolveListener() {
			@Override
			public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
				relayDiscoveryListener.onResolveFailed(new MulticastServiceInfo(nsdServiceInfo), i);
			}
			
			@Override
			public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
				map.put(nsdServiceInfo.getHost(), nsdServiceInfo);
				relayDiscoveryListener.onServiceResolved(new MulticastServiceInfo(nsdServiceInfo));
			}
		};
	}
	
	public void setDiscoveryListener(DiscoveryListener discoveryListener) {
		this.relayDiscoveryListener = discoveryListener;
	}
	
	public void discoverServices(String serviceType) {
		if (relayDiscoveryListener == null) throw new NullPointerException();
		nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this.discoveryListener);
		try {
			multicastSocket.discoverServices(serviceType);
		} catch (IOException e) {
			//TODO: only escalate internal error when both failed
			relayDiscoveryListener.onStartDiscoveryFailed(serviceType, NsdManager.FAILURE_INTERNAL_ERROR);
		}
	}
	
	public void stopServiceDiscovery() {
		//nsdManager.stopServiceDiscovery(this.discoveryListener);
	}
	
	public void resolveService(NsdServiceInfo serviceInfo) {
		if (relayDiscoveryListener == null) throw new NullPointerException();
		nsdManager.resolveService(serviceInfo, resolveListener);
	}
	
	public LinkedHashMap<InetAddress, NsdServiceInfo> getDiscoveredServices() {
		return this.map;
	}
	
	public interface DiscoveryListener {
		void onStartDiscoveryFailed(String serviceType, int errorCode);
		
		void onStopDiscoveryFailed(String serviceType, int errorCode);
		
		void onDiscoveryStarted(String serviceType);
		
		void onDiscoveryStopped(String serviceType);
		
		void onResolveFailed(MulticastServiceInfo serviceInfo, int errorcode);
		
		void onServiceResolved(MulticastServiceInfo serviceInfo);
		
		void onServiceFound(MulticastServiceInfo serviceInfo);
		
		void onServiceLost(MulticastServiceInfo serviceInfo);
	}
}
