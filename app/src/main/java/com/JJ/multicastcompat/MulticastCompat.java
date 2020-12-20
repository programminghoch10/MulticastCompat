package com.JJ.multicastcompat;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.net.InetAddress;
import java.util.LinkedHashMap;

@SuppressWarnings("unused")
public class MulticastCompat {
	
	private static final String TAG = MulticastCompat.class.getSimpleName();
	private final NsdManager nsdManager;
	public boolean autoResolve = false; //when this is true, found services are automatically resolved
	private LinkedHashMap<InetAddress, NsdServiceInfo> map = new LinkedHashMap<>();
	private NsdManager.DiscoveryListener discoveryListener;
	private NsdManager.ResolveListener resolveListener;
	private DiscoveryListener relayDiscoveryListener;
	private ResolveListener relayResolveListener;
	
	public MulticastCompat(Context context) {
		this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
		setupDiscoveryProxy();
		setupResolveProxy();
	}
	
	private void setupDiscoveryProxy() {
		discoveryListener = new NsdManager.DiscoveryListener() {
			@Override
			public void onStartDiscoveryFailed(String s, int i) {
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
				if (autoResolve) resolveService(nsdServiceInfo);
				relayDiscoveryListener.onServiceFound(nsdServiceInfo);
			}
			
			@Override
			public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
				map.remove(nsdServiceInfo.getHost());
				relayDiscoveryListener.onServiceLost(nsdServiceInfo);
			}
		};
	}
	
	private void setupResolveProxy() {
		resolveListener = new NsdManager.ResolveListener() {
			@Override
			public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
				relayResolveListener.onResolveFailed(nsdServiceInfo, i);
			}
			
			@Override
			public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
				map.put(nsdServiceInfo.getHost(), nsdServiceInfo);
				relayResolveListener.onServiceResolved(nsdServiceInfo);
			}
		};
	}
	
	public void setDiscoveryListener(DiscoveryListener discoveryListener) {
		this.relayDiscoveryListener = discoveryListener;
	}
	
	public void setResolveListener(ResolveListener resolveListener) {
		this.relayResolveListener = resolveListener;
	}
	
	public void discoverServices(String serviceType, int protocolType) {
		if (relayDiscoveryListener == null) throw new NullPointerException();
		nsdManager.discoverServices(serviceType, protocolType, this.discoveryListener);
	}
	
	public void stopServiceDiscovery() {
		nsdManager.stopServiceDiscovery(this.discoveryListener);
	}
	
	public void resolveService(NsdServiceInfo serviceInfo) {
		if (relayResolveListener == null) throw new NullPointerException();
		nsdManager.resolveService(serviceInfo, resolveListener);
	}
	
	public LinkedHashMap<InetAddress, NsdServiceInfo> getDiscoveredServices() {
		return this.map;
	}
}
