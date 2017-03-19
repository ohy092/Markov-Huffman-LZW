import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

public class Mandatory1 {

	/*
	 * These are static file-/directory paths etc. that could be changed
	 * manually if so desired
	 */
	static boolean skipAll = false; // Skip all user queries
	static String lzwFileExtension = "lzw"; // File extension for LZW files
	static String huffFileExtension = "huff"; // Extension for Huffman files
	static String cacheFolder = "cache"; // Folder where source file is cached
	static String markovFolder = "markovGenerated";
	static String lzwCpFolder = "lzw\\compressed";
	static String lzwDecFolder = "lzw\\decompressed";
	static String huffEncFolder = "lzw\\huffman\\encoded";
	static String huffDecFolder = "lzw\\huffman\\decoded";
	static String lzwHuffDecFolder = "lzw\\huffmanDecompressed";
	static String localRootPath = "C:\\inf144\\ohy092\\mandatory1\\"; // root

	/*
	 * This means we start on byte 0. I had some problems in regard to reading
	 * files written this way (due to the first few bytes in unicode being
	 * special line feed/backspace characters and the like) before I switched to
	 * byte-level reading. This could be set to any 16 bit value, but probably
	 * should stay at 0.
	 */
	static int lzwStartIndex = 0;

	public static void main(String[] args) {
		Mandatory1.kbd = new Scanner(System.in);
		Mandatory1.m1 = new Mandatory1();
		Mandatory1.rw = Mandatory1.m1.new ReadWrite();
		try {
			Mandatory1.uh = Mandatory1.m1.new UserHandling();
			if (Mandatory1.uh.getURLandLocalPath()) {
				return;
			}
			try {
				createLocalCache();
			} catch (IOException e) {
				System.out.println("Could not create local cache. Exiting.");
				return;
			}
			Mandatory1.mg = Mandatory1.m1.new MarkovGenerator();
			if (Mandatory1.uh.abortGenerateMarkov()) {
				return;
			}
			try {

				Mandatory1.mg.generate();
				if (Mandatory1.uh.abortCompressLZW()) {
					return;
				}
				Mandatory1.lzw = Mandatory1.m1.new LZW();
				Mandatory1.lzw.compress();
				if (Mandatory1.uh.abortDecompressLZW()) {
					return;
				}
				Mandatory1.lzw.decompress(false); // Without Huffman
				if (Mandatory1.uh.abortHuffmanEncode()) {
					return;
				}
				Mandatory1.huff = Mandatory1.m1.new Huffman();
				Mandatory1.huff.encode();
				try {
					if (Mandatory1.uh.abortHuffmanDecode()) {
						return;
					}
					Mandatory1.huff.decode();
					if (Mandatory1.uh.abortLZWHuff()) {
						return;
					}
					Mandatory1.lzw = Mandatory1.m1.new LZW();
					Mandatory1.lzw.decompress(true);
				} finally {
					/*
					 * This can safely be displayed regardless of whether or not
					 * the user wants to do the final decompression.
					 */
					Mandatory1.uh.displayCompressionStatistics();
				}
			} catch (IOException e) {
				System.out
						.println("There was an error reading from or writing to file. Exiting.");
				return;
			}
		} finally {
			System.out.println();
			System.out.println("Goodbye!");
			Mandatory1.kbd.close();
		}
	}

	static void createLocalCache() throws IOException {
		System.out.println();
		if (!Mandatory1.rw
				.fileExists(Mandatory1.localRootPath + Mandatory1.cacheFolder
						+ "\\" + Mandatory1.localCacheFileName)) {
			System.out.println("Downloading file " + Mandatory1.urlToFile);
			String readFile = Mandatory1.rw
					.readFileFromURL(Mandatory1.urlToFile)[Mandatory1.textIsOnLineNumber - 1];
			Mandatory1.rw.writeLine(readFile, Mandatory1.localRootPath
					+ "cache", Mandatory1.localCacheFileName);
			System.out.println("Created local cache in "
					+ Mandatory1.localRootPath + Mandatory1.cacheFolder + "\\"
					+ Mandatory1.localCacheFileName);
		} else {
			System.out.println("Local cache in " + Mandatory1.localRootPath
					+ Mandatory1.cacheFolder + "\\"
					+ Mandatory1.localCacheFileName + " already exists.");
		}
	}

	public class ReadWrite {

		public byte[] readBytes(String fileName) throws IOException {
			File file = new File(fileName);
			byte[] res = new byte[(int) file.length()];
			InputStream input = null;
			int numberOfBytes = 0;
			try {
				input = new BufferedInputStream(new FileInputStream(file));
				while (numberOfBytes < res.length) {
					int bytesRemaining = res.length - numberOfBytes;
					int newBytes = input.read(res, numberOfBytes,
							bytesRemaining);
					if (newBytes > 0) {
						numberOfBytes += newBytes;
					}
				}
			} finally {
				input.close();
			}
			return res;
		}

