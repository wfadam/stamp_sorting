package com.s;

import com.s.json.*;
import com.a.kei.*;
import java.util.*;
import java.io.*;

public class StampParse{
  public static boolean showLog = false;

	public StampParse() {
		if( isMod(fname) || jsObj == null) {
			jsObj = read(fname);
			grpMap = genMap(jsObj);
			TRUE_IF_16KMODE = _is16KMode();
			DEFAULT_BIN	= _defaultBin();
		}
		foundInGrp = initCounter( grpMap.keySet());
	}

//	public static void main(String[] args){
//
//		showLog = true;
//		StampParse[] stmp = new StampParse[getCDUT().length];
//		for(int dut : getCDUT()) stmp[dut-1] = new StampParse();
//
//		System.out.println("COL MODE is set to "+(is16KMode()?"16K":"8K"));
//		System.out.println("DEFAULT BIN is "+defaultBin());
//
//		// stamp reading
//		for(StampInfo si : StampParse.stamps()){
//
//			String stamp_name	= si.name;
//			String group		= si.group; // stamp group name
//
//			int start_col	= si.startCol;
//			int col_cnt		= si.colCnt;
//			int page		= si.page;
//			int exp			= si.exp;  // stamp value expected
//
//			// pattern executing
//
//			for(int dut : getCDUT()) stmp[dut-1].inc( si.group);
//		}
//
//		// binout
//		for(int dut : getCDUT()){
//			int sort = stmp[dut-1].findBin();
//			System.out.printf("-----------------------------------DUT%02d is set to %d\n", dut, sort);
//		}
//
//	}
//	private static int[] getCDUT(){
//		return new int[]{1,2,3,4};
//	}


	public static boolean is16KMode(){
		return TRUE_IF_16KMODE;
	}

	public static int defaultBin(){
		return DEFAULT_BIN;
	}

	public static List<StampInfo> stamps(){
		List<StampInfo> si = new ArrayList<StampInfo>();
		for( Map.Entry<String, Set<String>> kv : grpMap.entrySet()){
			for(String stamp_name : kv.getValue()){
				si.add( getStamp( kv.getKey(), stamp_name));
			}
		}
		return si;
	}

	public void inc(String grp_name){
		foundInGrp.put(grp_name, foundInGrp.get(grp_name)+1);
	}

	private static boolean _is16KMode(){
		JSONArray jarr = getJO(jsObj, DEF_SEC).getJSONArray(COL_VAR);
		return COL_16K.equals(jarr.optString(0));
	}

	private static int _defaultBin(){
		JSONArray jarr = getJO(jsObj, DEF_SEC).getJSONArray(BIN_VAR);
		return Integer.decode(jarr.optString(0));
	}

	public int findBin(){
		int rslt_bin = DEFAULT_BIN;
		for(String bin_name : binNames()){
			System.out.println("Checking "+bin_name);

			if( isMatch( bin_name)){
				System.out.println("!!!!! " + bin_name + " is found !!!!!");
				rslt_bin = Integer.decode(bin_name.replace("bin",""));
				break;
			}
		}
		return rslt_bin;
	}


	/*
	* Private Methods
	*/
	private boolean isMatch(String bin_name){
		return judgeBinLogic( getJO(jsObj, BIN_SEC).getJSONArray(bin_name));
	}

	private boolean judgeBinLogic(Object o){
		JSONArray jarr = (JSONArray)o;
		String op = null;
		Object bl = null;
		boolean accu_rslt;

		int len = jarr.length();
		if(len==1){
			bl	= jarr.get(0);
			accu_rslt = isAllFound((String)bl);  // boolean

		} else if(len==2){
			dlog("unary_op  " + jarr.toString());

			op = (String)jarr.get(0);
			bl	= jarr.get(1);
			if(op.equals(OP_NOT_ALL)){
				accu_rslt = !(bl instanceof String
								? isAllFound((String)bl)  // boolean
								: judgeBinLogic(bl));  // nested expression
			} else if(op.equals(OP_NOT_ANY)){
				accu_rslt = !(bl instanceof String
								? isAnyHit((String)bl)
								: judgeBinLogic(bl));
			} else{
				System.err.println("Unsupported unary operator !!");
				return false;
			}

		} else{
			dlog("binary_op " + jarr.toString());
	
			op = (String)jarr.get(1);
			if(op.equals(OP_AND))		accu_rslt = true;
			else if(op.equals(OP_OR))	accu_rslt = false;
			else{
				System.err.println("Unsupported binary operator !!");
				return false;
			}
	
			for(int i=0; i<len; i+=2){
			
				bl	= jarr.get(i);
				boolean mid = bl instanceof String
								? isAllFound((String)bl)
								: judgeBinLogic(bl);
	
				if(op.equals(OP_AND))		accu_rslt = accu_rslt && mid;
				else if(op.equals(OP_OR))	accu_rslt = accu_rslt || mid;
	
			}
		}
	
		return accu_rslt;
	}

