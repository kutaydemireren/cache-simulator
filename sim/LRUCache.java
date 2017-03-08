import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;

public class LRUCache extends Cache{

	private int n;

	private PrintWriter writer = null;
	/**
	 *
	 * @param a Associativity
	 * @param s Cache Size
	 * @param b Block Size
	 * @param n History Reset Frequency
	 *
	 * @throws FileNotFoundException (in case file is not found)
	 */
	public LRUCache(int a, int s, int b, int n, PrintWriter writer) throws FileNotFoundException {
		super(a, s, b, writer);
		this.n=n;
		this.writer = writer;
		if(!repOK()){
			writer.println("Failed at LRU cache construction");
			System.exit(1);
		}
	}

	public boolean RepOK(){
		super.repOK();
		if(n<0){
			writer.println("Invalid argument n: " + n);
			return false;
		}
		return true;
	}

	public void add(String address){
		setNumberOfAdds(getNumberOfAdds()+1);
		String tag = takeTagFromAddress(address);
		String index = takeIndexFromAddress(address);
		int indexDecimal = Integer.parseInt(index, 2);
		int set = isInTheSet(address);
		if(set!=-1){
			referenceToCache(address,set);
			writer.println("Hit \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + getCache().get(set).get(indexDecimal)[3] + "\t history bits: " + getCache().get(set).get(indexDecimal)[4]);
			setNumberOfHit(getNumberOfHit()+1);
			if(getNumberOfAdds()%5==0){
				String[] block = getCache().get(set).get(indexDecimal);
				block[1] = "1";
			}
		}else{
			set = leastRecentlyUsed(address);
			String[] block = getCache().get(set).get(indexDecimal);
			if(block[0].equals("0") && block[1].equals("0")){ //No data here.
				block[0]="1";
				block[1]="0";
				block[2]=tag;
				block[3]=takeData(address);
				referenceToCache(address,set);
				writer.println("Miss \t\t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3] + "\t history bits: " + block[4]);
				setNumberOfMiss(getNumberOfMiss()+1);
			}else if(block[0].equals("1") && block[1].equals("0") && !(block[2].equals(tag))){ //Different data in same index with dirty bit 0.
				//Do not need to write back since dirty bit is 0.
				writer.println("Miss, Replaced \t\t-> index: " + index + " tag: " + block[2] + " data: " + block[3] + "\t history bits: " + block[4]);
				block[0]="1";
				block[1]="0";
				block[2]=tag;
				block[3]=takeData(address);
				referenceToCache(address,set);
				writer.println("Miss, New \t\t\t-> index: " + index + " tag: " + tag + " data: " + block[3] + "\t history bits: " + block[4]);
				setNumberOfMiss(getNumberOfMiss()+1);

			}else if(block[0].equals("1") && block[1].equals("1") && !(block[2].equals(tag))){ //Different data in same index with dirty bit 1.
				//Write back.
				writer.println("Write Back Removed \t-> index: " + index + " tag: " + block[2] + " data: " + block[3] + "\t history bits: " + block[4]);
				block[0]="1";
				block[1]="0";
				block[2]=tag;
				block[3]=takeData(address);
				referenceToCache(address,set);
				writer.println("Write Back Added \t-> index: " + index + " tag: " + tag + " data: " + block[3] + "\t history bits: " + block[4]);
				setNumberOfWriteBack(getNumberOfWriteBack()+1);
				setNumberOfMiss(getNumberOfMiss()+1);
			}

			if(getNumberOfAdds()%5==0){
				block[1] = "1";
			}
		}
		resetTheCache();
	}

	public int leastRecentlyUsed(String address){
		int max = 0;
		int setNumber = 0;
		String index = takeIndexFromAddress(address);
		int indexDecimal = Integer.parseInt(index, 2);
		LinkedList<String> historyBits = new LinkedList<String>();
		for(int i = 0; i < getAssociativity() ; i++) { 		//Searches for least recently used block of set.
			String[] block = getCache().get(i).get(indexDecimal);
			historyBits.add(block[4]);
			if(Integer.parseInt(block[4])>max){
				max = Integer.parseInt(block[4]);
				setNumber=i;
			}
		}
		LinkedList<Integer> ties = new LinkedList<Integer>();
		for (int i = 0; i < historyBits.size(); i++) {
			if(max==Integer.parseInt(historyBits.get(i))){
				ties.add(i);
			}
		}

		if(ties.size()>1){ //More than 1 means there is a tie.
			int x = (int) (Math.random()*ties.size());
			setNumber=ties.get(x);
			return setNumber;
		}else{
			return setNumber;
		}
	}

	public void referenceToCache(String address,int set){
		String index = takeIndexFromAddress(address);
		int indexDecimal = Integer.parseInt(index, 2);
		String[] block = getCache().get(set).get(indexDecimal);
		block[4]="00";

		//Updates with respect to the state table given.
		for (int i = 1; i < getAssociativity() ; i++) {
			int currentSet = (set+i) % getAssociativity();
			String[] blocks = getCache().get(currentSet).get(indexDecimal);
			if(blocks[0].equals("1")){ //Valid
				if(blocks[4].equals("00")){
					blocks[4]="01";
				}else if(blocks[4].equals("01")){
					blocks[4]="11";
				}else if(blocks[4].equals("10")){
					blocks[4]="11";
				}else if(blocks[4].equals("11")){
					blocks[4]="11";
				}
			}
		}

	}

	public void resetTheCache(){
		if(getNumberOfAdds()%n==0){
			for (int i = 0; i < getAssociativity(); i++) {
				for(int j = 0; j < getNumBlocks(); j++) {
					String[] block = getCache().get(i).get(j);
					if(block[0].equals("1")){ //Valid
						if(block[4].equals("00")){
							block[4]="00";
						}else if(block[4].equals("01")){
							block[4]="00";
						}else if(block[4].equals("10")){
							block[4]="00";
						}else if(block[4].equals("11")){
							block[4]="10";
						}
					}
				}
			}
		}
	}

	public void printState(){
		writer.println("----------------------");
		writer.println("Printing LRU Cache State....");
		int count = 0;
		for (int index = 0; index < getNumBlocks(); index++) {
			writer.print("Index #" + index + ": ");
			for (int set = 0; set < getAssociativity(); set++) {
				LinkedList<String[]> list = getCache().get(set);
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
				writer.print(block[0] + " " + block[1] + " " + tag + " " + data + "\t" + block[4] + "\t");
			}
			writer.println();
		}
		writer.println("# non-empty indices: " + count);
		writer.println("----------------------");
	}
}