		public void writeBytes(byte[] byteArray, String dir, String fileName)
				throws IOException {
			OutputStream output = null;
			try {
				(new File(dir + "\\")).mkdirs();
				File file = new File(dir + "\\" + fileName);
				file.createNewFile();
				output = new BufferedOutputStream(
						new FileOutputStream(fileName));
				output.write(byteArray);
			} finally {
				output.close();
			}
		}

		public String readLines(String location) throws IOException {
			String res = "";
			InputStream ips = null;
			BufferedReader br = null;
			try {
				ips = new FileInputStream(location);
				InputStreamReader ipsr = new InputStreamReader(ips);
				br = new BufferedReader(ipsr);
				String line;
				while ((line = br.readLine()) != null) {
					res += line + '\n';
				}

			} finally {
				br.close();
				ips.close();
			}
			return res;
		}

		public void writeLine(String toWrite, String dir, String fileName)
				throws IOException {
			FileWriter fw = null;

			try {
				(new File(dir + "\\")).mkdirs();
				File file = new File(dir + "\\" + fileName);
				file.createNewFile();
				fw = new FileWriter(file);
				fw.write(toWrite);
			} finally {
				fw.close();
			}
		}

		public String[] readFileFromURL(String location) throws IOException {
			ArrayList<String> lines = new ArrayList<String>();
			URL path = new URL(location);
			BufferedReader in = null;
			try {
				in = new BufferedReader(
						new InputStreamReader(path.openStream()));
				String line;
				// We read all lines until we find the specified line
				while ((line = in.readLine()) != null) {
					lines.add(line);
				}
			} finally {
				in.close();
			}
			String[] res = new String[lines.size()];
			for (int i = 0; i < lines.size(); i++) {
				res[i] = lines.get(i);
			}
			return res;
		}

		public boolean fileExists(String fileName) {
			File file = new File(fileName);
			return (file.exists() && !file.isDirectory());
		}

		public String stripLineFeedsAndTrim(String s) {
			String res = s.replaceAll("\n", "");
			return res.trim();
		}

		public String bytesToString(byte[] bytes)
				throws UnsupportedEncodingException {
			return new String(bytes, "UTF-8");
		}
	}

	public class UserHandling {

		public boolean getYesNo() {
			if (Mandatory1.skipAll) {
				return false;
			}
			System.out.print("[y/n] ");
			String reply = Mandatory1.kbd.nextLine().toLowerCase();
			if (reply.equals("y")) {
				return true;
			}
			if (reply.equals("n")) {
				return false;
			}
			return getYesNo();
		}

		public int getIntFromUser() {
			System.out.print("Please type an integer: ");
			String reply = Mandatory1.kbd.nextLine();
			try {
				int ret = Integer.parseInt(reply);
				return ret;
			} catch (Exception e) {
				return getIntFromUser();
			}
		}

		public boolean getURLandLocalPath() {
			System.out
			.println("All necessary direcatories and subdirectories will be created.\nAll necessary files within these directories will be created.");
			System.out
			.println("Any pre-existing files matching the full path of any new file \nto be created by this program will be overwritten.");
			System.out
			.println("The local root folder is currently "
					+ Mandatory1.localRootPath
					+ ", \nbut this can be changed in a moment. The subdirectories within \nthe root directory, whose files may be overwritten, are\n");
			System.out.println(Mandatory1.cacheFolder + "\\\n"
					+ Mandatory1.markovFolder + "\\\n" + Mandatory1.lzwCpFolder
					+ "\\\n" + Mandatory1.lzwDecFolder + "\\\n"
					+ Mandatory1.lzwHuffDecFolder + "\\\n"
					+ Mandatory1.huffEncFolder + "\\\n"
					+ Mandatory1.huffDecFolder + "\\\n");

			System.out.print("Abort? ");
			if (getYesNo()) {
				return true;
			}

			System.out.print("Change root directory? ");
			if (getYesNo()) {
				System.out.println("Please type your desired new local path: ");
				Mandatory1.localRootPath = Mandatory1.kbd.nextLine();
				Mandatory1.localRootPath = formatDirectoryFromUser(Mandatory1.localRootPath);
				System.out.println();
			}

			System.out.print("Source text will be downloaded from\n"
					+ Mandatory1.urlToFile + "\nChange URL? ");
			if (getYesNo()) {
				System.out.println("Please type your desired new URL: ");
				Mandatory1.urlToFile = Mandatory1.kbd.nextLine();
				Mandatory1.urlToFile.replaceAll("\\", "/");
			}

			System.out.println();
			System.out
			.print("The source text is supposedly contained within line number "
					+ Mandatory1.textIsOnLineNumber
					+ "\nof the HTML source code.\nChange this? ");
			if (getYesNo()) {
				Mandatory1.textIsOnLineNumber = getIntFromUser();
			}

			Mandatory1.localCacheFileName = Mandatory1.urlToFile.split("/")[Mandatory1.urlToFile
			                                                                .split("/").length - 1];
			String[] split = Mandatory1.localCacheFileName.split("\\.");
			String newFileName = "";
			for (int i = 0; i < split.length - 1; i++) {
				newFileName += split[i] + ".";
			}
			if (newFileName.equals("")) {
				newFileName = Mandatory1.localCacheFileName + ".";
			}
			Mandatory1.localCacheFileName = newFileName + "txt";

			System.out.println();
			System.out.print("The file in\n" + Mandatory1.urlToFile
					+ "\nwill be downloaded and stored into\n"
					+ Mandatory1.localRootPath + Mandatory1.cacheFolder + "\\"
					+ Mandatory1.localCacheFileName + "\nAbort? ");
			return getYesNo();
		}

