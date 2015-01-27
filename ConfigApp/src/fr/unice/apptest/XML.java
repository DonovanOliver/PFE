package fr.unice.apptest;

import java.util.ArrayList;

import android.util.Log;

public class XML{
	private Tree tree;
	private String name;
	private Documents documents;
	
	class Tree{
		private ArrayList<Tree> trees=new ArrayList<Tree>();
		private Tree father;
		private String name;
		private String value=null;
		
		Tree(Tree father,String name){
			this.father=father;
			this.name=name;
		}
		
		Tree getByIndex(String name,int index){
			Tree res=null;
			for(int i=0;i<trees.size();i++){
				if(trees.get(i).getName().equals(name)){
					if(index==0){
						res=trees.get(i);
						break;
					}
					else{
						index--;
					}
				}
			}
			return res;
		}
		
		Tree get(int index){
			return trees.get(index);
		}

		void removeAll(String name){
			for(int i=0;i<trees.size();i++){
				if(trees.get(i).getName().equals(name)){
					trees.get(i).remove();
				}
			}
		}
		
		private void remove(){
			for(int i=0;i<trees.size();i++){
				trees.get(i).remove();
			}
		}
		
		void removeIndex(String name,int index){
			for(int i=0;i<trees.size();i++){
				if(trees.get(i).getName().equals(name)){
					if(index==0){
						trees.get(i).remove();
						break;
					}
					else{
						index--;
					}
				}
			}
		}
		

		
		void add(String name){
			trees.add(new Tree(this,name));
		}
		
		int toInt(String value){
			int res=0;
			if(value==null){return 0;}
			for(int i=0;i<value.length();i++){
				if(value.charAt(i)>='0'&&value.charAt(i)<='9'){
					res*=10;
					res+=value.charAt(i)-'0';
				}
				else{
					Log.i("XML::toInt","warning char not a digit");
				}
			}
			return res;
		}
		
		Tree last(){
			return trees.get(size()-1);
		}
		
		int size(){
			return trees.size();
		}
		
		String getName(){
			return name;
		}
		
		String getValue(){
			return value;
		}
		
		int getInt(){
			return toInt(value);
		}
		
		void setValue(String value){
			this.value=value;
		}
		
		Tree getFather(){
			return father;
		}
		
		String indent(String res,int nb){
			for(int i=0;i<nb;i++){res+="\t";}
			return res;
		}
		
		public String toData(int index){
			String res="";
			res=indent(res,index)+"<"+name+">";
			if(value==null){for(int i=0;i<trees.size();i++){res+=trees.get(i).toData(index+1);}}
			else{res+=value+"";}
			res=indent(res,index)+"</"+name+">";
			return res;
		}
		
		public String toData(){
			String res="";
			for(int i=0;i<trees.size();i++){
				res+=trees.get(i).toData(0);
			}
			return res;
		}
		
		public String toString(){
			if(value!=null){
				return name+":"+value;
			}
			else{
				String res="";
				for(int i=0;i<trees.size();i++){
					if(i+1==trees.size()){
						res+=trees.get(i).toString();
					}
					else{
						res+=trees.get(i).toString()+" ";
					}
				}
				return name+"("+res+")";
			}
		}
	}
	
	String[] loadNode(String data){
		ArrayList<String> tmp=new ArrayList<String>();
		String str="";
		int state=0;
		for(int i=0;i<data.length();i++){
			if(data.length()>i+"<?xml version=\"1.0\"?>".length()&&
				data.substring(i,i+"<?xml version=\"1.0\"?>".length()).equals("<?xml version=\"1.0\"?>")){
				i+="<?xml version=\"1.0\"?>".length();
			}
			if((state==0||state==2)&&data.charAt(i)=='<'){
				if(state==2){tmp.add(str+":data");}
				str="";
				state=1;
			}
			else if(state==1&&data.charAt(i)=='>'){
				tmp.add(str);
				str="";
				state=0;
			}
			else if(state==0&&data.charAt(i)!=' '&&data.charAt(i)!='\t'&&data.charAt(i)!='\n'){
				str+="data:";
				str+=data.charAt(i);
				state=2;
			}
			else if((state==1||state==2)&&data.charAt(i)!='\t'){
				str+=data.charAt(i);
			}
		}
		
		String[] res=new String[tmp.size()];
		for(int i=0;i<res.length;i++){
			res[i]=tmp.get(i);
		}
		return res;
	}
	
	Tree createTree(String[] data){
		Tree res=new Tree(null,"data");
		for(int i=0;i<data.length;i++){
			String[] tmp=data[i].split(":");
			if(data[i].length()>0&&data[i].charAt(0)=='/'){
				res=res.getFather();
			}
			else if(tmp.length>=3&&tmp[0].equals("data")&&tmp[tmp.length-1].equals("data")){
				String str="";
				for(int j=1;j<tmp.length-1;j++){
					str+=tmp[j];
				}
				res.setValue(str);
			}
			else{
				res.add(data[i]);
				res=res.last();
			}
		}
		return res;
	}
	
	
	XML(Documents documents,String name){
		this.documents=documents;
		this.name=name;
		//String tmp=this.documents.loadString(name);
		//if(tmp==null)
		//s{
			documents.saveString(name,"");
			String tmp=documents.loadString(name);
		//}
		String[] data=loadNode(tmp);
		tree=createTree(data);
	}
	
	boolean isChild(String name){
		return tree.getByIndex(name,0)!=null;
	}
	
	String getValue(String name){
		String res=null;
		for(int i=0;i<tree.size();i++){
			if(tree.get(i).getName().equals(name)){
				res=tree.get(i).getValue();
				break;
			}
		}
		return res;
	}
	
	void setValue(String name,String value){
		for(int i=0;i<tree.size();i++){
			if(tree.get(i).getName().equals(name)){
				tree.get(i).setValue(value);
				break;
			}
		}
	}
	
	void getChild(String name){
		getByIndex(name,0);
	}
	
	void getByIndex(String name,int index){
		if(tree.getByIndex(name,index)!=null){
			tree=tree.getByIndex(name,index);
		}
		else{
			Log.i("XML::back()","There's no index name by :"+name+","+index);
		}
	}
	
	void add(String name){
		tree.add(name);
	}
	
	void add(String name,String value){
		tree.add(name);
		tree.last().setValue(value);
	}
	
	void removeAll(String name){
		tree.removeAll(name);
	}
	
	void removeIndex(String name,int index){
		tree.removeIndex(name,index);
	}
	
	void addGetChild(String name){
		if(tree.getByIndex(name,0)!=null){
			tree=tree.getByIndex(name,0);
		}
		else{
			tree.add(name);
			tree=tree.getByIndex(name,0);
		}
	}
	
	void back(){
		if(tree.getFather()!=null){
			tree=tree.getFather();
		}
		else{
			Log.i("XML::back()","There's no father");
		}
	}
	
	public String toString(){
		return tree.toString();
	}
	
	public void print(){
		Log.i("XML::print()",toString());
	}
	
	void initTree(){
		while(!tree.getName().equals("data")){
			tree=tree.getFather();
		}
	}
	
	void save(){
		initTree();
		documents.saveString(name, tree.toData());
	}
	
	String toData(){
		initTree();
		return tree.toData();
	}

	public int getInt(String string) {
		return tree.getInt();
	}
}
