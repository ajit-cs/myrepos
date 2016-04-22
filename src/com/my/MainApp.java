package com.my;

import org.apache.http.Header;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MainApp {

	String USER_AGENT = "Mozilla/5.0";
	CloseableHttpClient client;
	public void cluster_check() throws Exception
	{
		String CHECK_SLAVE = "http://192.168.132.217:7477/db/manage/server/ha/slave";
		String CHECK_MASTER = "http://192.168.132.217:7474/db/manage/server/ha/master";
		String CHECK_ALL = "http://192.168.132.217:7475/db/manage/server/ha/available";
		// System.out.println("id"+st.nextToken());
		client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(CHECK_ALL);
		
		httpGet.addHeader(BasicScheme.authenticate(
				 new UsernamePasswordCredentials("neo4j", "pass123"),
				 "UTF-8", false));
		httpGet.addHeader("User-Agent", USER_AGENT);
		//httpGet.addHeader("Cookie", st.nextToken());
		CloseableHttpResponse httpResponse = client.execute(httpGet);
		System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		String inputLine;
		StringBuffer response2 = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			response2.append(inputLine);
		}
		reader.close();
		// print result
		System.out.println(response2.toString());
		client.close();	
	
		System.exit(0);
	}
	
	public void start() throws Exception
	{
		//cluster_check();
		ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
		Node node1 =(Node) context.getBean("node1");
		//create_scenario();
		
		
		
		
		
		
		// login page

		String url = "http://localhost:8090/REST/login";

		client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		// add header

		post.setHeader("User-Agent", USER_AGENT);

		List<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
		urlParameters.add(new BasicNameValuePair("userName", "admin"));
		urlParameters.add(new BasicNameValuePair("password", "Pass@123$"));

		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		HttpResponse response = client.execute(post);

		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

		Header[] h = response.getAllHeaders();
		for (int i = 0; i < h.length; i++) 
		{
			System.out.println(h[i].getName() + "\t" + h[i].getValue());
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line2 = "";
		while ((line2 = rd.readLine()) != null) {
			result.append(line2);
		}

		System.out.println(result);
		String id = "";
		for (int i = 0; i < h.length; i++)
		{
			System.out.println(h[i].getName() + "\t" + h[i].getValue());
			if (h[i].getName().equals("Set-Cookie"))
				id = h[i].getValue();

		}

		// read master
		String url2 = "http://localhost:8090/REST/config/attributes?sr=4_Read&neo=192.168.132.217%3A7474&es=456&end=false";
		System.out.println(id);
		StringTokenizer st = new StringTokenizer(id, ";");
		// System.out.println("id"+st.nextToken());
		client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url2);
		httpGet.addHeader("User-Agent", USER_AGENT);
		httpGet.addHeader("Cookie", st.nextToken());
		CloseableHttpResponse httpResponse = client.execute(httpGet);
		System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		String inputLine;
		StringBuffer response2 = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			response2.append(inputLine);
		}
		reader.close();
		// print result
		System.out.println(response2.toString());
		client.close();

		// write master
		String url3 = "http://localhost:8090/REST/config/attributes?sr=4_Read&neo=192.168.132.217%3A7477&es=456&end=false";
		System.out.println("\n\n\n\nrequest no3");
		// StringTokenizer st
		st = new StringTokenizer(id, ";");
		// System.out.println("id"+st.nextToken());
		client = HttpClients.createDefault();
		HttpPut httpPut;
		httpPut = new HttpPut(url2);
		String content = new Scanner(new File("d:\\a.txt")).useDelimiter("\\Z").next();
		System.out.println(content);

		StringEntity params = new StringEntity(content, "UTF-8");
		params.setContentType("application/json");
		httpPut.setEntity(params);
		httpPut.addHeader("User-Agent", USER_AGENT);
		httpPut.addHeader("Cookie", st.nextToken());

		// CloseableHttpResponse httpResponse
		httpResponse = client.execute(httpPut);

		System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
		// BufferedReader
		reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

		// String inputLine;
		// StringBuffer
		response2 = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			response2.append(inputLine);
		}
		reader.close();
		// print result
		System.out.println(response2.toString());
		client.close();
		
		
		
		System.exit(0);

	}
	
	public void create_scenario() throws IOException 
	{

		String START_ARBITER = "/var/lib/neo4j-enterprise-2.3.3-arbiter/bin/neo4j-arbiter start";
		String STOP_ARBITER = "/var/lib/neo4j-enterprise-2.3.3-arbiter/bin/neo4j-arbiter stop";
		String STOP_ARBITER_KILL = "cat /var/lib/neo4j-enterprise-2.3.3-arbiter/data/neo4j-arbiter.pid | xargs kill -9";

		String START_SLAVE = "/var/lib/neo4j-enterprise-2.3.3-slave/bin/neo4j start";
		String STOP_SLAVE = "/var/lib/neo4j-enterprise-2.3.3-slave/bin/neo4j stop";
		String STOP_SLAVE_KILL = "cat /var/lib/neo4j-enterprise-2.3.3-slave/data/neo4j-service.pid | xargs kill -9";

		String START_MASTER = "/var/lib/neo4j-enterprise-2.3.3/bin/neo4j start";
		String STOP_MASTER = "/var/lib/neo4j-enterprise-2.3.3/bin/neo4j stop";
		String STOP_MASTER_KILL = "cat /var/lib/neo4j-enterprise-2.3.3/data/neo4j-service.pid | xargs kill -9";

		try {
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();
			Session session = jsch.getSession("cs", "192.168.132.217", 22);
			session.setPassword("pass@123");
			session.setConfig(config);
			session.connect();
			System.out.println("Connected");
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			((ChannelExec) channel).setCommand(START_SLAVE);
			((ChannelExec) channel).setErrStream(System.err);
			channel.connect();
			InputStream in = channel.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;

			while ((line = reader.readLine()) != null) {
				System.out.println((line));
			}
			channel.disconnect();
			session.disconnect();
		} catch (JSchException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}

	}
	public void runssh() throws IOException 
	{

		String START_ARBITER = "/var/lib/neo4j-enterprise-2.3.3-arbiter/bin/neo4j-arbiter start";
		String STOP_ARBITER = "/var/lib/neo4j-enterprise-2.3.3-arbiter/bin/neo4j-arbiter stop";
		String STOP_ARBITER_KILL = "cat /var/lib/neo4j-enterprise-2.3.3-arbiter/data/neo4j-arbiter.pid | xargs kill -9";

		String START_SLAVE = "/var/lib/neo4j-enterprise-2.3.3-slave/bin/neo4j start";
		String STOP_SLAVE = "/var/lib/neo4j-enterprise-2.3.3-slave/bin/neo4j stop";
		String STOP_SLAVE_KILL = "cat /var/lib/neo4j-enterprise-2.3.3-slave/data/neo4j-service.pid | xargs kill -9";

		String START_MASTER = "/var/lib/neo4j-enterprise-2.3.3/bin/neo4j start";
		String STOP_MASTER = "/var/lib/neo4j-enterprise-2.3.3/bin/neo4j stop";
		String STOP_MASTER_KILL = "cat /var/lib/neo4j-enterprise-2.3.3/data/neo4j-service.pid | xargs kill -9";

		try {
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();
			Session session = jsch.getSession("cs", "192.168.132.217", 22);
			session.setPassword("pass@123");
			session.setConfig(config);
			session.connect();
			System.out.println("Connected");
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			((ChannelExec) channel).setCommand(START_SLAVE);
			((ChannelExec) channel).setErrStream(System.err);
			channel.connect();
			InputStream in = channel.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;

			while ((line = reader.readLine()) != null) {
				System.out.println((line));
			}
			channel.disconnect();
			session.disconnect();
		} catch (JSchException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public static void main(String[] args) throws Exception
	{

		new MainApp().start();
	}
}