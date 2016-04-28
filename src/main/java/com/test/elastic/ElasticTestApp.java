package com.test.elastic;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.hssf.record.PageBreakRecord.Break;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

public class ElasticTestApp {

	String USER_AGENT = "Mozilla/5.0";
	CloseableHttpClient client;
	ApplicationContext context;
	Session session;
	ChannelExec channel;
	StringBuffer response2;
	String content;
	ElasticNode nodes[];
	CloseableHttpResponse httpResponse;
	BufferedReader reader,br;
	 StringTokenizer st;
	 String inputLine;
	 JSONParser parser ;
	 String read_status[];
	 String write_status[];
	 boolean unknown_state=false;
	 
	 String final_command[][];
	 String final_result[][];
	 int row=0,column=0;
	 int scenarioid=0;
	 int scenarios_to_run,total_nodes;
	 int total_scenarios_in_excel;
	 Boolean access_down_nodes;
	 String commands[][];
	 
	 int total_read_success_counter=0;
	 int total_read_fail_counter=0;
	 int total_write_success_counter=0;
	 int total_write_fail_counter=0;
	 int waitTime_after_nodeUPDOWN;
	 
	 String sessionid="";
	 String current_node_status[];
	 int retry;
	 JSONObject obj=null;
	String index_name_to_create;
	
