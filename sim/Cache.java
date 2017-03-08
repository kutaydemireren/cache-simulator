import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;

public class Cache {
	private int a;
	private int s;
	private int b;
	private int indexSize;
	private int numBlocks;
	private int tagSize;
	private int numberOfMiss;
	private int numberOfHit;
	private int numberOfWriteBack;
	private int numberOfAdds;
	private int numBlocksInRow;
	private int blockOffsetsize ;


	private final int validBitSize = 1;
	private final int dirtyBitSize = 1;

	private final int byteOffsetSize = 2;

	private PrintWriter writer = null;

	private HashMap<Integer, LinkedList<String[]>> cache;

	/**
	 *
	 * @param a Associativity
	 * @param s Cache Size
	 * @param b Block Size
	 *
	 * @throws FileNotFoundException (in case file is not found)
	 */
	public Cache(int a, int s, int b, PrintWriter writer) throws FileNotFoundException{
		this.a = a;
		this.s = s;
		this.b = b;
		this.writer = writer;
		numberOfMiss=0;
		numberOfHit=0;
		numberOfWriteBack=0;
		numberOfAdds=0;
		numBlocksInRow=0;

		numBlocks = s/(a*b);
		numBlocksInRow = b/4;
		blockOffsetsize = (int)(Math.log(numBlocksInRow)/Math.log(2));

		if((Math.log(numBlocksInRow)/Math.log(2))!=(int)(Math.log(numBlocksInRow)/Math.log(2)))
			blockOffsetsize++;

		indexSize = (int)(Math.log(numBlocks)/Math.log(2));

		tagSize = 32 - indexSize - (byteOffsetSize + blockOffsetsize); //Address is assumed to be 32 bits.
		if(!repOK()){
			writer.println("Failed at cache construction");
			System.exit(1);
		}

		//Cache construction
		cache = new HashMap<Integer, LinkedList<String[]>>();
		for (int i = 0; i < a; i++) {
			LinkedList<String[]> oneSet = new LinkedList<String[]>();
			for(int j = 0; j < numBlocks; j++) {
				String[] block = new String[]{"0","0", null, null, "11"}; //Initialized all bits to be 0 in order valid, dirty, tag, data and history bits for using in LRUCache.
				//Valid = 0, Dirty = 0, Both are zero, because we did not add anything, yet.
				//Tag = null (because it is empty), Data = null (because it is empty). History bits are 11, initially.
				oneSet.add(block);
			}
			cache.put(i, oneSet);
		}
	}

	public boolean repOK(){
		if(a>8 || a<=0){
			writer.println("Invalid argument a: " + a);
			return false;
		}else if(s<=0 || !((int)(Math.log(s)/Math.log(2)) ==  Math.log(s)/Math.log(2))){ //log 2 to s
			writer.println("Invalid argument s: " + s);
			return false;
		}else if(b<=0 || b%4!=0){
			writer.println("Invalid argument b: " + b);
			return false;
		}else if(numBlocks<=0){
			writer.println("Invalid argument numBlocks: " + numBlocks);
			return false;
		}else if(indexSize<=0){
			writer.println("Invalid argument indexSize: " + indexSize);
			return false;
		}else if(tagSize<=0 || tagSize>=32){
			writer.println("Invalid argument tagSize: " + tagSize);
			return false;
		}
		return true;
	}

	public int getAssociativity() {
		return a;
	}

	public void setAssociativity(int a) {
		this.a = a;
	}

	public int getCacheSize() {
		return s;
	}

	public void setCacheSize(int s) {
		this.s = s;
	}

	public int getBlockSize() {
		return b;
	}

	public void setBlockSize(int b) {
		this.b = b;
	}

	public int getNumBlocksInRow() {
		return numBlocksInRow;
	}

	public int getBlockOffsetsize() {
		return blockOffsetsize;
	}

	public int getNumberOfMiss() {
		return numberOfMiss;
	}

	public void setNumberOfMiss(int numberOfMiss) {
		this.numberOfMiss = numberOfMiss;
	}

	public int getNumberOfHit() {
		return numberOfHit;
	}

	public void setNumberOfHit(int numberOfHit) {
		this.numberOfHit = numberOfHit;
	}

	public int getNumberOfWriteBack() {
		return numberOfWriteBack;
	}

	public void setNumberOfWriteBack(int numberOfWriteBack) {
		this.numberOfWriteBack = numberOfWriteBack;
	}

	public int getNumberOfAdds() {
		return numberOfAdds;
	}

	public void setNumberOfAdds(int numberOfAdds) {
		this.numberOfAdds = numberOfAdds;
	}

	public HashMap<Integer, LinkedList<String[]>> getCache() {
		return cache;
	}

	public int getNumBlocks() {
		return numBlocks;
	}

