package nds.publicweixin.ext.tools;

public class ChangeCNChar {
	public static final String LEFT_QUOTES = "¡°";
	public static final String RIGHT_QUOTES = "¡±";
	public static final String ELLIPSIS = "¡­";
	public static final String ANDS = "&";
	public static final String U1="\\u201c";
	public static final String U2="\\u201d";
	public static final String U3="\\u2026";
	public static final String U4="&amp;";
	
	
	public static String change(String msg){
		if(nds.util.Validator.isNull(msg)){
			return msg;
		}
		msg = msg.replace(U1, LEFT_QUOTES).replace(U2, RIGHT_QUOTES).replace(U3, ELLIPSIS).replace(U4, ANDS);
		//msg = msg.replace(U2, RIGHT_QUOTES);
		//msg = msg.replace(U3, ELLIPSIS);
		return msg;
	}
}
