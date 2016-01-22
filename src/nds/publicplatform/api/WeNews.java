package nds.publicplatform.api;


import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
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
import nds.query.QueryEngine;

public class WeNews {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeNews.class.getName());	 
	private static String we_uploadnews_URL=WebUtils.getProperty("weixin.we_uploadnews_URL","https://api.weixin.qq.com/cgi-bin/material/add_news?");
	private static String we_downloadnews_URL=WebUtils.getProperty("weixin.we_downloadnews_URL","https://api.weixin.qq.com/cgi-bin/material/get_material?");
	private static String we_updatenews_URL=WebUtils.getProperty("weixin.updatenews_URL","https://api.weixin.qq.com/cgi-bin/material/update_news?access_token=");
	private static String we_removeMedia_URL=WebUtils.getProperty("weixin.we_removeMedia_URL","https://api.weixin.qq.com/cgi-bin/material/del_material?access_token=");
	
	private static Hashtable<String,WeNews> weNews;
	private WeNews(){}
	
	public static synchronized WeNews getInstance(String customId){
		if(nds.util.Validator.isNull(customId)){return null;}
		
		WeNews instance=null;
		if(weNews==null){
			weNews=new Hashtable<String,WeNews>();
			instance=new WeNews();
			weNews.put(customId, instance);
		}else if(weNews.containsKey(customId)){
			instance=weNews.get(customId);
		}else{
			instance=new WeNews();
			weNews.put(customId, instance);
		}

		return instance;
	}

	public JSONObject uploadNews(WxPublicControl wc,String news){
		String returnr=null;
		String url=we_uploadnews_URL;
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

		
      	
        	HashMap<String, String> params =new HashMap<String, String>();
    		params.put("access_token",token);

    		logger.debug("uploadnews");
    		try{
    			url+=RestUtils.delimit(params.entrySet(), false);
    		}catch(Exception e){}
    		
        	try{
        		//url+=RestUtils.delimit(params.entrySet(), true);
        		vh=RestUtils.sendRequest_buff(url,news,"POST");
	        	//vh=RestUtils.sendRequest(url, params, "POST");
	        	String result=(String) vh.get("message");
	        	logger.debug("uploadnews result->"+result);
	        	JSONObject jo=new JSONObject(result);
	        	if(jo!=null&&jo.has("media_id")){
	        		returnr=jo.optString("media_id");
	        		resultjo.put("code", 0);
        			resultjo.put("message", returnr);
        			resultjo.put("media_id", returnr);
        			
        			//保存本地图文
        			JSONArray articles = new JSONObject(news).optJSONArray("articles");
        			downloadNews(wc, returnr ,articles);
	        	}else{
	        		returnr=result;
	        		resultjo.put("code", -1);
        			resultjo.put("message", returnr);
	        	}
	        	
        	}catch (Throwable tx) {
        		returnr="uploadnews error:"+tx.getMessage();
        		try {
					resultjo.putOpt("code", -1);
					resultjo.putOpt("message", returnr);
				} catch (JSONException e) {
					e.printStackTrace();
				}
    	 		logger.debug("uploadnews error: "+tx.getMessage());
    	 	}

        
        return resultjo;
	}
	
	public JSONObject downloadNews(WxPublicControl wc,String media_id,JSONArray articles){
		String url=we_downloadnews_URL;
		
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
		

		
    	HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		//params.put("media_id", media_id);
		String param="{\"media_id\":\""+media_id+"\"}";
		ValueHolder vh=null;
		logger.debug("downloadnews");
		Connection con=null;
		try{
			QueryEngine qe=QueryEngine.getInstance();
			con=qe.getConnection();
    		try{
    			url+=RestUtils.delimit(params.entrySet(), false);
    		}catch(Exception e){}
			//url+=RestUtils.delimit(params.entrySet(), true);
        	vh=RestUtils.sendRequest_buff(url, param, "POST");
        	String result= String.valueOf(vh.get("message"));
        	logger.debug("downloadnews result->"+result);
        	JSONObject news = new JSONObject(result);
        	JSONArray newsja = news.optJSONArray("news_item");
        	int wx_media_id=qe.getSequence("wx_media");
			String sql = "insert into wx_media(id,ad_client_id,ad_org_id,MEDIA_ID,MTYPE,CREATED_AT,OWNERID,MODIFIERID,CREATIONDATE,MODIFIEDDATE,CONTENT,NAME,UPFILE)"
					+" values(?,?,?,?,?,?,?,?,sysdate,sysdate,?,?,?)";
			qe.executeUpdate(sql, new Object[]{wx_media_id,wc.getWxPublic().getAd_client_id(),0,media_id,"news","",0,0,newsja.toString(),"",""},con);

        	for(int k=0 ;k<newsja.length();k++){
        		JSONObject thumbjo = newsja.optJSONObject(k);
        		JSONObject fromAndobjidjo =articles.optJSONObject(k);
        		logger.debug("fromid -->"+fromAndobjidjo.optInt("fromid",-1));
        		logger.debug("objid -->"+fromAndobjidjo.optString("objid",""));
        		int fromid=fromAndobjidjo.optInt("fromid",-1);
        		String objid=fromAndobjidjo.optString("objid","");
        		objid=objid!=null?objid:"";
        		
				String itemsql="insert into wx_thumb_media (ID, AD_CLIENT_ID, AD_ORG_ID, TITLE, THUMB_MEDIA_ID, SHOW_COVER_PIC, AUTHOR, DIGEST, CONTENT, URL, CONTENT_SOURCE_URL,FROMID,OBJID, WX_MEDIA_ID, THUMB_INDEX, OWNERID, MODIFIERID, CREATIONDATE, MODIFIEDDATE, ISACTIVE) "
						+ "values (get_sequences('wx_thumb_media'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? , ?, ?, ?, sysdate, sysdate, 'Y')";
				qe.executeUpdate(itemsql, new Object[]{wc.getWxPublic().getAd_client_id(),0,thumbjo.optString("title"),fromAndobjidjo.optString("thumb_media_id"),thumbjo.optString("show_cover_pic"),thumbjo.optString("author"),thumbjo.optString("digest"),thumbjo.optString("content"),thumbjo.optString("url"),thumbjo.optString("content_source_url"),fromid>0?fromid:Integer.class,objid,wx_media_id,k,0,0},con);

        	}
		}catch (Throwable tx) {
    		try {
				resultjo.put("code", -1);
				resultjo.put("message", "获取图文失败："+tx.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
			}
    		
	 		logger.debug("downloadnews error: "+tx.getMessage());
	 	}finally{
	 		if(con!=null){
	 			try{
	 				con.close();
	 			}catch(Exception e){
	 				
	 			}
	 		}
	 	}
		
		return resultjo;
	}
	
	public JSONObject editNews(WxPublicControl wc,String editNewjsons){
		String returnr=null;
		String url=we_updatenews_URL;
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

		
      	
/*        	HashMap<String, String> params =new HashMap<String, String>();
    		params.put("access_token",token);*/

    		logger.debug("updatenews");
    		int succ=0;
    		Connection con=null;
    		try{
    			QueryEngine qe=QueryEngine.getInstance();
    			con=qe.getConnection();
        		JSONArray editja=new JSONArray(editNewjsons);
        		JSONArray resultja= new JSONArray();
        		for(int i=0;i<editja.length();i++){
        			JSONObject editjo = editja.optJSONObject(i);
        			JSONObject resultjoi = new JSONObject();
        			String editNewjson = editjo.toString();
        			int index = editjo.optInt("index");
            		vh=RestUtils.sendRequest_buff(url+token,editNewjson,"POST");
    	        	String result=(String) vh.get("message");
    	        	logger.debug("updatenews index "+index+"  result->"+result);
    	        	JSONObject jo=new JSONObject(result);
    	        	if(jo!=null&&jo.has("errcode")){
    	        		if(jo.optInt("errcode")==0){
    	        			String media_id = editjo.optString("media_id");
    	        			
    	        			JSONObject articles = editjo.optJSONObject("articles");
    	            		logger.debug("fromid -->"+articles.optInt("fromid",-1));
    	            		logger.debug("objid -->"+articles.optString("objid",""));
    	        			String updatesql = "update wx_thumb_media set TITLE=?, SHOW_COVER_PIC=?, AUTHOR=?, DIGEST=?, CONTENT=?, CONTENT_SOURCE_URL=?,FROMID=?,OBJID=?, MODIFIEDDATE=sysdate, ISACTIVE='Y', thumb_media_id=?  where ad_client_id=?  and wx_media_id=(select id from wx_media t where t.media_id=?)   and thumb_index=?";
							qe.executeUpdate(updatesql,  new Object[]{getStringValue(articles.optString("title")),articles.optString("show_cover_pic"),getStringValue(articles.optString("author","")),getStringValue(articles.optString("digest","")),articles.optString("content"),getStringValue(articles.optString("content_source_url","")),
									articles.optInt("fromid",-1)>0?articles.optInt("fromid",-1):Integer.class,getStringValue(articles.optString("objid","")),articles.optString("thumb_media_id"),wc.getWxPublic().getAd_client_id(),media_id,index},con);

    	        		}else if(succ==0&&jo.optInt("errcode")!=0){
    	        			succ=jo.optInt("errcode");
    	        		}
    	        		resultjoi.put("index",index);
    	        		resultjoi.put("code",jo.optInt("errcode"));
    	        		resultjoi.put("message", jo.optString("errmsg"));
    	        	}else{
    	        		succ=-1;
    	        		returnr=result;
    	        		resultjoi.put("index",index);
    	        		resultjoi.put("code", -1);
    	        		resultjoi.put("message", returnr);
    	        	}
    	        	resultja.put(resultjoi);
        		}
				resultjo.putOpt("code", succ);
				resultjo.putOpt("message", resultja.toString());
	        	
        	}catch (Throwable tx) {
        		returnr="updatenews error:"+tx.getMessage();
        		try {
					resultjo.putOpt("code", -1);
					resultjo.putOpt("message", returnr);
				} catch (JSONException e) {
					e.printStackTrace();
				}
    	 		logger.debug("updatenews error: "+tx.getMessage());
    	 	}finally{
    	 		if(con!=null){
    	 			try{
    	 				con.close();
    	 			}catch(Exception e){
    	 				
    	 			}
    	 		}
    	 	}
        
        
        return resultjo;
	}
	
	public String getStringValue(String str){
		if(nds.util.Validator.isNull(str)){
			str="";
		}
		return str;
	}

	public JSONObject removeMedia(WxPublicControl wc,String media_ids){
		String url=we_removeMedia_URL;
		
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
		
		JSONArray mediasja;
		try {
			mediasja = new JSONArray(media_ids);
		} catch (JSONException e1) {
			try {
				resultjo.put("code", -1);
				resultjo.put("message","media_ids 参数错误");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			e1.printStackTrace();
			return resultjo;
		}
		
		JSONArray rja = new JSONArray();
		int errcode=0;
		Connection con=null;
		try{
			QueryEngine qe=QueryEngine.getInstance();
			con=qe.getConnection();
			for(int i=0;i<mediasja.length();i++){
				JSONObject mediajo= mediasja.optJSONObject(i);
				String media_id=mediajo.optString("MEDIA_ID");
				String wx_media_id=mediajo.optString("ID");
	/*	    	HashMap<String, String> params =new HashMap<String, String>();
				params.put("access_token",token);
				params.put("media_id", media_id);*/
				String params="{\"media_id\":\""+media_id+"\"}";
				
				ValueHolder vh=null;
				logger.debug("removeMedia");
				
		        	vh=RestUtils.sendRequest_buff(url+token, params, "POST");
		        	logger.debug("removeMedia "+media_id+" message: "+vh.get("message").toString());
					JSONObject jsonObject = new JSONObject(vh.get("message").toString());
					if(jsonObject.optInt("errcode",-1)==0){
						String delthumsql="delete from wx_thumb_media t where t.wx_media_id=?";
						qe.executeUpdate(delthumsql, new Object[]{wx_media_id},con);
						String  existsql= "select count(1) from wx_thumb_media t where  t.thumb_media_id=?";
						int recordCount = Integer.parseInt(qe.doQueryOne(existsql,
										new Object[] {media_id},con)
								.toString());
						if(recordCount>0){
							String updsql = "update wx_media set ISACTIVE='N'  where  id=? and  media_id=?";
							qe.executeUpdate(updsql,  new Object[]{wx_media_id,media_id},con);
						}else{
							String delsql="delete from wx_media t where  t.id=? and t.media_id=?";
							qe.executeUpdate(delsql, new Object[]{wx_media_id,media_id},con);
						}
					}else if(errcode==0&&jsonObject.optInt("errcode",-1)!=0){
						errcode=-1;
					}
					jsonObject.put("mediaid", media_id);
					rja.put(jsonObject);
	
		        	logger.debug("remove media->"+jsonObject.toString());
				}	    	
		}catch (Throwable tx) {
	 		logger.debug("removeMedia error: "+tx.getMessage());
	 	}finally{
	 		if(con!=null){
	 			try{
	 				con.close();
	 			}catch(Exception e){
	 				
	 			}
	 		}
	 	}
		
		try {
			resultjo.put("code", errcode);
			resultjo.put("message",rja.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
		return resultjo;
	}
}