		public String formatDirectoryFromUser(String dir) {
			dir.replaceAll("/", "\\");
			if (dir.charAt(dir.length() - 1) != '\\') {
				dir += "\\";
			}
			return dir;
		}

		public boolean abortGenerateMarkov() {
			System.out.println();
			System.out
					.print("Proceeding to generate random Markov texts.\nA total of 400 new text files will be added to subdirectories\nwithin "
							+ Mandatory1.localRootPath
							+ Mandatory1.markovFolder + "\\\nAbort? ");
			return getYesNo();
		}

		public boolean abortCompressLZW() {
			System.out.println();
			System.out
					.print("Proceeding to LZW-compress the previously generated Markov texts.\nThis will add another 400 files to subdirectories within "
							+ Mandatory1.localRootPath
							+ Mandatory1.lzwCpFolder
							+ "\\\nAbort? ");
			return getYesNo();
		}

		public boolean abortDecompressLZW() {
			System.out.println();
			System.out
					.print("Proceeding to LZW-decompress the previously compressed Markov texts.\nThis will add another 400 files to subdirectories within "
							+ Mandatory1.localRootPath
							+ Mandatory1.lzwDecFolder + "\\\nAbort? ");
			return getYesNo();
		}

		public boolean abortHuffmanEncode() {
			System.out.println();
			System.out
					.print("Proceeding to Huffman-encode the previously compressed Markov texts.\nThis will add another 400 files to subdirectories within "
							+ Mandatory1.localRootPath
							+ Mandatory1.huffEncFolder + "\\\nAbort? ");
			return getYesNo();
		}

		public boolean abortHuffmanDecode() {
			System.out.println();
			System.out
					.print("Proceeding to Huffman-decode the previously encoded LZW-compressed texts.\nThis will add another 400 files to subdirectories within "
							+ Mandatory1.localRootPath
							+ Mandatory1.huffDecFolder + "\\\nAbort? ");
			return getYesNo();
		}

		public boolean abortLZWHuff() {
			System.out.println();
			System.out
					.print("Proceeding to LZW-decompress the previously decoded Huffman-encodings.\nThis will add another 400 files to subdirectories within "
							+ Mandatory1.localRootPath
							+ Mandatory1.lzwHuffDecFolder + "\\\nAbort? ");
			return getYesNo();
		}

		public void displayCompressionStatistics() {
			long fullSize = calculateSize(Mandatory1.sizeAfterGenerating);
			System.out.println();
			System.out
					.println("Displaying compression ratios relative to plaintext form. Higher is better.");
			int lineLength = 20;
			displayIndividualStatistics(lineLength,
					Mandatory1.sizeAfterGenerating, fullSize,
					"Before compression");
			displayIndividualStatistics(lineLength,
					Mandatory1.sizeAfterLZWCompression, fullSize,
					"After LZW compression");
			displayIndividualStatistics(lineLength,
					Mandatory1.sizeAfterHuffmanCompression, fullSize,
					"After LZW and Huffman compression");
			printLine(lineLength);
		}

		public void displayIndividualStatistics(int lineLength, int[] array,
				long fullSize, String firstLine) {
			printLine(lineLength);
			System.out.println(firstLine + ":");

			for (int i = -1; i < array.length; i++) {
				long size;
				double ratio;
				if (i == -1) {
					size = calculateSize(array);
					ratio = (double) fullSize;
					System.out.print("Total");
				} else {
					size = array[i];
					ratio = (double) Mandatory1.sizeAfterGenerating[i];
					System.out.print("Order " + i);
				}
				ratio = 100.0 - (100.0 * (Math.max(size, 1) / ratio));
				System.out.println(" size: " + size + ", Compression ratio "
						+ String.format("%.1f", ratio) + "%");

			}
		}

