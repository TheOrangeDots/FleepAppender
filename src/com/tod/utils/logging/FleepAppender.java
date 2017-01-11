/*
The MIT License (MIT)

Copyright (c) 2016 The Orange Dots

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.tod.utils.logging;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

public class FleepAppender extends AppenderSkeleton {

	private String url;
	private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return this.url;
	}

	private String userName = null;

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return this.userName;
	}
	
	public FleepAppender() {
	}

	public FleepAppender(boolean isActive) {
		super(isActive);
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		try {
			LogLog.debug("Sending 'POST' request to URL: " + url);
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-type", "application/json");
	
			ScriptObjectMirror payload = getJSObject();
			
			if (this.userName != null) {
				payload.put("user", this.userName);
			}

			StringBuffer content = new StringBuffer();
			content.append("*");
			content.append(event.getLevel().toString());
			content.append("* ");
			content.append(event.getMessage().toString());
			
			
			String[] stack = event.getThrowableStrRep();
			if (stack != null && stack.length > 0) {
				content.append("\n:::\n");
				content.append(String.join("\n", stack));
			}
			
			payload.put("message", content.toString());
			
			String payloadString = toJSONString(payload);
			LogLog.debug("Prequest payload: " + payloadString);
			
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(payloadString);
			wr.flush();
			wr.close();

			LogLog.debug("Response Code: " + con.getResponseCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			LogLog.debug("Response: " + response.toString());
		} catch (Exception e) {
			e.printStackTrace();
			LogLog.error("Error posting to Fleep", e);
		}
	}
	
	private static ScriptObjectMirror getJSObject() throws ScriptException {
		return (ScriptObjectMirror) engine.eval("new Object()");
	}
	
	private static String toJSONString(ScriptObjectMirror object) throws ScriptException {
		ScriptObjectMirror json = (ScriptObjectMirror) engine.eval("JSON");
		return json.callMember("stringify", object).toString();
	}
}