	private boolean isAllFound(String grp_name){
		int cnt = foundInGrp.get(grp_name);
		int tot = grpMap.get(grp_name).size();
		boolean rslt = (cnt==tot)&&(tot>0);

		System.out.println(grp_name + " is "+ rslt + "(" + cnt + "/" + tot + ", true if all found)");
		return rslt;
	}

	private boolean isAnyHit(String grp_name){
		int cnt = foundInGrp.get(grp_name);
		int tot = grpMap.get(grp_name).size();
		boolean rslt = cnt>0;

		System.out.println(grp_name + " is "+ rslt + "(" + cnt + "/" + tot + ", true if any hit)");
		return rslt;
	}


	private static JSONObject read(String fname) {
		try{
			return new JSONObject(new JSONTokener(new BufferedInputStream(new FileInputStream(fname))));
		} catch( FileNotFoundException e){
			throw new RuntimeException(fname, e);
		}
	}

	private static List<String> binNames(){
		List<String> bin_num = new ArrayList<String>(Arrays.asList(JSONObject.getNames( getJO(jsObj, BIN_SEC))));
		sortStr(bin_num);
		return 	bin_num;
	}

	private static void sortStr(List<String> str_arr){
		Collections.sort(	str_arr, 
					new Comparator<String>() {
					public int compare(String a, String b) {
						return a.compareTo(b); }});
	}

	private static Map<String, Integer> initCounter(Set<String> nameSet){
		Map<String, Integer> counter = new HashMap<String, Integer>();
		for( String k : nameSet) counter.put(k, 0);
		return counter;
	}

	private static void dlog(String str){
		if(showLog) System.out.println(str);
	}

	private static Map<String, Set<String>> genMap(JSONObject jsObj){

		Set<String> grpNames = new HashSet<String>(Arrays.asList(JSONObject.getNames(jsObj)));
		grpNames.removeAll(resv_sec);

		Map<String, Set<String>> grpMap = new HashMap<String, Set<String>>();
		for(String grp_name : grpNames){
			grpMap.put(grp_name,  new HashSet<String>(	Arrays.asList(
										JSONObject.getNames(	//returns stamp names
											getJO(jsObj, grp_name))))); //returns the obj of a group
		}
		return grpMap;
	}

	private static StampInfo getStamp(String grp_name, String stamp_name){

		StampInfo si = new StampInfo();
		si.name		= stamp_name;
		si.group	= grp_name;

		JSONArray jarr = getJO(jsObj, grp_name).getJSONArray(stamp_name);
		for(int i=0; i<jarr.length(); i++){
			String ele = jarr.optString(i);
			switch(i){
				case 0:	si.startCol	= Integer.decode(ele);	break;
				case 1:	si.colCnt	= Integer.decode(ele);	break;
				case 2:	si.page		= Integer.decode(ele);	break;
				case 3:	si.exp		= Integer.decode(ele);	break;
			}
		}

		return si;
	}

	private final static JSONObject getJO(JSONObject o, String name){
		return (JSONObject)o.get(name);
	}

	private static boolean isMod(String fname){
		try{
			long c = new File(fname).lastModified();
			boolean mod = (file_mod_time != c);
			file_mod_time = mod ? c : file_mod_time;
			return mod;
		} catch( Exception e){
			throw new RuntimeException(fname, e);
		}
	}

	private final static String fname = "stamp.json";
	private       static long file_mod_time	= 0;
	private       static int DEFAULT_BIN;
	private       static boolean TRUE_IF_16KMODE;

	// reserved chars
	private final static String BIN_SEC	= "BIN";
	private final static String DEF_SEC	= "DEFAULT";
	private final static String COL_VAR	= "col_mode";
	private final static String COL_16K	= "16k";
	private final static String BIN_VAR	= "bin";
	private final static String OP_NOT_ALL	= "~";
	private final static String OP_NOT_ANY	= "!";
	private final static String OP_AND	= "&";
	private final static String OP_OR	= "|";
	private final static Set<String> resv_sec	= new HashSet<String>(Arrays.asList(BIN_SEC,DEF_SEC));
	private final static Set<String> resv_var	= new HashSet<String>(Arrays.asList(COL_VAR,BIN_VAR));

	private       static JSONObject jsObj = null;
	private       static Map<String, Set<String>> grpMap = null; // group name : set of stamp names
	private              Map<String, Integer> foundInGrp = null; // group name : # of found stamps

}