		public long calculateSize(int[] array) {
			long sum = 0;
			for (int i : array) {
				sum += i;
			}
			return sum;
		}

		public void printLine(int length) {
			for (int i = 0; i < length; i++) {
				System.out.print("-");
			}
			System.out.println();
		}
	}

	public class MarkovGenerator {

		// Our chain/statistics generated from source file
		Map<String, ArrayList<Character>> model;

		// All recorded start-prefixes, i.e. all prefixes that has been recorded
		// starting with a capital letter
		ArrayList<String> startPrefixes;

		// The length of our input (number of characters)
		int length;
		Random r = new Random();

		void generate() throws IOException {
			System.out.println();
			for (int order = 0; order <= 3; order++) {
				System.out.print("Generating order " + order + ": ");
				for (int i = 1; i <= 100; i++) {
					this.model = new HashMap<String, ArrayList<Character>>();
					this.startPrefixes = new ArrayList<String>();
					markovProcess(order, i);
					System.out.print(i + " ");
				}
				System.out.println();
			}
			System.out.println("Markov generating done!");
		}

		public void markovProcess(int orderOfApproximation, int number)
				throws IOException {
			String filePath = Mandatory1.localRootPath + Mandatory1.cacheFolder
					+ "\\" + Mandatory1.localCacheFileName;
			String readFile = Mandatory1.rw.readLines(filePath);
			readFile = Mandatory1.rw.stripLineFeedsAndTrim(readFile);
			char[] charactersOrdered = readFile.toCharArray();
			this.length = charactersOrdered.length;
			createModel(charactersOrdered, orderOfApproximation);
			String s = generateText(orderOfApproximation);
			Mandatory1.sizeAfterGenerating[orderOfApproximation] += s.length();
			Mandatory1.rw.writeLine(s, Mandatory1.localRootPath
					+ Mandatory1.markovFolder + "\\order"
					+ orderOfApproximation, "markov" + number + ".txt");
		}

		public String generateText(int orderOfApproximation) {
			String generated = "";
			if (orderOfApproximation == 0) {
				ArrayList<Character> suffix = this.model.get("");
				for (int i = 0; i < this.length; i++) {
					generated += suffix.get(this.r.nextInt(suffix.size()));
				}
			} else {
				// We retrieve a valid prefix to start with
				int selectInitialPrefix = this.r.nextInt(this.startPrefixes
						.size());
				generated = this.startPrefixes.get(selectInitialPrefix);

				/*
				 * Then we append this string with a valid "next character"
				 * until we reach our desired length
				 */
				while (generated.length() < this.length) {
					String prefix = generated.substring(generated.length()
							- orderOfApproximation);
					generated += nextChar(prefix);
				}
			}
			return generated;
		}

		public char nextChar(String prefix) {
			ArrayList<Character> suffix = this.model.get(prefix);

			/*
			 * If we have no recording of the given prefix - which absolutely
			 * should not happen - the preceding code will return exceptions.
			 * 
			 * As no non-recorded prefixes should be generated by the process in
			 * generateText, I reckon this will not be a problem.
			 * 
			 * I considered handling this by returning a random character based
			 * on their individual (independent) statistics, but that would
			 * break the semantics here.
			 */
			int selectNextChar = this.r.nextInt(suffix.size());
			return suffix.get(selectNextChar);
		}

		public void createModel(char[] characters, int orderOfApproximation) {
			if (orderOfApproximation == 0) {
				this.model.put("", toLowerCase(characters));
			} else {
				for (int i = 0; i + orderOfApproximation < characters.length; i++) {
					String prefix = "";
					for (int j = i; j < i + orderOfApproximation; j++) {
						prefix += characters[j];
					}
					/*
					 * If the prefix starts with an upper case letter we add it
					 * to our list of possible starting prefixes
					 */
					if (prefix.charAt(0) != toLowerCase(prefix.charAt(0))) {
						prefix = prefix.toLowerCase();
						this.startPrefixes.add(prefix);
					}
					prefix = prefix.toLowerCase();
					ArrayList<Character> suffix = this.model.get(prefix);
					if (suffix == null) {
						suffix = new ArrayList<Character>();
					}
					suffix.add(toLowerCase(characters[i + orderOfApproximation]));
					this.model.put(prefix, suffix);
				}
			}
		}

		public ArrayList<Character> toLowerCase(char[] c) {
			ArrayList<Character> res = new ArrayList<Character>();
			for (int i = 0; i < c.length; i++) {
				res.add(toLowerCase(c[i]));
			}
			return res;
		}

		public char toLowerCase(char c) {
			return ("" + c).toLowerCase().charAt(0);
		}
	}

	public class LZW {

		public void compress() throws IOException {
			LZWCompression c = new LZWCompression();
			c.compress();
		}

