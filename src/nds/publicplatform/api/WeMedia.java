package nds.publicplatform.api;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.tools.RestUtils;

public class WeMedia {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeMedia.class.getName());
	private static String we_uploadfile_URL=WebUtils.getProperty("weixin.we_uploadfile_URL","https://api.weixin.qq.com/cgi-bin/material/add_material?");
	private static String we_downloadfile_URL=WebUtils.getProperty("weixin.we_downloadfile_URL","");
	private static String we_uploadImage_URL=WebUtils.getProperty("weixin.we_uploadImage_URL","https://api.weixin.qq.com/cgi-bin/media/uploadimg?");
	
	
	private static Hashtable<String,WeMedia> wmedias;
	private WeMedia(){
	}
	
	public static synchronized WeMedia getInstance(String pappid){
		if(nds.util.Validator.isNull(pappid)){return null;}
		
		WeMedia instance=null;
		if(wmedias==null){
			wmedias=new Hashtable<String,WeMedia>();
			instance=new WeMedia();
			wmedias.put(pappid, instance);
		}else if(wmedias.containsKey(pappid)){
			instance=wmedias.get(pappid);
		}else{
			instance=new WeMedia();
			wmedias.put(pappid, instance);
		}

		return instance;
	}
	
	public JSONObject uploadFile(WxPublicControl wc,JSONObject pjo){
		logger.debug("uploadFile---begin----------filePath:"+pjo.optString("filepath"));
		String returnr=null;
		String url=we_uploadfile_URL;
		JSONObject resultjo=new JSONObject();
		
		JSONObject atoken=wc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				resultjo.put("code", "-1");
				resultjo.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return resultjo;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				resultjo.put("code", -1);
				resultjo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}
		
		String type=pjo.optString("type");
		String filePath=pjo.optString("filepath");
		
		ValueHolder vh=null;
		File file =null;
		
		try{
			file = new File(filePath);
			if(!file.exists()){
				returnr="File not find!"+filePath;
				resultjo.put("code", -1);
				resultjo.put("message", returnr);
				return resultjo;
			}
			FileInputStream fis = null;
		    fis = new FileInputStream(file);
		    fis.close();
		}catch(Throwable t){
			returnr="File not find!"+t.getMessage();
			try {
				resultjo.putOpt("code", -1);
				resultjo.putOpt("message", returnr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}

        //String contentType = new MimetypesFileTypeMap().getContentType(file); 
        /*需要 commons-loggin.jar;jmimemagic.jar
		 Magic parser = new Magic(); 
        MagicMatch match = parser.getMagicMatch(file, false); */
        String prefix=filePath.substring(filePath.lastIndexOf(".")+1);

        if(nds.util.Validator.isNotNull(prefix)){
        	long filesize=file.length()/1024;
        	if(prefix.equalsIgnoreCase("JPG")){
        		if(filesize>128 && prefix.equalsIgnoreCase("image")){
        			returnr="File size is larger than 128k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			
        			return resultjo;
        		}else if(filesize>64 && prefix.equalsIgnoreCase("thumb")){
        			returnr="File size is larger than 64k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}else if("AMR".equalsIgnoreCase(prefix)||"MP3".equalsIgnoreCase(prefix)){
        		if(filesize>256){
        			returnr="File size is larger than 256k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}else if("MP4".equalsIgnoreCase(prefix)){
        		if(filesize>1024){
        			returnr="File size is larger than 1Mb!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}
        	
        	HashMap<String, String> params =new HashMap<String, String>();
    		params.put("access_token",token);
    		params.put("type", type);

    		logger.debug("uploadfile");
        	try{
        		url+=RestUtils.delimit(params.entrySet(), true);
	        	vh=RestUtils.sendRequest(url, null, "POST",file);
	        	String result=(String) vh.get("message");
	        	logger.debug("uploadfile result->"+result);
	        	JSONObject jo=new JSONObject(result);
	        	if(jo!=null&&jo.has("media_id")){
	        		returnr=jo.optString("media_id");
	        		resultjo.put("code", 0);
        			resultjo.put("message", returnr);
        			resultjo.put("media_id", returnr);
        			resultjo.put("url", jo.optString("url"));
	        	}else{
	        		returnr=result;
	        		resultjo.put("code", -1);
        			resultjo.put("message", returnr);
	        	}
	        	
        	}catch (Throwable tx) {
        		returnr="uploadfile error:"+tx.getMessage();
        		try {
					resultjo.putOpt("code", -1);
					resultjo.putOpt("message", returnr);
				} catch (JSONException e) {
					e.printStackTrace();
				}
    	 		logger.debug("uploadfile error: "+tx.getMessage());
    	 	}
        }else{
        	logger.debug("Mimetypes errors");
        	returnr="Mimetypes errors!";
        	try {
				resultjo.putOpt("code", -1);
				resultjo.putOpt("message", returnr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        
        return resultjo;
	}
	
	public JSONObject downloadFile(WxPublicControl wc,JSONObject pjo){
		byte[] result=null;	
		String url=we_downloadfile_URL;
		
		JSONObject resultjo=new JSONObject();
		
		JSONObject atoken=wc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				resultjo.put("code", "-1");
				resultjo.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return resultjo;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				resultjo.put("code", -1);
				resultjo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}
		
		String filePath=pjo.optString("filePath");
		String media_id=pjo.optString("media_id");
		
    	HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		params.put("media_id", media_id);
		
		ValueHolder vh=null;
		logger.debug("downloadfile");
		try{
			//url+=RestUtils.delimit(params.entrySet(), true);
        	vh=RestUtils.sendRequest_buffs(url, params, "POST");
        	result=(byte[])vh.get("message");
        	OutputStream os = new FileOutputStream(filePath);
        	os.write(result);
        	os.close();
        	
        	resultjo.put("code", 0);
			resultjo.put("message", "下载成功");
        	logger.debug("resulttype->"+vh.get("message").getClass().getSimpleName());
    	}catch (Throwable tx) {
    		try {
				resultjo.put("code", -1);
				resultjo.put("message", "下载失败："+tx.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
			}
    		
	 		logger.debug("downloadfile error: "+tx.getMessage());
	 	}
		
		return resultjo;
	}
	
	
	public JSONObject uploadImage(WxPublicControl wc,String filePath){
		logger.debug("uploadImage---begin----------filePath:"+filePath);
		String returnr=null;
		String url=we_uploadImage_URL;
		JSONObject resultjo=new JSONObject();
		
		
		JSONObject atoken=wc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				resultjo.put("code", "-1");
				resultjo.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return resultjo;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				resultjo.put("code", -1);
				resultjo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}
		
		ValueHolder vh=null;
		File file =null;
		
		try{
			file = new File(filePath);
			if(!file.exists()){
				returnr="File not find!"+filePath;
				resultjo.put("code", -1);
				resultjo.put("message", returnr);
				return resultjo;
			}
			FileInputStream fis = null;
		    fis = new FileInputStream(file);
		    fis.close();
		}catch(Throwable t){
			returnr="File not find!"+t.getMessage();
			try {
				resultjo.putOpt("code", -1);
				resultjo.putOpt("message", returnr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}

        //String contentType = new MimetypesFileTypeMap().getContentType(file); 
        /*需要 commons-loggin.jar;jmimemagic.jar
		 Magic parser = new Magic(); 
        MagicMatch match = parser.getMagicMatch(file, false); */
        String prefix=filePath.substring(filePath.lastIndexOf(".")+1);

        if(nds.util.Validator.isNotNull(prefix)){
        	long filesize=file.length()/1024;
        	if(prefix.equalsIgnoreCase("JPG")){
        		if(filesize>128 && prefix.equalsIgnoreCase("image")){
        			returnr="File size is larger than 128k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			
        			return resultjo;
        		}else if(filesize>64 && prefix.equalsIgnoreCase("thumb")){
        			returnr="File size is larger than 64k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}else if("AMR".equalsIgnoreCase(prefix)||"MP3".equalsIgnoreCase(prefix)){
        		if(filesize>256){
        			returnr="File size is larger than 256k!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}else if("MP4".equalsIgnoreCase(prefix)){
        		if(filesize>1024){
        			returnr="File size is larger than 1Mb!";
        			try {
						resultjo.putOpt("code", -1);
						resultjo.putOpt("message", returnr);
					} catch (JSONException e) {
						e.printStackTrace();
					}
        			return resultjo;
        		}
        	}
        	
        	HashMap<String, String> params =new HashMap<String, String>();
    		params.put("access_token",token);
 
    		logger.debug("uploadImage");
        	try{
        		url+=RestUtils.delimit(params.entrySet(), true);
	        	vh=RestUtils.sendRequest(url, null, "POST",file);
	        	String result=(String) vh.get("message");
	        	logger.debug("uploadImage result->"+result);
	        	JSONObject jo=new JSONObject(result);
	        	if(jo!=null&&jo.has("url")){
	        		returnr=jo.optString("url");
	        		resultjo.put("code", 0);
        			resultjo.put("message", returnr);
        			resultjo.put("url", returnr);
	        	}else{
	        		returnr=result;
	        		resultjo.put("code", -1);
        			resultjo.put("message", returnr);
	        	}
	        	
        	}catch (Throwable tx) {
        		returnr="uploadImage error:"+tx.getMessage();
        		try {
					resultjo.putOpt("code", -1);
					resultjo.putOpt("message", returnr);
				} catch (JSONException e) {
					e.printStackTrace();
				}
    	 		logger.debug("uploadImage error: "+tx.getMessage());
    	 	}
        }else{
        	logger.debug("Mimetypes errors");
        	returnr="Mimetypes errors!";
        	try {
				resultjo.putOpt("code", -1);
				resultjo.putOpt("message", returnr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        
        return resultjo;
	}

}
