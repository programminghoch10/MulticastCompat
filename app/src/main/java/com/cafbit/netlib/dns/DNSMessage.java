/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cafbit.netlib.dns;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class represents a single DNS message, and is capable
 * of parsing or constructing such a message.
 * <p>
 * see: http://www.ietf.org/rfc/rfc1035.txt
 *
 * @author simmons
 */
public class DNSMessage {
	
	private static short nextMessageId = 0;
	
	private short messageId;
	private LinkedList<DNSQuestion> questions = new LinkedList<DNSQuestion>();
	private LinkedList<DNSAnswer> answers = new LinkedList<DNSAnswer>();
	
	/**
	 * Construct a DNS host query
	 */
	public DNSMessage(String hostname) {
		messageId = nextMessageId++;
		questions.add(new DNSQuestion(DNSQuestion.Type.ANY, hostname));
	}
	
	/**
	 * Parse the supplied packet as a DNS message.
	 */
	public DNSMessage(byte[] packet) {
		parse(packet, 0, packet.length);
	}
	
	/**
	 * Parse the supplied packet as a DNS message.
	 */
	public DNSMessage(byte[] packet, int offset, int length) {
		parse(packet, offset, length);
	}
	
	public int length() {
		int length = 12; // header length
		for (DNSQuestion q : questions) {
			length += q.length();
		}
		for (DNSAnswer a : answers) {
			length += a.length();
		}
		return length;
	}
	
	public byte[] serialize() {
		DNSBuffer buffer = new DNSBuffer(length());
		
		// header
		buffer.writeShort(messageId);
		buffer.writeShort(0); // flags
		buffer.writeShort(questions.size()); // qdcount
		buffer.writeShort(answers.size()); // ancount
		buffer.writeShort(0); // nscount
		buffer.writeShort(0); // arcount
		
		// questions
		for (DNSQuestion question : questions) {
			question.serialize(buffer);
		}
		
		// answers
		for (DNSAnswer answer : answers) {
			answer.serialize(buffer);
		}
		
		return buffer.bytes;
	}
	
	private void parse(byte[] packet, int offset, int length) {
		DNSBuffer buffer = new DNSBuffer(packet, offset, length);
		
		// header
		messageId = buffer.readShort();
		buffer.readShort(); // flags
		int qdcount = buffer.readShort();
		int ancount = buffer.readShort();
		buffer.readShort(); // nscount
		buffer.readShort(); // arcount
		
		// questions
		questions.clear();
		for (int i = 0; i < qdcount; i++) {
			questions.add(new DNSQuestion(buffer));
		}
		
		// answers
		answers.clear();
		for (int i = 0; i < ancount; i++) {
			answers.add(new DNSAnswer(buffer));
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		// questions
		for (DNSQuestion q : questions) {
			sb.append(q.toString() + "\n");
		}
		
		// group answers by name
		SortedMap<String, List<DNSAnswer>> answersByName = new TreeMap<String, List<DNSAnswer>>();
		for (DNSAnswer a : answers) {
			List<DNSAnswer> list;
			if (answersByName.containsKey(a.name)) {
				list = answersByName.get(a.name);
			} else {
				list = new LinkedList<DNSAnswer>();
				answersByName.put(a.name, list);
			}
			list.add(a);
		}
		for (Map.Entry<String, List<DNSAnswer>> entry : answersByName.entrySet()) {
			sb.append(entry.getKey() + "\n");
			for (DNSAnswer a : entry.getValue()) {
				sb.append("  " + a.type.toString() + " " + a.getRdataString() + "\n");
			}
		}
		
		return sb.toString();
	}
	
	public Map<String, String> getAttributes() {
		HashMap<String, String> map = new HashMap<>();
		for (DNSAnswer answer : answers) {
			Map<String, String> attributes = answer.getAttributes();
			for (String key : attributes.keySet()) {
				map.put(key, attributes.get(key));
			}
		}
		return map;
	}
	
	public String getPTR() {
		for (DNSAnswer answer : answers) {
			if (answer.getPTR() == null) continue;
			return answer.getPTR();
		}
		return null;
	}
	
	public String getType() {
		String ptr = getPTR();
		if (ptr == null) return null;
		ptr = ptr.replace(".local", ".");
		int l = ptr.lastIndexOf("._");
		l = ptr.lastIndexOf("._", l);
		return ptr.substring(l + 1);
	}
	
	public String getHost() {
		String ptr = getPTR();
		if (ptr == null) return null;
		ptr = ptr.replace(".local", ".");
		int l = ptr.lastIndexOf("._");
		if (l == -1) return ptr;
		int l2 = ptr.lastIndexOf("._", l - 1);
		if (l2 == -1) return ptr.substring(0, l);
		return ptr.substring(0, l2);
	}
	
	public LinkedList<DNSAnswer> getAnswers() {
		return answers;
	}
	
}