		public void decompress(boolean huffman) throws IOException {
			LZWDecompression d = new LZWDecompression();
			d.decompress(huffman);
		}

		public class LZWCompression {
			public HashMap<String, Character> dict;
			public int lastIndex;

			public void compress() throws IOException {
				System.out.println();
				for (int order = 0; order <= 3; order++) {
					System.out.print("Compressing order " + order + ": ");
					for (int i = 1; i <= 100; i++) {
						String readPath = Mandatory1.localRootPath
								+ Mandatory1.markovFolder + "\\order" + order
								+ "\\markov" + i + ".txt";
						String readFile = Mandatory1.rw.readLines(readPath);
						readFile = Mandatory1.rw
								.stripLineFeedsAndTrim(readFile);
						String cp = compressedString(readFile);
						Mandatory1.sizeAfterLZWCompression[order] += cp
								.length();
						String writeDir = Mandatory1.localRootPath
								+ Mandatory1.lzwCpFolder + "\\order" + order;
						Mandatory1.rw.writeLine(cp, writeDir, "compressed" + i
								+ "." + Mandatory1.lzwFileExtension);
						System.out.print(i + " ");
					}
					System.out.println();
				}
				System.out.println("Compression done!");
			}

			public String compressedString(String s) {
				this.dict = new HashMap<String, Character>();
				this.lastIndex = Mandatory1.lzwStartIndex;
				initiateCompressionDictionary();
				String compressed = "";
				while (s.length() > 1) {
					String currentSymbol = findLargestSymbolStartingIn(s);
					s = s.substring(currentSymbol.length());
					String nextSymbol = findLargestSymbolStartingIn(s);
					addToDict(currentSymbol + nextSymbol);
					compressed += "" + getFromDict(currentSymbol);
				}
				if (s.length() > 0) {
					compressed += getFromDict(s);
				}
				return compressed;
			}

			/*
			 * A "symbol" here is actually a substring of s, or more precisely:
			 * the longest substring starting in index 0 for which we currently
			 * have a recording.
			 */
			public String findLargestSymbolStartingIn(String s) {
				if (s.length() < 2) {
					return s;
				}
				/*
				 * Actually, since we have already pre-computed all strings of
				 * length 1, we need only consider strings of length 2 and
				 * above.
				 */
				int index = 2;
				String symbol = s.substring(0, index);
				/*
				 * We keep increasing the length of our symbol until we find a
				 * symbol that has not been recorded yet
				 */
				while (isInDictionary(symbol) && index++ < s.length()) {
					symbol = s.substring(0, index);
				}
				/*
				 * We return the previous symbol, i.e. the longest symbol we
				 * have a recording of that matches the start of the string s
				 */
				return symbol.substring(0, symbol.length() - 1);
			}

			public boolean isInDictionary(String check) {
				return getFromDict(check) != null;
			}

			public void initiateCompressionDictionary() {
				char[] chars = new char[30];
				for (int i = 0; i < 26; i++) {
					chars[i] = (char) (0 + i + 'a');
				}
				chars[26] = 'æ';
				chars[27] = 'ø';
				chars[28] = 'å';
				chars[29] = ' ';

				/*
				 * I choose to map the char sequences i record to new char
				 * values. This makes sense to me, as it allows for 2^16
				 * possible entries in our dictionary, along with the fact that
				 * a String (which is what we also compress into) consist of a
				 * sequence of characters - although a lot of the characters
				 * this string contains will look random-like in clear text.
				 */
				for (int i = 0; i < chars.length; i++) {
					addToDict("" + chars[i]);
				}
			}

			public String mapToLowerIndex(String s) {
				String lower = "";
				for (int i = 0; i < s.length(); i++) {
					lower += mapToLowerIndex(s.charAt(i));
				}
				return lower;
			}

			public Character mapToLowerIndex(char c) {
				for (int i = 0 + 'a'; i < 'z'; i++) {
					if (c == (char) i) {
						return (char) (i - 'a');
					}
				}
				if (c == 'æ') {
					return (char) 'z' - 'a' + 1;
				}
				if (c == 'ø') {
					return (char) 'z' - 'a' + 2;
				}
				if (c == 'å') {
					return (char) 'z' - 'a' + 3;
				}
				if (c == ' ') {
					return (char) 'z' - 'a' + 4;
				}
				return null;
			}

			public void addToDict(String add) {
				this.dict.put(mapToLowerIndex(add), (char) (this.lastIndex++));
			}

			public Character getFromDict(String get) {
				return this.dict.get(mapToLowerIndex(get));
			}
		}

		public class LZWDecompression {
			public HashMap<Character, String> dict;
			public int lastIndex;

