package nds.publicplatform.api;

import java.sql.Connection;
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
import nds.security.User;

public class WeBatchGetMaterial {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeBatchGetMaterial.class.getName());
    private static String we_getAllMaterial_URL=WebUtils.getProperty("weixin.we_getAllMaterial_URL","https://api.weixin.qq.com/cgi-bin/material/batchget_material?access_token=");
    private static String we_getMaterialCount_URL=WebUtils.getProperty("weixin.we_getMaterialCount_URL","https://api.weixin.qq.com/cgi-bin/material/get_materialcount?access_token=");
   
	private static Hashtable<String,WeBatchGetMaterial> weBatchGetMaterials;
	private WeBatchGetMaterial(){}
	public static synchronized WeBatchGetMaterial getInstance(String customId){
		if(nds.util.Validator.isNull(customId)){return null;}
		
		WeBatchGetMaterial instance=null;
		if(weBatchGetMaterials==null){
			weBatchGetMaterials=new Hashtable<String,WeBatchGetMaterial>();
			instance=new WeBatchGetMaterial();
			weBatchGetMaterials.put(customId, instance);
		}else if(weBatchGetMaterials.containsKey(customId)){
			instance=weBatchGetMaterials.get(customId);
		}else{
			instance=new WeBatchGetMaterial();
			weBatchGetMaterials.put(customId, instance);
		}

		return instance;
	}
	
	public JSONObject getAllMaterials(WxPublicControl wc,User user,String type) throws Exception{
		JSONObject resultjo=new JSONObject();
		
		JSONObject newsjo=null;
		String media_id = "";
		String content = "";
		String update_time = "";
		
		String name = "";
		String url = "";
		
		
		//素材偏移下标
		int offset = 0;
		//每页素材数
		int count = 20;
		//String QQPARAM = "{\"type\":\""+type+"\",\"offset\":"+offset+",\"count\":"+count+"}";
		String QQPARAM = null;
		//素材总数
		int scount=0;
		//已获取总数
		int ccount=0;
		//总素材存储
		ValueHolder hd =null;
		
		ValueHolder counthd =null;
		
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
		counthd=RestUtils.sendRequest_buff(we_getMaterialCount_URL+token, "", "POST");
		logger.debug("test we_getMaterialCount"+counthd.get("message").toString());
		JSONObject countjo = new JSONObject(counthd.get("message").toString());
		int  image_count= countjo.optInt("image_count");
		int news_count=countjo.optInt("news_count");
		if("news".equals(type)){
			if(news_count<=0){
				resultjo.put("code", -1);
				resultjo.put("message", "获取失败,news_count为"+news_count);
				return resultjo;
			}else{
				getAllMaterials(wc,user,"image");
				count=news_count>=20?20:news_count;
				logger.debug("每页素材数 news_count-->"+count);
			}
		}else if("image".equals(type)){
			if(image_count<=0){
				resultjo.put("code", -1);
				resultjo.put("message", "获取失败,image_count为"+image_count);
				return resultjo;
			}else{
				count=image_count>=20?20:image_count;
				logger.debug("每页素材数 image_count-->"+count);
			}
		}else{
			resultjo.put("code", -1);
			resultjo.put("message", "获取失败,type"+type+" 暂不支持!!");
			return resultjo;
		}
		
		


		Connection con=null;
		try {
			
			QueryEngine qe=QueryEngine.getInstance();
			con=qe.getConnection();
			
			//先把当前类型全部不可用,再循环比较,更新或插入数据

			QueryEngine.getInstance().executeUpdate("update wx_media set ISACTIVE='N' where ad_client_id=? and mtype=?",new Object[] { wc.getWxPublic().getAd_client_id(),type},con);
			do{
				QQPARAM = "{\"type\":\""+type+"\",\"offset\":"+offset+",\"count\":"+count+"}";
				hd=RestUtils.sendRequest_buff(we_getAllMaterial_URL+token, QQPARAM, "POST");
				logger.debug("testcc"+hd.get("message").toString());
				
				JSONObject jsonObject = new JSONObject(hd.get("message").toString());
				scount = jsonObject.optInt("total_count");
				ccount+=jsonObject.optInt("item_count");
				offset = ccount;

				for(int i = 0; i < jsonObject.optInt("item_count"); i++){
					newsjo = jsonObject.optJSONArray("item").optJSONObject(i);
					media_id = newsjo.optString("media_id");
					name = newsjo.optString("name");
					content = newsjo.optString("content");
					update_time = newsjo.optString("update_time");
					url = newsjo.optString("url");
					String sql="";
					String  existsql= "select count(1) from wx_media t where t.ad_client_id=? and t.media_id=?";
					int recordCount = Integer.parseInt(QueryEngine
							.getInstance()
							.doQueryOne(existsql,
									new Object[] { wc.getWxPublic().getAd_client_id(), media_id},con)
							.toString());
					if(recordCount>0){
/*						sql = "update wx_media set CREATED_AT=?,OWNERID=?,MODIFIERID=?,MODIFIEDDATE=sysdate,CONTENT=?,NAME=?,UPFILE=?,ISACTIVE='Y'  where ad_client_id=? and media_id=?";
						QueryEngine.getInstance().executeUpdate(sql,  new Object[]{update_time,user.id,user.id,content,name,url,user.adClientId,media_id});
*/
						sql = "update wx_media set CREATED_AT=?,OWNERID=?,MODIFIERID=?,MODIFIEDDATE=sysdate,CONTENT=?,UPFILE=?,ISACTIVE='Y'  where ad_client_id=? and media_id=?";
						QueryEngine.getInstance().executeUpdate(sql,  new Object[]{update_time,user.id,user.id,content,url,user.adClientId,media_id},con);
					}else{
						sql = "insert into wx_media(id,ad_client_id,ad_org_id,MEDIA_ID,MTYPE,CREATED_AT,OWNERID,MODIFIERID,CREATIONDATE,MODIFIEDDATE,CONTENT,NAME,UPFILE)"
								+" values(get_sequences('wx_media'),?,?,?,?,?,?,?,sysdate,sysdate,?,?,?)";
						QueryEngine.getInstance().executeUpdate(sql, new Object[]{wc.getWxPublic().getAd_client_id(),user.adOrgId,media_id,type,update_time,user.id,user.id,content,name,url},con);
					}
					
					if("news".equals(type)&&newsjo.optJSONObject("content")!=null){
						JSONArray news_item = newsjo.optJSONObject("content").optJSONArray("news_item");
						String getIdsql="select id from wx_media where ad_client_id=? and media_id=?  ";
						String mediaid=QueryEngine.getInstance().doQueryOne(getIdsql,  new Object[]{wc.getWxPublic().getAd_client_id(),media_id}).toString();
						
						//先把图文明细全部不可用,再循环比较,更新或插入数据
						QueryEngine.getInstance().executeUpdate("update wx_thumb_media set ISACTIVE='N' where ad_client_id=? and wx_media_id=?",new Object[] { wc.getWxPublic().getAd_client_id(), mediaid},con);

						String itemsql;
						String title,thumb_media_id,show_cover_pic,author,digest,itemcontent,itemurl,content_source_url;
						for(int ic=0;ic<news_item.length();ic++){
							JSONObject itemjo = news_item.optJSONObject(ic);
							title = itemjo.optString("title");
							thumb_media_id = itemjo.optString("thumb_media_id");
							show_cover_pic = itemjo.optString("show_cover_pic");
							author = itemjo.optString("author");
							digest = itemjo.optString("digest");
							itemcontent = itemjo.optString("content");
							itemurl = itemjo.optString("url");
							content_source_url = itemjo.optString("content_source_url");
							String  existitemsql= "select count(1) from wx_thumb_media t where t.ad_client_id=? and t.thumb_media_id=? and t.wx_media_id=? and t.thumb_index=?";
							int itemCount = Integer.parseInt(QueryEngine
									.getInstance()
									.doQueryOne(existitemsql,
											new Object[] { wc.getWxPublic().getAd_client_id(), thumb_media_id,mediaid,ic},con)
									.toString());
							if(itemCount>0){
								itemsql = "update wx_thumb_media set TITLE=?, SHOW_COVER_PIC=?, AUTHOR=?, DIGEST=?, CONTENT=?, URL=?, CONTENT_SOURCE_URL=?, MODIFIEDDATE=sysdate, ISACTIVE='Y' where ad_client_id=? and thumb_media_id=? and wx_media_id=? and thumb_index=?";
								QueryEngine.getInstance().executeUpdate(itemsql,  new Object[]{title,show_cover_pic,author,digest,itemcontent,itemurl,content_source_url,wc.getWxPublic().getAd_client_id(),thumb_media_id,mediaid,ic},con);
							}else{
								itemsql="insert into wx_thumb_media (ID, AD_CLIENT_ID, AD_ORG_ID, TITLE, THUMB_MEDIA_ID, SHOW_COVER_PIC, AUTHOR, DIGEST, CONTENT, URL, CONTENT_SOURCE_URL, WX_MEDIA_ID, THUMB_INDEX, OWNERID, MODIFIERID, CREATIONDATE, MODIFIEDDATE, ISACTIVE) "
										+ "values (get_sequences('wx_thumb_media'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, sysdate, sysdate, 'Y')";
								QueryEngine.getInstance().executeUpdate(itemsql, new Object[]{wc.getWxPublic().getAd_client_id(),user.adOrgId,title,thumb_media_id,show_cover_pic,author,digest,itemcontent,itemurl,content_source_url,mediaid,ic,user.id,user.id},con);
							}
						}
					}

				}
			}while(scount>ccount);
			resultjo.put("code", 0);
			resultjo.put("message", "获取素材列表成功");
		} catch (Exception e) {
			resultjo.put("code", -1);
			resultjo.put("message", "获取素材列表失败："+e.getMessage());
			e.printStackTrace();
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

}
