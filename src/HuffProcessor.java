import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] freq = readForCounts(in);
		HuffNode root = makeTreeFromCounts(freq);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	
	
	
	private void writeCompressedBits(String[] encodings, BitInputStream in, BitOutputStream out) {
		int bits = 1;
		while (bits >= 0) { 
			bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) {
				writeStringToBits(encodings, out, ALPH_SIZE);
				return;
			}
			writeStringToBits(encodings, out, bits);
			
		}
		
	}
	
	private void writeStringToBits(String[] encodings, BitOutputStream out, int bits) {
		String code = encodings[bits];
		for (int i = 0; i < code.length(); i++) {
			String s = code.substring(i, i+1);
			int k = Integer.parseInt(s);
			out.writeBits(1, k);
		}
		return;
	}
		
		

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if (root.myValue != -1) {
	    	    out.writeBits(1,  1);
	    	    out.writeBits(9, root.myValue);
	    	    return;
	    }

	    out.writeBits(1, 0);
	    writeHeader(root.myLeft, out);
		writeHeader(root.myRight, out); 
		return;
	}
	private String[] makeCodingsFromTree(HuffNode root) {
		String[] codings = new String[257];
		codingHelper(root, "", codings);
		return codings;
	}

	private void codingHelper(HuffNode root, String path, String[] codings) {
		if (root.myValue != -1) {
			codings[root.myValue] = path;
	        return;
		}
		codingHelper(root.myLeft, path + "0", codings);
		codingHelper(root.myRight, path + "1", codings);
		return;
	}

	private HuffNode makeTreeFromCounts(int[] freq) {
		PriorityQueue<HuffNode> PQ = new PriorityQueue<>();
		
		for (int index = 0; index < freq.length; index++) {
			if (freq[index] > 0) {
				PQ.add(new HuffNode(index, freq[index], null, null));
			}
		}
		PQ.add(new HuffNode(PSEUDO_EOF, 1));
		while (PQ.size() > 1) {
			HuffNode left = PQ.remove();
			HuffNode right = PQ.remove();
			HuffNode t = new HuffNode(-1, left.myWeight + right.myWeight, left, right);
			PQ.add(t);
		}
		
		HuffNode root = PQ.remove();
		return root;
	}

	private int[] readForCounts(BitInputStream in) {
		
		int[] freq = new int[ALPH_SIZE + 1];
		freq[PSEUDO_EOF] = 1;
		while (true) {
			int bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) break;
			freq[bits]++;
		}
		return freq;
		
	}


	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 *            
	 *           
	 */
	public void decompress(BitInputStream in, BitOutputStream out) {
		int bits = in.readBits(BITS_PER_INT);
		if(bits !=HUFF_TREE) {
			throw new HuffException("Illegal header starts with "+bits);
			
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
	}
	
	private HuffNode readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root;
		while(true) {
			int bits = in.readBits(1);
			if(bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF\"");
			}
			else {
				if(bits == 0) current = current.myLeft;
				else current = current.myRight;
				
				
				if (current.myLeft ==null &&current.myRight ==null ) {
					if(current.myValue == PSEUDO_EOF) break;
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
			}
		}
		return root;
	}
	
	
	private HuffNode readTreeHeader(BitInputStream in) {
		int bits = in.readBits(1);
		if(bits == -1 ) {
			throw new HuffException("reading bits failed");
			
		}
		if(bits ==0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			return new HuffNode(0,1,left,right);
			}
		else {
			int value = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value, 0, null, null);
		}
	}
	
	
}