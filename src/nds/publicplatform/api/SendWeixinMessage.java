package nds.publicplatform.api;

import java.util.ArrayList;
import java.util.Hashtable;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.publicweixin.ext.tools.WeixinSipStatus;
import nds.query.QueryEngine;
import nds.query.QueryException;

import org.json.JSONException;
import org.json.JSONObject;

public class SendWeixinMessage {	
	private static Logger logger= LoggerManager.getInstance().getLogger(SendWeixinMessage.class.getName());	 
	private static final String we_send_all_weixinmessage_URL=WebUtils.getProperty("we_send_all_weixinmessage_URL","https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=");
	private static final String we_batch_send_wexinMsg_URL = WebUtils.getProperty("we_batch_send_weixinmessage_URL","https://api.weixin.qq.com/cgi-bin/message/mass/send?access_token=");
	
	private static Hashtable<String,SendWeixinMessage> weNews;
	private SendWeixinMessage(){}
	
	public static synchronized SendWeixinMessage getInstance(String customId){//为了节省资源，不用new一个对象，直接传过来
		if(nds.util.Validator.isNull(customId)){return null;}
		
		SendWeixinMessage instance=null;
		if(weNews==null){
			weNews=new Hashtable<String,SendWeixinMessage>();
			instance=new SendWeixinMessage();
			weNews.put(customId, instance);
		}else if(weNews.containsKey(customId)){
			instance=weNews.get(customId);
		}else{
			instance=new SendWeixinMessage();
			weNews.put(customId, instance);
		}
		return instance;
	}

	public JSONObject sendmessage(WxPublicControl wc,JSONObject mjo) throws JSONException{//mjo是我们将要传给微信服务器的数据
		JSONObject result= new JSONObject();
		
		
		int ad_client_id =wc.getWxPublic().getAd_client_id();
		JSONObject atoken=wc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				result.put("code", "-1");
				result.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return result;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				result.put("code", -1);
				result.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return result;
		}
		
		if(nds.util.Validator.isNull(mjo.optString("msgtype"))){//对消息进行异常处理，当信息为空的时候需要捕捉异常
			try {
				result.put("code", -1);
				result.put("message", "消息类型不能为空");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return result;
		}
		
		String is_to_all = mjo.optString("is_to_all");
		String touser = mjo.optString("touser","");
		String msgtype= mjo.optString("msgtype");
		String msg_content= mjo.optString("msg_content");
		String url="";
		JSONObject jo = new JSONObject();
		if(is_to_all.equals("true")){
			url = we_send_all_weixinmessage_URL+token;//拼接url，url指上面的那个final修饰的we_sendweixinmessage_URL
			//用于设定是否向全部用户发送，值为true或false，选择true该消息群发给所有用户，选择false可根据group_id发送给指定群组的用户
			jo.put("filter", "{is_to_all:false}");
			jo.put("msgtype", msgtype);
			JSONObject conjo = new JSONObject();
			if("text".equals(msgtype)){
				conjo.put("content", msg_content);
			}else{
				conjo.put("media_id", msg_content);
			}
			jo.put(msgtype, conjo);
		}else{
			url = we_batch_send_wexinMsg_URL+token;
			String getVip = "select t.wechatno from wx_vip t where t.id  "+touser;
			ArrayList vlist = null;
			try {
				vlist = (ArrayList)QueryEngine.getInstance().doQueryList(getVip);
			} catch (QueryException e) {
				e.printStackTrace();
			}
			if(vlist==null||vlist.size()<=0){
				result.put("code", -1);
				result.put("message", "发送VIP不能为空");
				return result;
			}
			
			logger.debug("tousers------------>"+vlist.toString());
			jo.put("touser",vlist);
			jo.put("msgtype", msgtype);
			JSONObject conpo=new JSONObject();
			if("text".equals(msgtype)){
				conpo.put("content", msg_content);
			}else{
				conpo.put("media_id", msg_content);
			}
			jo.put(msgtype, conpo);
	
		}
		logger.debug("sendWeixinMessage begin----sendMsg"+jo.toString());
		
		
		ValueHolder vh=null;
		
		try{
			vh=RestUtils.sendRequest_buff(url, jo.toString(), "POST");//message是url传送的内容
			String resultstr= String.valueOf(vh.get("message"));//message是字符串形式的，我们要将message转换成json格式
			logger.debug("sendWeixinMsg result->"+resultstr);
        	JSONObject tjo = new JSONObject(resultstr);
        	String returns="";
        	if(tjo!=null&&tjo.has("errcode")) {//微信返回的errcode我们需要对其进行转换，转换成code
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(tjo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(tjo.has("errmsg")){returns=tjo.optString("errmsg");}
				result.put("code", tjo.optInt("errcode",-1));
				result.put("message", returns);
				if(tjo.optInt("errcode",-1)==0){
					String msg_id = tjo.optString("msg_id","");
					String msg_data_id = tjo.optString("msg_data_id","");
					String errmsg = tjo.optString("errmsg","");
					result.put("msg_id", msg_id);//获得msg_id以及msg_data_id，需要将获得的更新到数据库中
					result.put("msg_data_id", msg_data_id);
					int wx_mass_id = QueryEngine.getInstance().getSequence("WX_MASS_MESSAGE");
					String insert_sql = "insert into wx_mass_message(id,ad_client_id,description,is_to_all,touser,msgtype,msg_content,msg_id,msg_data_id,errmsg,creationdate,modifieddate)" +
							" values(?,?,?,?,?,?,?,?,?,?,sysdate,sysdate)";
					QueryEngine.getInstance().executeUpdate(insert_sql, new Object[]{wx_mass_id,ad_client_id,mjo.optString("description"),is_to_all,touser,msgtype,msg_content,msg_id,msg_data_id,errmsg});
				}
			}else {
				result.put("code", -1);
				result.put("message", returns);
			}
		}catch(Exception e){//异常处理,所有不成功的code都为-1，返回的是错误的状态码对应的状态属性
			try {
				result.put("code", "-1");
				result.put("message", "公共平台网络通信障碍->"+e.getMessage());
			} catch (JSONException t) {
				e.printStackTrace();
			}
			//logger.debug("公共平台网络通信障碍->"+e.getMessage());
		}
		
		
		
		return result;
	}
}