			public void decompress(boolean huffman) throws IOException {
				System.out.println();
				for (int order = 0; order <= 3; order++) {
					System.out.print("Decompressing order " + order + ": ");
					for (int i = 1; i <= 100; i++) {
						String readPath = Mandatory1.localRootPath
								+ Mandatory1.lzwCpFolder + "\\order" + order
								+ "\\compressed" + i + "."
								+ Mandatory1.lzwFileExtension;
						if (huffman) {
							readPath = Mandatory1.localRootPath
									+ Mandatory1.huffDecFolder + "\\order"
									+ order + "\\decoded" + i + "."
									+ Mandatory1.lzwFileExtension;
						}
						byte[] readBytes = Mandatory1.rw.readBytes(readPath);
						String readFile = Mandatory1.rw
								.bytesToString(readBytes);
						String dec = decompressedString(readFile);
						String writeDir = Mandatory1.localRootPath
								+ Mandatory1.lzwDecFolder + "\\order" + order;
						if (huffman) {
							writeDir = Mandatory1.localRootPath
									+ Mandatory1.lzwHuffDecFolder + "\\order"
									+ order;
						}
						Mandatory1.rw.writeLine(dec, writeDir, "huffmanEncoded"
								+ i + ".txt");
						System.out.print(i + " ");
					}
					System.out.println();
				}
				System.out.println("Decompression done!");
			}

			public String decompressedString(String s) {
				this.dict = new HashMap<Character, String>();
				this.lastIndex = Mandatory1.lzwStartIndex;
				initiateDecompressionDictionary();
				String decompressed = "";
				for (int i = 0; i < s.length() - 1; i++) {
					char currentSymbol = s.charAt(i);
					String current = this.dict.get(currentSymbol);
					char nextSymbol = s.charAt(i + 1);
					String next = this.dict.get(nextSymbol);
					addToDict(current + next);
					decompressed += current;
				}
				return decompressed;
			}

			public boolean isInDictionary(char check) {
				return getFromDict(check) != null;
			}

			public void initiateDecompressionDictionary() {
				char[] chars = new char[30];
				for (int i = 0; i < 26; i++) {
					chars[i] = (char) (0 + i + 'a');
				}
				chars[26] = 'æ';
				chars[27] = 'ø';
				chars[28] = 'å';
				chars[29] = ' ';

				/*
				 * I choose to map the char sequences i record to new char
				 * values. This makes sense to me, as it allows for 2^16
				 * possible entries in our dictionary, along with the fact that
				 * a String (which is what we also compress into) consist of a
				 * sequence of characters - although a lot of the characters
				 * this string contains will look random-like in clear text.
				 */
				for (int i = 0; i < chars.length; i++) {
					addToDict("" + chars[i]);
				}
			}

			public Character mapBack(char c) {
				for (int i = 0; i < 'z' - 'a'; i++) {
					if (c == (char) i) {
						return (char) (i + 'a');
					}
				}
				if (c == (char) ('z' - 'a' + 1)) {
					return 'æ';
				}
				if (c == (char) ('z' - 'a' + 2)) {
					return 'ø';
				}
				if (c == (char) ('z' - 'a' + 3)) {
					return 'å';
				}
				if (c == (char) ('z' - 'a' + 4)) {
					return ' ';
				}
				return null;
			}

			public void addToDict(String add) {
				this.dict.put((char) (this.lastIndex++), add);
			}

			public String getFromDict(char get) {
				return this.dict.get(mapBack(get));
			}
		}
	}

	public class Huffman {

		public void encode() throws IOException {
			System.out.println();
			for (int order = 0; order <= 3; order++) {
				System.out.print("Encoding order " + order + ": ");
				for (int i = 1; i <= 100; i++) {
					String readPath = Mandatory1.localRootPath
							+ Mandatory1.lzwCpFolder + "\\order" + order
							+ "\\compressed" + i + "."
							+ Mandatory1.lzwFileExtension;
					byte[] readBytes = Mandatory1.rw.readBytes(readPath);
					String readFile = Mandatory1.rw.bytesToString(readBytes);
					HuffmanEncode hfe = new HuffmanEncode();
					String enc = hfe.encode(readFile);
					Mandatory1.sizeAfterHuffmanCompression[order] += enc
							.length();
					String writeDir = Mandatory1.localRootPath
							+ Mandatory1.huffEncFolder + "\\order" + order;
					Mandatory1.rw.writeLine(enc, writeDir, "encoded" + i + "."
							+ Mandatory1.huffFileExtension);
					System.out.print(i + " ");
				}
				System.out.println();
			}
			System.out.println("Decompression done!");
		}