	public void add(String address){
		numberOfAdds++;
		String tag = takeTagFromAddress(address);
		String index = takeIndexFromAddress(address);
		int indexDecimal = Integer.parseInt(index, 2);
		int set = isInTheSet(address);
		if(set!=-1){
			writer.println("Hit \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + cache.get(set).get(indexDecimal)[3]);
			numberOfHit++;
			if(numberOfAdds%5==0){
				String[] block = cache.get(set).get(indexDecimal);
				block[1] = "1";
			}
		}else{
			set = (int)(Math.random()*a);
			String[] block = cache.get(set).get(indexDecimal);
			if(block[0].equals("0") && block[1].equals("0")){ //No data here.
				block[0]="1";
				block[1]="0";
				block[2]=tag;
				block[3]=takeData(address);
				writer.println("Miss \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3]);
				numberOfMiss++;
			}else if(block[0].equals("1") && block[1].equals("0") && !(block[2].equals(tag))){ //Different data in same index with dirty bit 0.
				//Do not need to write back since dirty bit is 0.
				int tempSet = set;
				for(int i = 0; i < a; i++) { //Searches for empty blocks of set.
					set = (set+1)%a;
					block = cache.get(set).get(indexDecimal);
					if(block[0].equals("0")) //Found.
						break;
				}
				if(set==tempSet){
					writer.println("Miss, Replaced \t\t-> index: " + index + " tag: " + block[2] + " data: " + block[3]);
					block[0]="1";
					block[1]="0";
					block[2]=tag;
					block[3]=takeData(address);
					writer.println("Miss, New \t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3]);
					numberOfMiss++;
				}else{
					block = cache.get(set).get(indexDecimal);
					block[0]="1";
					block[1]="0";
					block[2]=tag;
					block[3]=takeData(address);
					writer.println("Miss \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3]);
					numberOfMiss++;
				}
			}else if(block[0].equals("1") && block[1].equals("1") && !(block[2].equals(tag))){ //Different data in same index with dirty bit 1.
				//Write back.
				int tempSet = set;
				for(int i = 0; i < a; i++) { //Searches for empty blocks of set.
					set = (set+1)%a;
					block = cache.get(set).get(indexDecimal);
					if(block[0].equals("0")) //Found.
						break;
				}
				if(set == tempSet){
					writer.println("Write Back Removed \t-> index: " + index + " tag: " + block[2] + " data: " + block[3]);
					block[0]="1";
					block[1]="0";
					block[2]=tag;
					block[3]=takeData(address);
					writer.println("Write Back Added \t-> index: " + index + " tag: " + tag + " data: " + block[3]);
					numberOfWriteBack++;
					numberOfMiss++;
				}else{
					block = cache.get(set).get(indexDecimal);
					block[0]="1";
					block[1]="0";
					block[2]=tag;
					block[3]=takeData(address);
					writer.println("Miss \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3]);
					numberOfMiss++;
				}
			}
			if(numberOfAdds%5==0){
				block[1] = "1";
			}
		}
	}


	public String takeTagFromAddress(String address){
		String tagFromAddress = address.substring(0, tagSize);
		return tagFromAddress;
	}

	public String takeIndexFromAddress(String address){
		String indexFromAddress = address.substring(tagSize, tagSize+indexSize);
		return indexFromAddress;
	}

	public int isInTheSet(String searchingAddress){ //Returns set if it is found, o.w. returns -1
		String index = takeIndexFromAddress(searchingAddress);
		int indexDecimal = Integer.parseInt(index, 2);
		String tag = takeTagFromAddress(searchingAddress);

		for (int i = 0; i < a; i++) {
			String[] block = cache.get(i).get(indexDecimal);
			if(block[0].equals("1") && block[2].equals(tag)){
				return i; //Found and return that set.
			}
		}
		return -1; //Couldn't find the set.
	}

	public String takeData(String address){
		String data="Mem(";
		int addressDecimal = Integer.parseInt(address, 2);
		addressDecimal -= addressDecimal% numBlocksInRow;
		for (int i = 0; i < numBlocksInRow-1; i++) {
			data += (addressDecimal+i) + " ";
		}
		data += (addressDecimal + numBlocksInRow-1);
		data += ")";
		return data;
	}

	public void printState(){
		writer.println("----------------------");
		writer.println("Printing Cache State....");
		int count = 0;
		for (int index = 0; index < numBlocks; index++) {
			writer.print("Index #" + index + ": ");
			for (int set = 0; set < a; set++) {
				LinkedList<String[]> list = cache.get(set);
				String[] block = list.get(index);
				String tag, data;
				if(block[2]==null)
					tag = "-";
				else
					tag = block[2];
				if(block[3]==null)
					data = "-";
				else{
					data = block[3];
					count++;
				}
				writer.print(block[0] + " " + block[1] + " " + tag + " " + data + "\t");
			}
			writer.println();
		}
		writer.println("# non-empty indices: " + count);
		writer.println("----------------------");
	}
}
