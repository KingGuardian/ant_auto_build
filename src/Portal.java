import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Portal {
	
	private static final String KEY_ATTR_VALUE ="value";
	private static final String KEY_ATTR_NAME ="name";
	
	private static final String KEY_ATTR_VER_NAME ="android:versionName";
	
	private static final String KEY_NODE_PROJECT ="project";
	private static final String KEY_NODE_ITEM ="item";	
			
    private static final String MAINFEST = "AndroidManifest.xml";
	private static final String BUILD = "build.xml";
	
	private static final String CHANNEL = "channel.xml";

	private String projectName = "";
	private String antRunModel = "";
	
	public static void main(String[] argus) {
		Portal portal = new Portal();
		portal.run();
	}

	
	private void run(){
		readBuildCfg();
		List<ChannelInfo> channelInfos=parseChanelData();
		try {
			startBuild(channelInfos);	
		} catch (Exception e) {
			System.out.println("build error : " + e.getMessage());
			return;
		}				
		System.out.println("build successfully " );
	}

	
	private void readBuildCfg(){
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder;
		InputStream is = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
			is = new FileInputStream(BUILD);
			Document doc = dombuilder.parse(is);
			NodeList nodes =  doc.getElementsByTagName(KEY_NODE_PROJECT);
			if (nodes.getLength()>0){
				Node node= nodes.item(0);
				NamedNodeMap  nodeMaps =node.getAttributes();
				Node nodeAttr = nodeMaps.getNamedItem("name");
				projectName= nodeAttr.getNodeValue();				
				nodeAttr = nodeMaps.getNamedItem("default");
				antRunModel= nodeAttr.getNodeValue();	
			}
			
			
		}catch (Exception e) {
			e.printStackTrace();
		}
			
		if (projectName == null || projectName.isEmpty()) {
			projectName = "default";
		}
		if (antRunModel == null || antRunModel.isEmpty()) {
			antRunModel = "debug";
		}
	}
	
	private String generalTimestamp() {
		Calendar calendar = Calendar.getInstance();
		
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH) + 1;
		int min = calendar.get(Calendar.MINUTE);
		int year = calendar.get(Calendar.YEAR);

		String formater = "%1$02d";
		String sHour = String.format(formater, hour);
		String sMin = String.format(formater, min);

		String sday = String.format(formater, day);
		String sMonth = String.format(formater, month);

		String dateDsc = year + sMonth + sday + sHour + sMin;
		return dateDsc;
	}
	
	private List<ChannelInfo> parseChanelData() {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder;
		InputStream is = null;		
		List<ChannelInfo> channelInfos = new ArrayList<ChannelInfo>();
		try {
			dombuilder = domfac.newDocumentBuilder();
			is = new FileInputStream(CHANNEL);
			Document doc = dombuilder.parse(is);
			NodeList nodes =  doc.getElementsByTagName(KEY_NODE_PROJECT);;
						
			nodes = doc.getElementsByTagName(KEY_NODE_ITEM);	
			
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(KEY_NODE_ITEM)) {
					Node nodeName = node.getAttributes().getNamedItem(KEY_ATTR_NAME);
					Node nodeValue = node.getAttributes().getNamedItem(KEY_ATTR_VALUE);
					ChannelInfo info = new ChannelInfo();
					channelInfos.add(info);
					info.name = nodeName.getNodeValue();
					info.value = nodeValue.getNodeValue();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Parse channel config file error : " + e.getMessage());
		} finally {
			if (is!=null){
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return channelInfos;
	}
	
	static class ChannelInfo{
		String name;
		String value;
		
	}
	
	private void startBuild(List<ChannelInfo> channelList){
		callAnt("clean");		
		String projectVer = getMainfestVerName();
		String dateDsc = generalTimestamp();
		if (channelList != null && !channelList.isEmpty()) {
			String orgiMainfest = readFile(MAINFEST);
			for (ChannelInfo item : channelList) {
				replaceMainfest(item.value);
				replacePkgName(projectName + "-" + item.name + "-V" + projectVer + "-" + dateDsc);

				System.out.println("Ongoing build channel : " + item.value);
				try {
					callAnt(antRunModel);
					callAnt("clean");	
				} catch (Exception e) {
					System.out.println(e.getMessage());
					break;
				}				
				System.out.println("Build channel : " + item.value + " successfull");
			}
			writeFile(MAINFEST, orgiMainfest);
		} else {
			replacePkgName(projectName + "-V" + projectVer + "-" + dateDsc);
			try {
				callAnt(antRunModel);
				callAnt("clean");	
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
		}		
		replacePkgName(projectName);				
	}
		
	private String getMainfestVerName() {
		String ver = "";
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder;
		InputStream is = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
			is = new FileInputStream(MAINFEST);
			Document doc = dombuilder.parse(is);
			Node node = doc.getDocumentElement();
			node = node.getAttributes().getNamedItem(KEY_ATTR_VER_NAME);
			ver = node.getNodeValue();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ver;
	}
	
	private void replacePkgName(String name) {
		String content = readFile(BUILD);
		StringBuffer relacement = new StringBuffer();
		relacement.append("project name=\"");
		relacement.append(name);
		relacement.append("\"");
		relacement.append(" default=\"" + antRunModel + "\"");
		
		String regex = "project name=\"(.*)\"";
		content = content.replaceAll(regex, relacement.toString());
		writeFile(BUILD, content);
	}
	
	private void replaceMainfest(String channel){
		String content = readFile(MAINFEST);
		StringBuffer relacement = new StringBuffer();		
		relacement.append("meta-data android:name=\"channel\"");		
		relacement.append(" android:value=\"");
		relacement.append(channel);
		relacement.append("\"");				  
		String regex = "meta-data android:name=\"channel\" android:value=\"(.*)\"";	
		content= content.replaceAll(regex, relacement.toString());
		writeFile( MAINFEST,content);
		
	}

	 /** 
     *  
     * @param filePath 
     * @param content 
     */  
    public void writeFile(String fileName,String content) {
        BufferedWriter bw = null;            
        try {  
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));  
            bw.write(content);  
        } catch (Exception e) {  
            e.printStackTrace();  
        } finally {  
            if (bw != null) {  
                try {  
                    bw.close();  
                } catch (IOException e) {  
                    bw = null;  
                }  
            }  
        }  
    }  
    
    
 
    
	public String readFile(String fileName) {
		InputStream is = null;
		StringBuffer buf = new StringBuffer();
		try {
			is = new FileInputStream(fileName);
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			int data;
			while ((data = is.read()) != -1) {
				bao.write(data);
			}
			bao.flush();
			buf.append(new String(bao.toByteArray(), "UTF-8"));
			bao.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					is = null;
				}
			}
		}

		return buf.toString();
	}  
    
    
	void executeCmd(String cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			 String ls_1;
			while ((ls_1 = bufferedReader.readLine()) != null) {
				System.out.println(ls_1);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	void callAnt(String target) throws BuildException {
		File buildFile = new File(BUILD);
		Project p = new Project();

		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		p.addBuildListener(consoleLogger);

		p.fireBuildStarted();
		p.init();
		ProjectHelper.configureProject(p, buildFile);
		p.executeTarget(target);
		p.fireBuildFinished(null);

	}
		
}