		public void decode() throws IOException {
			System.out.println();
			for (int order = 0; order <= 3; order++) {
				System.out.print("Decoding order " + order + ": ");
				for (int i = 1; i <= 100; i++) {
					String readPath = Mandatory1.localRootPath
							+ Mandatory1.huffEncFolder + "\\order" + order
							+ "\\encoded" + i + "."
							+ Mandatory1.huffFileExtension;
					byte[] readBytes = Mandatory1.rw.readBytes(readPath);
					String readFile = Mandatory1.rw.bytesToString(readBytes);

					HuffmanDecode hfd = new HuffmanDecode();
					String dec = hfd.decode(readFile);
					String writeDir = Mandatory1.localRootPath
							+ Mandatory1.huffDecFolder + "\\order" + order;
					Mandatory1.rw.writeLine(dec, writeDir, "decoded" + i + "."
							+ Mandatory1.lzwFileExtension);
					System.out.print(i + " ");
				}
				System.out.println();
			}
			System.out.println("Decompression done!");
		}

		public class HuffmanDecode {
			HashMap<String, Character> huffmanCodes;
			String binaryEncoded = "";
			int shortestCode = Integer.MAX_VALUE;

			String decode(String s) {
				String encodedPart = getEncodedPartAndBuildDictionary(s);
				String encodedBinary = convertToBinaryString(encodedPart);
				this.binaryEncoded = stripEncodedBinary(encodedBinary);
				String decoded = "";
				while (this.binaryEncoded.length() > 0) {
					decoded += findNextChar();
				}
				return decoded;
			}

			Character findNextChar() {
				/*
				 * We do not need to look for prefixes shorter than the shortest
				 * prefix recorded
				 */
				int length = this.shortestCode;
				if (length >= this.binaryEncoded.length()) {
					String ret = this.binaryEncoded;
					this.binaryEncoded = "";
					return this.huffmanCodes.get(ret);
				}
				String binary = this.binaryEncoded.substring(0, length);
				while (this.huffmanCodes.get(binary) == null
						&& length < this.binaryEncoded.length() - 1) {
					length++;
					binary = this.binaryEncoded.substring(0, length);
				}
				this.binaryEncoded = this.binaryEncoded.substring(binary
						.length());
				return this.huffmanCodes.get(binary);

			}

			String stripEncodedBinary(String enc) {
				int i = 0;
				while (enc.charAt(i) == '0') {
					i++;
				}
				return enc.substring(i + 1);
			}

			String convertToBinaryString(String s) {
				String bin = "";
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					bin += prependLeadingZeroes(c, 8);
				}
				return bin;
			}

			String prependLeadingZeroes(char c, int upToLength) {
				String bitString = Integer.toBinaryString((int) c);
				while (bitString.length() < upToLength) {
					bitString = "0" + bitString;
				}
				return bitString;
			}

			String getEncodedPartAndBuildDictionary(String s) {
				int dictSize = getLeadingInteger(s);
				s = s.substring(("" + dictSize).length() + 1);

				/*
				 * Now we know how many characters are used to represent our
				 * dictionary. The rest are obviously our encoded string.
				 */
				String dict = s.substring(0, dictSize);
				String encoded = s.substring(dict.length());
				buildDictionary(dict);
				return encoded;
			}

			int getLeadingInteger(String s) {
				int ret = 0;
				try {
					for (int i = 1; i < s.length(); i++) {
						ret = Integer.parseInt(s.substring(0, i));
					}
				} catch (NumberFormatException nfe) {
					return ret;
				}
				return -1;
			}

			void buildDictionary(String s) {
				this.huffmanCodes = new HashMap<String, Character>();
				while (s.length() > 1) {
					char c = s.charAt(0);
					int codeLength = getLeadingInteger(s.substring(1));
					String code = "";
					int startIndex = 2 + ("" + codeLength).length();
					for (int i = startIndex; i < startIndex + codeLength; i++) {
						code += s.charAt(i);
					}
					s = s.substring(startIndex + codeLength);
					this.huffmanCodes.put(code, c);
					this.shortestCode = Math.min(this.shortestCode, codeLength);
				}
			}

		}

		public class HuffmanEncode {

			public HMTree tree;
			public HuffmanEncode hm;
			public HashMap<Character, Integer> frequencies;
			public HashMap<Character, String> huffmanCodes;
			public ArrayList<Character> chars;

			public String encode(String s) {
				return encodeAndPrependLibrary(s);
			}

