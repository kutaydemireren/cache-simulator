import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Test {

	public static void main(String[] args) throws IOException {
		/**
		 * Command Line Args:
		 *
		 * policy	type "random" to test random cache, "lru" to test LRU Cache
		 * a 		Associativity
		 * s		Cache Size
		 * b		Block Size
		 * n		History Reset Frequency
		 * f		Address Stream File
		 * d		Detailed Log
		 * debug	1 to see steps, 0 not to see.
		 */
		int a = 1, s = 512, b = 4, n = 100, debug = 0;
		Cache cache = null;
		String policy = "random", f = "addresses.txt", d = "log.txt" ;

		if(args.length > 0){
			policy = args[0];
			a = Integer.parseInt(args[1]);
			s = Integer.parseInt(args[2]);
			b = Integer.parseInt(args[3]);
			n = Integer.parseInt(args[4]);
			f = args[5];
			d = args[6];
			debug = Integer.parseInt(args[7]);
			usage(args);
		}

		//Constructing IO files.
		BufferedReader reader = null;
		PrintWriter writer = null;
		File file = new File(f);
		reader = new BufferedReader(new FileReader(file));
		String pathToWrite = "./" + d;
		writer = new PrintWriter(pathToWrite);

		if(policy.equalsIgnoreCase("random"))
			cache = new Cache(a, s, b, writer);
		else if(policy.equalsIgnoreCase("lru"))
			cache = new LRUCache(a, s, b, n, writer);


		int step = 0;

		String line;
		while ((line = reader.readLine()) != null) {
			if(step%100==0 && step!=0 && debug == 1) //step output
				cache.printState();
			String[] squareInf = line.split(" ");
			String decimalAddress = squareInf[0];
			String binaryAddress = convertToBinary(decimalAddress);
			cache.add(binaryAddress);
			step++;
		}
		System.out.println("Number of misses: " + cache.getNumberOfMiss() +
				"\nNumber of hits: " + cache.getNumberOfHit() +
				"\nNumber of write backs: " + cache.getNumberOfWriteBack());

		cache.printState();

		writer.close();
		reader.close();
	}

	public static String convertToBinary(String decimalAddress){
		int address = Integer.parseInt(decimalAddress);
		String binaryAddress = Integer.toBinaryString(address);
		binaryAddress = makeBinary32Bit(binaryAddress);
		return binaryAddress;
	}

	public static String makeBinary32Bit(String binaryAddress){
		while(binaryAddress.length()<32)
			binaryAddress = "0" + binaryAddress;
		return binaryAddress;
	}

	public static void usage(String[] args){
		if(args.length != 8 ||
				!(args[0].equals("lru") || args[0].equals("random")) ||
				Integer.parseInt(args[1]) < 1 || Integer.parseInt(args[1]) > 8 ||
				Integer.parseInt(args[2]) < 1 ||
				Integer.parseInt(args[3])<0 ||
				Integer.parseInt(args[4])<0 ||
				!(Integer.parseInt(args[7]) == 0 || Integer.parseInt(args[7]) == 1)){
			System.out.println("Given arguments are invalid: <policy> <a> <s> <b> <n> <debug> ");
			System.out.println(" policy: type of the cache, either lru or random");
			System.out.println(" a: An integer (<= 8) specifying the associativity of the cache");
			System.out.println(" s: Total size of the cache in bytes");
			System.out.println(" b: Block size of the cache in bytes");
			System.out.println(" n: History reset frequency <Integer>");
			System.out.println(" f: File name ");
			System.out.println(" d: Log file name ");
			System.out.println(" debug: 1 to see initial states of cache at some steps, 0 not to see.");
			System.exit(1);
		}
	}
}
