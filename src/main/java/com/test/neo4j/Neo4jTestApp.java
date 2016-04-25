package com.my;

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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

public class MainApp {

	String USER_AGENT = "Mozilla/5.0";
	CloseableHttpClient client;
	ApplicationContext context;
	Session session;
	ChannelExec channel;
	StringBuffer response2;
	String content;
	Node nodes[];
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
	 //Boolean downflag;
	 int retry;
	 
	 public void readexcel()
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
	            FileInputStream file = new FileInputStream(new File("src\\main\\resources\\excel.xls"));
	 
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
	         		    	   
	         		    	   column++;
	                            
	                            
	                            break;
	                    }
	                }
	                row++;
	                //System.out.println("");
	            }
	            file.close();
	            for(int i=0;i<235;i++)
	            	for(int j=0;j<2;j++)
	            		System.out.println(final_command[i][j]);
	        } 
	        catch (Exception e) 
	        {
	            e.printStackTrace();
	        }
	 }
	 
	 public void restart_cluster() throws Exception
	 {
		 	System.out.println("Restarting cluster...");
		 	// stop all nodes
		 	node_command(3,nodes[2],"DOWN");
			node_command(1,nodes[0],"DOWN");
			node_command(2,nodes[1],"DOWN");
			
			 
			// start all nodes.. Node 3 master initially
			node_command(3,nodes[2],"UP");
			node_command(1,nodes[0],"UP");
			node_command(2,nodes[1],"UP");
			
	 }
	 public void clear_alldata()
	 {
		for(int m=0;m<nodes.length;m++)
		{
			String	command="sudo -S -p '' rm -rf /var/lib/"+nodes[m].getHome_path()+"/data/graph.db";
			
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

	public void start() throws Exception
	{
		try
		{
		Date date1 = new Date();		
		configure(); 
		readexcel();
		//read_scenarios();
		clear_alldata();
		
		
		cluster_check(0,null);
		 restart_cluster();
		 cluster_check(0,null);
		 
		 int nodeno=0;
		 scenarioid=1;
		 
		 login();
		 
		 //Main loop.. Iterate over test scenarios
		 for(int i=1;i<=scenarios_to_run;i++)  
	    {
			 	//scenario id
			 System.out.println("scen id"+ scenarioid);
	    		
	    		System.out.println(final_command[i][0]+"scen id/.   "+ nodeno);
	    		
	    		nodeno=Integer.parseInt(final_command[i][0]);
	    		node_command(nodeno,nodes[nodeno-1],final_command[i][1]);
	    		retry=(int) context.getBean("retry");
	    		cluster_check(nodeno,final_command[i][1]);		
	    		read_write();
	    		
	    		final_result[scenarioid][3]=commands[scenarioid][0];
	    		final_result[scenarioid][4]=commands[scenarioid][1];
	    		final_result[scenarioid][5]=commands[scenarioid][2];
	    		
	    		scenarioid++;
	    }
		 
		 //Print final results
		 System.out.println("Final Result as below");
		 System.out.println("---------------------------------------------------------------------------------------------");
		 System.out.println("Sr No\tNode1\tNode2\tNode3\t\tNode1\tNode2\tNode3\t\tMasterNode\tComments");
		 System.out.println("---------------------------------------------------------------------------------------------\n");
		for(int i=1;i<=scenarios_to_run;i++)
		{
			System.out.print(i+"\t");
			System.out.print(final_result[i][0]+"\t");
			System.out.print(final_result[i][1]+"\t");
			System.out.print(final_result[i][2]+"\t\t");
			System.out.print(final_result[i][3]+"\t");
			System.out.print(final_result[i][4]+"\t");
			System.out.print(final_result[i][5]+"\t\t");
			System.out.print(final_result[i][6]+"\t\t");
			System.out.print(final_result[i][7]+"\t\n");
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
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public void node_command(int nodeid,Node node, String updown) throws IOException 
	{
		String command="";
		if (updown.equals("DOWN"))
		{
			//command="cat /var/lib/"+node.getHome_path()+"/data/neo4j-service.pid | xargs kill -9";
			command="sudo -S -p '' cat /var/lib/"+node.getHome_path()+"/data/neo4j-service.pid | sudo -S -p '' xargs kill -9";
		}
		else
		if (updown.equals("UP"))
		{
			command="sudo -S -p '' /var/lib/"+node.getHome_path()+"/bin/neo4j start";
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
				System.out.println((line));
			}
			channel.disconnect();
			session.disconnect();
			
			
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
		new MainApp().start();
	}
	public void login()throws Exception
	{
	// login page
		try{
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
			int res=response.getStatusLine().getStatusCode();
			if(res==302)
			{
				System.out.println("Login successful. Now redirecting\n");
			
			}
			System.out.println("ad");
			Header[] h = response.getAllHeaders();
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line2 = "";
			while ((line2 = rd.readLine()) != null) {
				result.append(line2);
			}
			System.out.println("ac");
			// print login response result if required
			//System.out.println(result);
			
			//extract session ID
			String id = "";
			for (int i = 0; i < h.length; i++)
			{
				//System.out.println(h[i].getName() + "\t" + h[i].getValue());
				if (h[i].getName().equals("Set-Cookie"))
					id = h[i].getValue();
			}
			StringTokenizer st = new StringTokenizer(id, ";");
			 sessionid=st.nextToken();
			 System.out.println("ab");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void read_write()throws Exception
	{
		  //downflag=false;
			
		for(int i=0;i<nodes.length;i++)
		{
			/*if(!access_down_nodes && current_node_status[i].equals("DOWN"))
			break;*/
			
			// read Node
			String url2 = "http://localhost:8090/REST/config/attributes?sr=4_Read&neo="+nodes[i].getIp()+"%3A"+nodes[i].getPort()+"&es=456&end=false";
			client = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(url2);
			httpGet.addHeader("User-Agent", USER_AGENT);
			httpGet.addHeader("Cookie", sessionid);
			CloseableHttpResponse httpResponse = client.execute(httpGet);
			System.out.print("\nRead Node"+(i+1)+" Response Status : ");
			
			if(httpResponse.getStatusLine().getStatusCode()==200)   
				System.out.print("SUCCESS\n");
			else
				if(httpResponse.getStatusLine().getStatusCode()==500)
					System.out.print("FAILURE\n");
				else
					System.out.print("Unknown read write status\n");
					
			//		+ httpResponse.getStatusLine().getStatusCode());
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String inputLine;
			StringBuffer response2 = new StringBuffer();
			while ((inputLine = reader.readLine()) != null) {
				response2.append(inputLine);
			}
			reader.close();
			// print result
			//System.out.println("read response JSON is => "+response2.toString());
			
			httpGet.reset();
			JSONObject obj = (JSONObject)parser.parse(response2.toString());
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
			 }
			Thread.sleep(2000);
		
				
				
			// write Node
			String url3 = "http://localhost:8090/REST/config/attributes?sr=4_Read&neo="+nodes[i].getIp()+"%3A"+nodes[i].getPort()+"&es=456&end=false";
			//client = HttpClients.createDefault();
			HttpPut httpPut;
			httpPut = new HttpPut(url3);
			
			JSONObject o=(JSONObject) parser.parse(content);
			o.remove("label");
			o.put("label", "A"+scenarioid);
			
			o.toJSONString();
			//System.out.println("put json is ===> " +o.toJSONString());
			
			StringEntity params = new StringEntity(o.toJSONString(), "UTF-8");
			//StringEntity params = new StringEntity(content, "UTF-8");
			params.setContentType("application/json");
			httpPut.setEntity(params);
			httpPut.addHeader("User-Agent", USER_AGENT);
			httpPut.addHeader("Cookie", sessionid);
			httpResponse = client.execute(httpPut);
			System.out.print("\nWrite Node"+(i+1)+" Response Status: ");
			 
			if(httpResponse.getStatusLine().getStatusCode()==200)   
				System.out.print("SUCCESS\n");
			else
				if(httpResponse.getStatusLine().getStatusCode()==500)
					System.out.print("FAILURE\n");
				else
					System.out.print("Unknown read write status\n");
			
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			
			// StringBuffer
			response2 = new StringBuffer();
			while ((inputLine = reader.readLine()) != null) {
				response2.append(inputLine);
			}
			reader.close();
			// print result
			//System.out.println("write response JSON is => "+response2.toString());
			 obj = (JSONObject)parser.parse(response2.toString());
		
			 //System.out.println("\n\n"+obj.get("status"));   
			 //System.out.println("write response JSON val is => "+obj.get("status")+"\t"+obj.get("label"));
				
			 
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
			 }
			 
			httpPut.reset();
			client.close();
			}
			System.out.println("\n\n--------Scenario "+scenarioid+" Result-----------");
			for(int i=0;i<3;i++)
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
	public void cluster_check(int nodeno, String command) throws Exception
	{
		Thread.sleep(12000); 
		
		System.out.println("---------- Cluster status -------------");
		for(int i=0;i<nodes.length;i++)
		{
			try
			{
			String CHECK_ALL = "http://"+nodes[i].getIp()+":"+nodes[i].getPort()+"/db/manage/server/ha/available";
			client = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(CHECK_ALL);
			
			httpGet.addHeader(BasicScheme.authenticate(
					 new UsernamePasswordCredentials("neo4j","pass123"),
					 "UTF-8", false));
			httpGet.addHeader("User-Agent", USER_AGENT);
			//httpGet.addHeader("Cookie", st.nextToken());
			CloseableHttpResponse httpResponse = client.execute(httpGet);
			//System.out.println("GET Response Status:: " + httpResponse.getStatusLine().getStatusCode());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String inputLine;
			 response2 = new StringBuffer();
			while ((inputLine = reader.readLine()) != null) {
				response2.append(inputLine);
			}
			reader.close();
			// print result
			String res=response2.toString();
			System.out.print("Node"+(i+1)+"\t");
			System.out.print(res);
			
			if(res.equals("master"))
				final_result[scenarioid][6]="Node"+(i+1); 
			if(res.equals("UNKNOWN"))
			{
					unknown_state=true;
					//current_node_status[i]="UNKNOWN";
			}
			System.out.println("\tUP");
			//final_result[scenarioid][]
			current_node_status[i]="UP";
			client.close();
			
			}
			catch(HttpHostConnectException e)
			{
				//System.out.println("exception happened - here's what I know: ");
				//e.printStackTrace();
				System.out.print("Node"+(i+1)+"\t");
				//System.out.print(response2.toString());
				System.out.println("\tDOWN");
				current_node_status[i]="DOWN";
			}
			catch(Exception e)
			{
				System.out.println("exception happened - here's what I know: ");
				e.printStackTrace();
			}		
		}		
		System.out.println("--------------------------------------");
		final_result[scenarioid][7]="";
		if(unknown_state)
		{
			System.out.println("\nUnknown state reached..");
			final_result[scenarioid][6]="No Master";
			final_result[scenarioid][7]="INCONSISTENT STATE - Unresponsive";
			unknown_state=false;
		}
		
		//check if cluster is stable as per last node command
		/*if( nodeno!=0)
		{
			if((! current_node_status[nodeno].equals(command)) && ( retry!=0))
			{
				System.out.println("retrying ... "+retry);
				cluster_check(nodeno, command);
				retry--;
			}
				
		}*/
	}
	public void configure()throws Exception
	{
		parser = new JSONParser(); 
		context = new ClassPathXmlApplicationContext("beans.xml");
		 br = new BufferedReader(new FileReader(new File("src\\main\\resources\\commands.txt"))); 
		  content = new Scanner(new File("src\\main\\resources\\jsonwrite.txt")).useDelimiter("\\Z").next();
						  
		 //context variables
		  total_scenarios_in_excel=(int) context.getBean("total_scenarios_in_excel");
		 scenarios_to_run=(int) context.getBean("scenarios_to_run");
		 total_nodes=(int) context.getBean("total_nodes");
		 access_down_nodes=(Boolean) context.getBean("access_down_nodes");
		 waitTime_after_nodeUPDOWN=(int) context.getBean("waitTime_after_nodeUPDOWN");
		 retry=	(int) context.getBean("retry");
		 
		 //other vars
		 current_node_status=new String[3];
		 read_status=new String[3];
		 write_status=new String[3];
			
		 nodes=new Node[total_nodes];		
		 for(int i=0;i<total_nodes;i++)
		 {
			 nodes[i] =(Node) context.getBean("node"+(i+1));
		 }
		 
		 final_result=new String[scenarios_to_run+1][8];              //3+1
		
	}
	public void writexcel()
	{
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("sheet");
		 
		Map<String, Object[]> data = new HashMap<String, Object[]>();
		
		data.put("1",new Object[] {"Sr No","Node1","Node2","Node3","Node1","Node2","Node3","MasterNode","Comments"} );
		for(int i=1;i<=scenarios_to_run;i++)
		{
			data.put((i+1)+"", new Object[] {i+"",final_result[i][0],final_result[i][1],final_result[i][2],final_result[i][3],final_result[i][4],final_result[i][5],final_result[i][6], final_result[i][7]});
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
			File file=new File("src\\main\\resources\\output\\Output "+tt+".xls");
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
		System.out.println("\nFinal read. Querying Node 1.\n");
		String url2 = "http://localhost:8090/REST/config/attributes?sr=4_Read&neo="+nodes[0].getIp()+"%3A"+nodes[0].getPort()+"&es=456&end=false";
		client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url2);
		httpGet.addHeader("User-Agent", USER_AGENT);
		httpGet.addHeader("Cookie", sessionid);
		CloseableHttpResponse httpResponse = client.execute(httpGet);
		
		if(httpResponse.getStatusLine().getStatusCode()==200)   
			System.out.print("SUCCESS\n");
		else
			if(httpResponse.getStatusLine().getStatusCode()==500)
				System.out.print("FAILURE\n");
			else
				System.out.print("Unknown read write status\n");
				
		//		+ httpResponse.getStatusLine().getStatusCode());
		BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		String inputLine;
		StringBuffer response2 = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			response2.append(inputLine);
		}
		reader.close();
		
		// print result
		//System.out.println("read response JSON is => "+response2.toString());
		
		httpGet.reset();
		JSONObject obj = (JSONObject)parser.parse(response2.toString());
		System.out.println("Final label values are... ");
		
		if( obj.get("status").equals("SUCCESS"))
		 {				
			 //code to read actual data
			JSONArray array=(JSONArray) obj.get("response");
			 //System.out.println("values read are => "+ array.size());
			System.out.print("Total : "+array.size()+". label values are =>");
			 for(int k=0;k<array.size();k++)
			 {
				 JSONObject objj=((JSONObject)array.get(k));
				 System.out.print(objj.get("label")+",");
			 }			 
		 }
		
	}
}