			public String encodeAndPrependLibrary(String s) {
				this.hm = new HuffmanEncode();
				this.huffmanCodes = new HashMap<Character, String>();
				calcFrequencies(s);
				this.tree = buildTree();
				generateCodes(this.tree, new StringBuffer());
				String encodedBitString = "";
				for (int i = 0; i < s.length(); i++) {
					encodedBitString += this.huffmanCodes.get(s.charAt(i));
				}
				/*
				 * To handle the problematique regarding this bit string's
				 * variable length, I define the start of the bit string to be
				 * the first bit after the first 1-bit, and add a leading 1 and
				 * zeroes if needed.
				 * 
				 * This way, the bit string is divisible by 8, it can be
				 * converted to bytes, and it can easily be recovered by
				 * removing all leading zeroes and the first 1.
				 */
				encodedBitString = "1" + encodedBitString;
				while (encodedBitString.length() % 8 != 0) {
					encodedBitString = "0" + encodedBitString;
				}

				/*
				 * This was a little bit tricky. I don't know how to decode
				 * without any knowledge of the dictionary, so I prepend it.
				 * 
				 * Also, we need to know which part of the string is dictionary
				 * and which is actually encoded text.
				 * 
				 * I ended up returning <length of dictionary> + space +
				 * dictionary + encoded text.
				 * 
				 * This way, we can expect any integer followed by a space, and
				 * then we know how many characters to read in order to
				 * re-generate our dictionary. On the decoder side we read this
				 * amount of characters, generate our dictionary, and read the
				 * encoded text.
				 */
				String lib = dictionary();
				return "" + lib.length() + " " + lib
						+ convertFromBinary(encodedBitString);
			}

			public String convertFromBinary(String binary) {
				String bin = binary;
				String res = "";
				while (bin.length() > 0) {
					String sub = bin.substring(0, Math.min(8, bin.length()));
					bin = bin.substring(sub.length());
					res += convertCharFromBinary(sub);
				}
				return res;
			}

			public String dictionary() {
				String lib = "";
				for (int i = 0; i < this.chars.size(); i++) {
					char c = this.chars.get(i);
					String code = this.huffmanCodes.get(c);
					/*
					 * Delimiters are a pain. Here the syntax will be:
					 * 
					 * one character + the length of a code + a space denoting
					 * the end of this integer + the code itself
					 */
					lib += c + "" + code.length() + " " + code;
				}
				return lib;
			}

			public char convertCharFromBinary(String binaryLength8) {
				return (char) Integer.parseInt(binaryLength8, 2);
			}

			public void calcFrequencies(String s) {
				char[] stringChars = s.toCharArray();
				this.frequencies = new HashMap<Character, Integer>();
				this.chars = new ArrayList<Character>();
				for (char c : stringChars) {
					if (this.frequencies.get(c) == null) {
						this.frequencies.put(c, 1);
						this.chars.add(c);
					} else {
						int prevFreq = this.frequencies.get(c);
						this.frequencies.put(c, prevFreq + 1);
					}
				}
			}

			public HMTree buildTree() {
				PriorityQueue<HMTree> trees = new PriorityQueue<HMTree>();
				for (int i = 0; i < this.chars.size(); i++) {
					char c = this.chars.get(i);
					trees.offer(this.hm.new HMLeaf(this.frequencies.get(c), c));
				}
				while (trees.size() > 1) {
					HMTree t1 = trees.poll();
					HMTree t2 = trees.poll();
					trees.offer(this.hm.new HMNode(t1, t2));
				}
				return trees.poll();
			}

			public void generateCodes(HMTree tree, StringBuffer prefix) {
				if (tree instanceof HMLeaf) {
					HMLeaf leaf = (HMLeaf) tree;
					this.huffmanCodes.put(leaf.c, "" + prefix);
				} else if (tree instanceof HMNode) {
					HMNode node = (HMNode) tree;
					prefix.append('0');
					generateCodes(node.left, prefix);
					prefix.deleteCharAt(prefix.length() - 1);
					prefix.append('1');
					generateCodes(node.right, prefix);
					prefix.deleteCharAt(prefix.length() - 1);
				}
			}

			class HMTree implements Comparable<HMTree> {
				int freq;

				public HMTree(int freq) {
					this.freq = freq;
				}

				@Override
				public int compareTo(HMTree otherTree) {
					return this.freq - otherTree.freq;
				}
			}

			class HMLeaf extends HMTree {
				char c;

				public HMLeaf(int freq, char c) {
					super(freq);
					this.c = c;
				}
			}

			class HMNode extends HMTree {
				HMTree left, right;

				public HMNode(HMTree left, HMTree right) {
					super(left.freq + right.freq);
					this.left = left;
					this.right = right;
				}
			}
		}
	}

	/*
	 * These should not be changed manually. They either need to stay this way,
	 * or they can be changed through UI.
	 */
	static Scanner kbd = null;
	static Mandatory1 m1 = null;;
	static ReadWrite rw = null;
	static UserHandling uh = null;
	static MarkovGenerator mg = null;
	static LZW lzw = null;
	static Huffman huff = null;
	static String urlToFile = "https://raw.githubusercontent.com/ohy092/Markov-Huffman-LZW/master/folktale.txt";
	static String localCacheFileName = "folktale.txt";
	static int textIsOnLineNumber = 9;
	static int[] sizeAfterGenerating = new int[4];
	static int[] sizeAfterLZWCompression = new int[4];
	static int[] sizeAfterHuffmanCompression = new int[4];
}
