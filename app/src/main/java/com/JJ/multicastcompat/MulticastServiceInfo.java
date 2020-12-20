package com.JJ.multicastcompat;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class MulticastServiceInfo {
	public int port;
	public InetAddress host;
	public String serviceName;
	public String serviceType;
	public HashMap<String, String> attributes;
	
	public MulticastServiceInfo() {
	}
	
	public MulticastServiceInfo(String serviceName, String serviceType, InetAddress host, int port) {
		this.serviceName = serviceName;
		this.serviceType = serviceType;
		this.host = host;
		this.port = port;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = (HashMap<String, String>) attributes;
	}
	
	public Map<String,String> getAttributes() {
		return attributes;
	}
	
	public MulticastServiceInfo(NsdServiceInfo serviceInfo) {
		this.serviceType = serviceInfo.getServiceType();
		this.serviceName = serviceInfo.getServiceName();
		this.port = serviceInfo.getPort();
		this.host = serviceInfo.getHost();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			attributes = new HashMap<>();
			Map<String, byte[]> map = serviceInfo.getAttributes();
			for (String key: map.keySet()) {
				attributes.put(key, new String(map.get(key)));
			}
		}
	}
	
	public NsdServiceInfo toNsdServiceInfo() {
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setServiceName(this.serviceName);
		serviceInfo.setServiceType(this.serviceType);
		serviceInfo.setHost(this.host);
		serviceInfo.setPort(this.port);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			for (String key: attributes.keySet()) {
				serviceInfo.setAttribute(key, attributes.get(key));
			}
		}
		return serviceInfo;
	}
	
	//TODO: overwrite toString
}