	 public void restart_cluster() throws Exception
	 {
		 	System.out.println("Restarting cluster...");
		 	// stop all nodes
		 	
			node_command(1,nodes[0],"DOWN");
			node_command(2,nodes[1],"DOWN");
			node_command(3,nodes[2],"DOWN");
			 
			// start all nodes.. Node 3 master initially
			
			node_command(1,nodes[0],"UP");
			node_command(2,nodes[1],"UP");
			node_command(3,nodes[2],"UP");
			
	 }
	 public void clear_alldata()
	 {
		
		 
		for(int m=0;m<nodes.length;m++)
		{
			String	command="sudo -S -p '' rm -rf /var/lib/"+nodes[m].getHome_path()+"/data/*";
			
			try
			{
				java.util.Properties config = new java.util.Properties();
				config.put("StrictHostKeyChecking", "no");
				JSch jsch = new JSch();
				session = jsch.getSession(nodes[m].getUser(), nodes[m].getIp(), 22);
				session.setPassword(nodes[m].getPassword());
				session.setConfig(config);
				session.connect();
				 channel = (ChannelExec) session.openChannel("exec");
				((ChannelExec) channel).setCommand(command);
				((ChannelExec) channel).setErrStream(System.err);
				channel.connect();
				InputStream in = channel.getInputStream();
				OutputStream out=channel.getOutputStream();
				
				out.write((nodes[m].getPassword()+"\n").getBytes());
				out.flush();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println((line));
				}
				channel.disconnect();
				session.disconnect();
				
				System.out.println("Clearing data from node"+(m+1));
				Thread.sleep(1000);
			}
			catch (JSchException e) 
			{
				System.out.println("exception happened - here's what I know: ");
				e.printStackTrace();
				channel.disconnect();
				session.disconnect();
				System.exit(-1);
			}
			catch(Exception e)
			{
				System.out.println("exception happened - here's what I know: ");
				e.printStackTrace();
				channel.disconnect();
				session.disconnect();
				System.exit(-1);
				
			}
		}
		
		
		
	 }
	 public void create_index()
	 {
		 HttpPut httpPut=null;
			/*CloseableHttpResponse httpResponse=null;
			BufferedReader reader=null;
			StringBuffer response2=null;
			JSONObject obj=null;*/
		 
		
			try
			{
				//create index myindex
				String url2 = "http://"+nodes[0].getIp()+":"+nodes[0].getPort()+"/"+index_name_to_create;
				
				
				client = HttpClients.createDefault();
				httpPut = new HttpPut(url2);
				httpPut.addHeader("User-Agent", USER_AGENT);
				httpPut.addHeader("Cookie", sessionid);
				 httpResponse = client.execute(httpPut);
				if(httpResponse.getStatusLine().getStatusCode()==200)   
					System.out.print("Index created.\n");
				else
					if(httpResponse.getStatusLine().getStatusCode()==500)
						System.out.print("Index creation failed\n");
					else
						System.out.print(" Unknown index creation status\t"+ httpResponse.getStatusLine().getStatusCode());
				 
				//		+ httpResponse.getStatusLine().getStatusCode());
				 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				String inputLine;
				 response2 = new StringBuffer();
				while ((inputLine = reader.readLine()) != null) {
					response2.append(inputLine);
				}
				reader.close();
				//System.out.println(response2);
				
				
				//set replica
				 url2 = "http://"+nodes[0].getIp()+":"+nodes[0].getPort()+"/"+index_name_to_create+"/_settings";
				JSONObject o=(JSONObject) parser.parse("{\"number_of_replicas\": 2}");
				
				StringEntity params = new StringEntity(o.toJSONString(), "UTF-8");
				params.setContentType("application/json");
				client = HttpClients.createDefault();
				httpPut = new HttpPut(url2);
				httpPut.setEntity(params);
				httpPut.addHeader("User-Agent", USER_AGENT);
				httpPut.addHeader("Cookie", sessionid);
				 httpResponse = client.execute(httpPut);
				if(httpResponse.getStatusLine().getStatusCode()==200)   
					System.out.print("Replica set.\n");
				else
					if(httpResponse.getStatusLine().getStatusCode()==500)
						System.out.print("Replica setting failed\n");
					else
						System.out.print(" Unknown replica creation status\t"+ httpResponse.getStatusLine().getStatusCode());
				 
				//		+ httpResponse.getStatusLine().getStatusCode());
				 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				//String inputLine;
				 response2 = new StringBuffer();
				while ((inputLine = reader.readLine()) != null) {
					response2.append(inputLine);
				}
				reader.close();
				//System.out.println(response2);
			}
			catch(Exception e)
			{
				System.out.println("index creation  failed" +e.getMessage()+" "+e.getCause()+" "+e.getClass());
				e.printStackTrace();
			}
	 }

	public void start()
	{
		try
		{
		Date date1 = new Date();		
		configure(); 
		read_excel();
		clear_alldata();
		restart_cluster();
		
		create_index();
		
		 cluster_check();
		 
		 int nodeno=0;
		 scenarioid=1;
		 
		 
		 //Main loop.. Iterate over test scenarios
		 for(int i=1;i<=scenarios_to_run;i++)  
	    {
			 try{
				 
			 
				 System.out.println("loop starts");
			 	nodeno=Integer.parseInt(final_command[i][0]);
	    		node_command(nodeno,nodes[nodeno-1],final_command[i][1]);
	    		cluster_check();		
	    		System.out.println("now read write");
	    		read_write();
	    		
	    		System.out.println("now read write done");
	    		
	    		final_result[scenarioid][4]=commands[scenarioid][0];
	    		final_result[scenarioid][5]=commands[scenarioid][1];
	    		final_result[scenarioid][6]=commands[scenarioid][2];
	    		
	    		System.out.println("now print done");
	    		
	    		scenarioid++;
			 }
			 catch(Exception e)
			 {
				 System.out.println("Loop exception happnened. "+e.getCause());
			 }
	    }
		 
		 //Print final results
		 System.out.println("Final Result as below");
		 System.out.println("---------------------------------------------------------------------------------------------");
		 System.out.println("Sr No\tNode1\tNode2\tNode3\tHealth\tNode1\tNode2\tNode3\tMasterNode\tComments");
		 System.out.println("---------------------------------------------------------------------------------------------\n");
		for(int i=1;i<=scenarios_to_run;i++)
		{
			System.out.print(i+"\t");
			System.out.print(final_result[i][0]+"\t");
			System.out.print(final_result[i][1]+"\t");
			System.out.print(final_result[i][2]+"\t");
			System.out.print(final_result[i][3]+"\t");
			
			
			System.out.print(final_result[i][4]+"\t");
			System.out.print(final_result[i][5]+"\t");
			System.out.print(final_result[i][6]+"\t");
			System.out.print(final_result[i][7]+"\t\t");
			System.out.print(final_result[i][8]+"\t\n");
		}
		System.out.println("---------------------------------------------------------------------------------------------\n\n");
		Date date2 = new Date();
		System.out.println("\n Time elapsed in Minutes =>" +((date2.getTime()-date1.getTime())/60000));
		System.out.println();
		System.out.println("total Successful reads  : "+ total_read_success_counter);
		System.out.println("total Failed reads      : "+ total_read_fail_counter);
		System.out.println("total Successful writes : "+ total_write_success_counter);
		System.out.println("total Failed writes     : "+ total_write_fail_counter);
		
		
		
		final_read();
		
		
		
		writexcel();
		
		}
		catch(Exception e)
		{
			System.out.println("final exc"+e.getCause()+" "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void node_command(int nodeid,ElasticNode node, String updown) throws IOException 
	{
		String command="";
		if (updown.equals("DOWN"))
		{
			//command="sudo -S -p '' cat /var/lib/"+node.getHome_path()+"/data/neo4j-service.pid | sudo -S -p '' xargs kill -9";
			command="sudo -S -p '' fuser -k "+node.getPort()+"/tcp";
		}
		else
		if (updown.equals("UP"))
		{
			//command="sudo -S -p '' /var/lib/"+node.getHome_path()+"/bin/neo4j start";
			command="sudo -S -p '' /var/lib/"+node.getHome_path()+"/bin/elasticsearch -Des.insecure.allow.root=true -d";
		}
		
		try
		{
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();
			session = jsch.getSession(node.getUser(), node.getIp(), 22);
			session.setPassword(node.getPassword());
			session.setConfig(config);
			//System.out.println(node.getUser()+ node.getIp()+ Integer.parseInt(node.getPort()));
			session.connect();
			//System.out.println("Connected");
			 channel = (ChannelExec) session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			((ChannelExec) channel).setErrStream(System.err);
			channel.connect();
			InputStream in = channel.getInputStream();
			OutputStream out=channel.getOutputStream();
			
			out.write((node.getPassword()+"\n").getBytes());
			out.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			System.out.println("\n Running scenario "+scenarioid+". Making Node"+nodeid+" "+updown);
			while ((line = reader.readLine()) != null) {
				//System.out.println((line));
			}
			channel.disconnect();
			session.disconnect();
			
			current_node_status[nodeid-1]=updown;
			Thread.sleep(waitTime_after_nodeUPDOWN);
		}
		catch (JSchException e) 
		{
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			channel.disconnect();
			session.disconnect();
			System.exit(-1);
		}
		catch(Exception e)
		{
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			channel.disconnect();
			session.disconnect();
			System.exit(-1);
			
		}

	}
	public static void main(String[] args) throws Exception
	{
		new ElasticTestApp().start();
	}
	public void read_write()throws Exception
	{
		HttpGet httpGet=null;
		CloseableHttpResponse httpResponse=null;
		BufferedReader reader=null;
		StringBuffer response2=null;
		
		// post create new index   http://192.168.132.217:9201/myindex/    index already exists
		
		// get search     http://192.168.132.217:9201/myindex/mydoc/_search/                      
		//create new doc     http://192.168.132.217:9201/myindex/mydoc/  
				//   and provide data
		
		
		// get  count   http://192.168.132.217:9201/_count?pretty/   
		// get search all  http://192.168.132.217:9201//myindex/_search/   
			
		for(int i=0;i<nodes.length;i++)
		{
			
			try
			{
				// read Node
				String url2 = "http://"+nodes[i].getIp()+":"+nodes[i].getPort()+"/"+index_name_to_create+"/mydoc/_search?size=100";
				
				client = HttpClients.createDefault();
				 httpGet = new HttpGet(url2);
				httpGet.addHeader("User-Agent", USER_AGENT);
				httpGet.addHeader("Cookie", sessionid);
				 httpResponse = client.execute(httpGet);
				System.out.print("\nRead Node"+(i+1)+" Response Status : "+httpResponse.getStatusLine().getStatusCode());
				
				if(httpResponse.getStatusLine().getStatusCode()==200)   
				{
					System.out.print(" SUCCESS\n");
					read_status[i]="SUCCESS";
					total_read_success_counter++;
				}
				else
					if(httpResponse.getStatusLine().getStatusCode()==500)
					{
						System.out.print(" FAILURE\n");
						 read_status[i]="FAILURE";
						 total_read_fail_counter++;
					}
					else
						System.out.print(" Unknown read write status\n");
						
				//		+ httpResponse.getStatusLine().getStatusCode());
				 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				String inputLine;
				 response2 = new StringBuffer();
				while ((inputLine = reader.readLine()) != null) {
					response2.append(inputLine);
				}
				reader.close();
				httpGet.reset();
				
				
				//Readjson response
				 obj = (JSONObject)parser.parse(response2.toString());
				JSONObject subobj=(JSONObject) obj.get("hits");
				JSONArray array= (JSONArray) subobj.get("hits");
				System.out.print("Total labels "+array.size()+". Values are => ");
				for(int j=0;j<array.size();j++)
				{
					JSONObject element=(JSONObject) array.get(j);
					JSONObject subelement =(JSONObject) element.get("_source");
					System.out.print(subelement.get("label")+",");
				}
			}
			catch(HttpHostConnectException e)
			{
				System.out.println("Read failed Node"+(i+1) );
				 read_status[i]="FAILURE";
				 total_read_fail_counter++;
				e.printStackTrace();
			}
			catch(Exception e)
			{
				System.out.println("Read failed" +e.getMessage()+"  "+e.getClass());
				
				e.printStackTrace();
			}

			
			/*
			if( obj.get("status").equals("SUCCESS"))
			 {
					
				read_status[i]=(String) obj.get("status");
				 //code to read actual data
				JSONArray array=(JSONArray) obj.get("response");
				 //System.out.println("values read are => "+ array.size());
				System.out.print("Total : "+array.size()+". label values are =>");
				 for(int k=0;k<array.size();k++)
				 {
					 JSONObject objj=((JSONObject)array.get(k));
					 System.out.print(objj.get("label")+",");
				 }
				 total_read_success_counter++;
			 }
			 else
			 if( obj.get("status").equals("FAILURE"))
			 {	 
				 read_status[i]=(String) obj.get("status");
				 total_read_fail_counter++;
			 }*/
			Thread.sleep(2000);
			try
			{
				// write Node
				
				String url3 = "http://"+nodes[i].getIp()+":"+nodes[i].getPort()+"/"+index_name_to_create+"/mydoc/";
				HttpPost httpPut;
				httpPut = new HttpPost(url3);
				
				JSONObject o=(JSONObject) parser.parse("{}");
				//o.remove("label");
				o.put("label", "A"+scenarioid);
				
				StringEntity params = new StringEntity(o.toJSONString(), "UTF-8");
				params.setContentType("application/json");
				httpPut.setEntity(params);
				httpPut.addHeader("User-Agent", USER_AGENT);
				//httpPut.addHeader("Cookie", sessionid);
				httpResponse = client.execute(httpPut);
				System.out.print("\n\nWrite Node"+(i+1)+" Response Status: "+httpResponse.getStatusLine().getStatusCode());
				 
				if(httpResponse.getStatusLine().getStatusCode()==201)   
				{
					System.out.print(" SUCCESS\n");
					 write_status[i]="SUCCESS";
					 total_write_success_counter++;
					 System.out.println("Write label is => A"+scenarioid);
					
				}
				else
					if(httpResponse.getStatusLine().getStatusCode()==500)
					{
					
						System.out.print(" FAILURE\n");
						 write_status[i]="FAILURE";
						 total_write_fail_counter++;
					}
					else
						System.out.print(" Unknown  write status\n");
				
				
				reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
				
				// StringBuffer
				response2 = new StringBuffer();
				while ((inputLine = reader.readLine()) != null) {
					response2.append(inputLine);
				}
				reader.close();
				// print result
				
				
				//System.out.println("\nwrite response JSON is => "+response2.toString());
				 obj = (JSONObject)parser.parse(response2.toString());
		
				 
			 //System.out.println("\n\n"+obj.get("status"));   
			 //System.out.println("write response JSON val is => "+obj.get("status")+"\t"+obj.get("label"));
				
				 /*
				 if( obj.get("status").equals("SUCCESS"))
				 {
					 write_status[i]=(String) obj.get("status");
					 System.out.println("label written is => A"+scenarioid);
					 Thread.sleep(2000);
					 total_write_success_counter++;
				 }
				 else
				 if( obj.get("status").equals("FAILURE"))
				 {	 
					 write_status[i]=(String) obj.get("status");
					 total_write_fail_counter++;
				 }
				 else
				 {
					 System.out.println("Read/write status othen than SUCCESS/FAILURE. Please check");
					 write_status[i]=(String) obj.get("status");
				 }*/
			 
				httpPut.reset();
				client.close();
				
			}
			catch (HttpHostConnectException e)
			{
				System.out.println("\n Write failed Node"+(i+1));
				 write_status[i]="FAILURE";
				 total_write_fail_counter++;
				e.printStackTrace();
			}
			catch (Exception e)
			{
				System.out.println("\n\nwrite failure\n"+e.getClass()+" "+e.getClass());
				e.printStackTrace();
			}
		}
			System.out.println("\n\n--------Scenario "+scenarioid+" Result-----------");
			for(int i=0;i<nodes.length;i++)
			{
				System.out.print("Node"+(i+1)+"\t"+read_status[i]);
				System.out.print("\t"+write_status[i]+"\n");
				
				if(read_status[i].equals("SUCCESS") && write_status[i].equals("SUCCESS"))
				{
					final_result[scenarioid][i]="PASS";
					//System.out.println("ajit => "+final_result[scenarioid][i]);
				}
				if(read_status[i].equals("FAILURE") && write_status[i].equals("FAILURE"))
				{
					final_result[scenarioid][i]="FAIL";
				}
				
			}
			System.out.println("--------------------------------------\n\n");
		
	}
	public void cluster_check() throws Exception
	{
		Thread.sleep(waitTime_after_nodeUPDOWN); 
		int upnode=0;
		String node_status[][];
		node_status=new String[nodes.length+1][2];
		
		System.out.println("\n---------- Cluster status -------------");
		
		for(int i=0;i<nodes.length;i++)
		{
			//System.out.print(" "+current_node_status[i]);
			if (current_node_status[i].equals("UP"))
				upnode=i+1;
			
		}
		//System.out.println("upnode is "+upnode);
		
		HttpGet httpGet;
		//int upnode=0;
			
			//for(int i=0;i<nodes.length;i++)
			if(upnode!=0)
			{
				try
				{
					//get node list
					String LIST_NODES = "http://"+nodes[upnode-1].getIp()+":"+nodes[upnode-1].getPort()+"/_cat/nodes?v&h=master";
					client = HttpClients.createDefault();
					 httpGet = new HttpGet(LIST_NODES);
					httpGet.addHeader("User-Agent", USER_AGENT);
					CloseableHttpResponse httpResponse = client.execute(httpGet);
					//System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
					BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
					String inputLine;
					 response2 = new StringBuffer();
					 int row=0;
					while ((inputLine = reader.readLine()) != null) 
					{
						response2.append(inputLine);
						//System.out.print("\ntokens are   "+inputLine);
						node_status[row][0]=inputLine;
								row++;

					}
					reader.close();
					
					
					
					//get master list
					 LIST_NODES = "http://"+nodes[upnode-1].getIp()+":"+nodes[upnode-1].getPort()+"/_cat/nodes?v&h=name";
					client = HttpClients.createDefault();
					 httpGet = new HttpGet(LIST_NODES);
					httpGet.addHeader("User-Agent", USER_AGENT);
					 httpResponse = client.execute(httpGet);
					//System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
					 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
					// inputLine;
					 response2 = new StringBuffer();
					 row=0;
					 
					 //System.out.println("\nNodes status is \n");
					while ((inputLine = reader.readLine()) != null) 
					{
						response2.append(inputLine);
						node_status[row][1]=inputLine;
						row++;
					}
					reader.close();
					//System.out.println("node length is  "+node_status.length);
					
					for(int i=0;i<nodes.length+1;i++)
						System.out.println(node_status[i][0]+"  "+node_status[i][1]);
					
					//identify master for this scenario
					int r=0;
					//System.out.println("identify master");
					for(r=1;r<nodes.length+1;r++)
					{
						//System.out.println("under for"+node_status[r][0].trim());
						if(node_status[r][0].trim().equals("*"))
						{
							//System.out.println("under if");
							final_result[scenarioid][7]=node_status[r][1];
							final_result[scenarioid][8]="";
						}
					}
					
				}
				catch(HttpHostConnectException e)
				{
					//System.out.println("exception happened - here's what I know: ");
					e.printStackTrace();
					//System.out.println("Node"+(i+1)+" down");

				}
				catch(Exception e)
				{
					System.out.println("exception happened - here's what I know: "+e.getMessage()+"  "+e.getCause()+" "+e.getClass());
					e.printStackTrace();
				}

			}
			
			try
			{
			//Get health
				if(upnode!=0)
				{
					//System.out.println("\nCluster Health is \n");
					String CHECK_ALL = "http://"+nodes[upnode-1].getIp()+":"+nodes[upnode-1].getPort()+"/_cluster/health?pretty=true";
					//   list of node      localhost:9200/_cat/nodes?v
					
					 httpGet = new HttpGet(CHECK_ALL);
					
					httpGet.addHeader("User-Agent", USER_AGENT);
					 httpResponse = client.execute(httpGet);
					//System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
					
					 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
					
					 response2 = new StringBuffer();
					while ((inputLine = reader.readLine()) != null) {
						response2.append(inputLine);
						//System.out.println(inputLine);
					}
					reader.close();
				}
				// print result
				//System.out.println(response2);
				
				//print cluster health
				obj = (JSONObject)parser.parse(response2.toString());
				String status=(String) obj.get("status");
				long no_nodes= (Long) obj.get("number_of_nodes");
				System.out.println("\nCluster Health => "+status );
				final_result[scenarioid][3]=status;
				System.out.println("No of nodes Active => "+no_nodes );
			
			}
			catch(HttpHostConnectException e)
			{
				//System.out.println("exception happened - here's what I know: ");
				e.printStackTrace();

			}
			catch(Exception e)
			{
				System.out.println("exception happened - here's what I know: "+e.getMessage());
				e.printStackTrace();
			}
		
		
		
		System.out.println("--------------------------------------");


	}
	public void configure()throws Exception
	{
		parser = new JSONParser(); 
		context = new ClassPathXmlApplicationContext("elastic\\beans.xml");
		  content = new Scanner(new File("src\\main\\resources\\elastic\\jsonwrite.txt")).useDelimiter("\\Z").next();
						  
		 //context variables
		  total_scenarios_in_excel=(int) context.getBean("total_scenarios_in_excel");
		 scenarios_to_run=(int) context.getBean("scenarios_to_run");
		 total_nodes=(int) context.getBean("total_nodes");
		 access_down_nodes=(Boolean) context.getBean("access_down_nodes");
		 waitTime_after_nodeUPDOWN=(int) context.getBean("waitTime_after_nodeUPDOWN");
		 index_name_to_create=(String) context.getBean("index_name_to_create");
		 //other vars
		 current_node_status=new String[3];
		 read_status=new String[3];
		 write_status=new String[3];
			
		 nodes=new ElasticNode[total_nodes];		
		 for(int i=0;i<total_nodes;i++)
		 {
			 nodes[i] =(ElasticNode) context.getBean("node"+(i+1));
		 }
		 
		 final_result=new String[scenarios_to_run+1][9];              //3+1
		
	}
	public void writexcel()
	{
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("sheet");
		 
		Map<String, Object[]> data = new LinkedHashMap<String, Object[]>();
		
		data.put("1",new Object[] {"Sr No","Node1","Node2","Node3","Health","Node1","Node2","Node3","MasterNode","Comments"} );
		for(int i=1;i<=scenarios_to_run;i++)
		{
			data.put((i+1)+"", new Object[] {i+"",final_result[i][0],final_result[i][1],final_result[i][2],final_result[i][3],final_result[i][4],final_result[i][5],final_result[i][6], final_result[i][7],final_result[i][8]});
		}
		

		Set<String> keyset = data.keySet();
		int rownum = 0;
		for (String key : keyset) {
		    Row row = sheet.createRow(rownum++);
		    Object [] objArr = data.get(key);
		    int cellnum = 0;
		    for (Object obj : objArr) {
		        Cell cell = row.createCell(cellnum++);
		        if(obj instanceof Date) 
		            cell.setCellValue((Date)obj);
		        else if(obj instanceof Boolean)
		            cell.setCellValue((Boolean)obj);
		        else if(obj instanceof String)
		            cell.setCellValue((String)obj);
		        else if(obj instanceof Double)
		            cell.setCellValue((Double)obj);
		    }
		}
		 
		try {
			Date date3=new Date();
			String tt=date3.toString().replaceAll(":",".");
			File file=new File("src\\main\\resources\\elastic\\output\\Output "+tt+".xls");
			Boolean b = file.createNewFile();
			if(b)
				System.out.println("Output Excel created");
			else
				System.out.println("Excel creation failed");
		    FileOutputStream out = 
		            new FileOutputStream(file);
		    workbook.write(out);
		    out.close();
		    System.out.println("\n\nOutput Excel written successfully..");
		     
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}	catch (Exception e){
			e.printStackTrace();
		}
		
	}
	public void final_read() throws Exception
	{
		Thread.sleep(5000);
		System.out.println("Final read..Querying node 1");
		HttpGet httpGet=null;
		CloseableHttpResponse httpResponse=null;
		BufferedReader reader=null;
		StringBuffer response2=null;
		try
		{
			// read Node
			String url2 = "http://"+nodes[0].getIp()+":"+nodes[0].getPort()+"/"+index_name_to_create+"/mydoc/_search?size=500";
			
			client = HttpClients.createDefault();
			 httpGet = new HttpGet(url2);
			httpGet.addHeader("User-Agent", USER_AGENT);
			httpGet.addHeader("Cookie", sessionid);
			 httpResponse = client.execute(httpGet);
			System.out.print("\nRead Node"+(1)+" Response Status : "+httpResponse.getStatusLine().getStatusCode());
			
			if(httpResponse.getStatusLine().getStatusCode()==200)   
			{
				System.out.print(" SUCCESS\n");
			}
			else
				if(httpResponse.getStatusLine().getStatusCode()==500)
				{
					System.out.print(" FAILURE\n");
				}
				else
					System.out.print(" Unknown read write status\n");
					
			//		+ httpResponse.getStatusLine().getStatusCode());
			 reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String inputLine;
			 response2 = new StringBuffer();
			while ((inputLine = reader.readLine()) != null) {
				response2.append(inputLine);
			}
			reader.close();
			httpGet.reset();
			
			
			//Readjson response
			 obj = (JSONObject)parser.parse(response2.toString());
			JSONObject subobj=(JSONObject) obj.get("hits");
			JSONArray array= (JSONArray) subobj.get("hits");
			System.out.print("\n\nFinal labels "+array.size()+". Values are => ");
			for(int j=0;j<array.size();j++)
			{
				JSONObject element=(JSONObject) array.get(j);
				JSONObject subelement =(JSONObject) element.get("_source");
				System.out.print(subelement.get("label")+",");
			}
		}
		catch(HttpHostConnectException e)
		{
			System.out.println("Read failed Node"+(1) );
			e.printStackTrace();
		}
		catch(Exception e)
		{
			System.out.println("Read failed" +e.getMessage()+"  "+e.getClass());
			e.printStackTrace();
		}
		
	}
	 public void read_excel()
	 {
		  commands=new String[total_scenarios_in_excel+1][3];
		  final_command=new String[total_scenarios_in_excel+1][2];
		 int r=0,c=0;
		  
		 System.out.println("Reading Excel commands file");
		    String line;
		    
		    commands[0][0]="UP";
		    commands[0][1]="UP";
		    commands[0][2]="UP";
		    row=1;
		 try
	        {
	            FileInputStream file = new FileInputStream(new File("src\\main\\resources\\elastic\\excel.xls"));
	 
	            //Create Workbook instance holding reference to .xlsx file
	            HSSFWorkbook workbook = new HSSFWorkbook(file);
	 
	            //Get first/desired sheet from the workbook
	            HSSFSheet sheet = workbook.getSheetAt(0);
	 
	            //Iterate through each rows one by one
	            Iterator<Row> rowIterator = sheet.iterator();
	            
	            while (rowIterator.hasNext()) 
	            {
	                Row rowit = rowIterator.next();
	                //For each row, iterate through all the columns
	                Iterator<Cell> cellIterator = rowit.cellIterator();
	 		       column=0;
	                while (cellIterator.hasNext()) 
	                {
	                    Cell cell = cellIterator.next();
	                    //Check the cell type and format accordingly
	                    switch (cell.getCellType()) 
	                    {
	                        case Cell.CELL_TYPE_NUMERIC:
	                            System.out.print(cell.getNumericCellValue() + "\t");
	                            break;
	                        case Cell.CELL_TYPE_STRING:
	                            //System.out.print(cell.getStringCellValue() + "\t");
	                            
	                            commands[row][column]=cell.getStringCellValue();
	         		    	   
	         		    	   //COMAPRE LOGIC
	         		    	   if(!commands[row][column].equals(commands[row-1][column]))
	         		    	   {
	         		    		    final_command[row][0]=(column+1)+"";
	         		    		    final_command[row][1]=commands[row][column];
	         		    		   
	         		    	   }
	         		    	   else
	         		    	   {
	         		    		   
	         		    	   }
	         		    	   
	         		    	  column++;
	                            break;
	                    }
	                }
	                row++;
	                //System.out.println("");
	            }
	            file.close();
	            /*for(int i=0;i<235;i++)
	            	for(int j=0;j<2;j++)
	            		System.out.println(final_command[i][j]);*/
	        } 
	        catch (Exception e) 
	        {
	            e.printStackTrace();
	        }
	 }
	 
}
