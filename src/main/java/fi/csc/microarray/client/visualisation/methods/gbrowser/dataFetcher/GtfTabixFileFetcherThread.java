package fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.broad.tribble.readers.TabixReader;
import org.broad.tribble.readers.TabixReader.Iterator;

import fi.csc.microarray.client.visualisation.methods.gbrowser.GtfTabixDataSource;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.Strand;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.GeneRequest;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Region;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.RegionContent;

/**
 * @author Aleksi Kallio, Petri Klemelä
 *
 */
public class GtfTabixFileFetcherThread extends Thread {

	private BlockingQueue<BpCoordFileRequest> fileRequestQueue;
	private ConcurrentLinkedQueue<ParsedFileResult> fileResultQueue;

	private GtfTabixDataSource dataSource;

	private GtfTabixHandlerThread areaRequestThread;

	private boolean poison = false;


	public GtfTabixFileFetcherThread(
			BlockingQueue<BpCoordFileRequest> fileRequestQueue, 
			ConcurrentLinkedQueue<ParsedFileResult> fileResultQueue, 
			GtfTabixHandlerThread areaRequestThread, GtfTabixDataSource dataSource) {

		this.fileRequestQueue = fileRequestQueue;
		this.fileResultQueue = fileResultQueue;
		this.areaRequestThread = areaRequestThread;
		this.dataSource = dataSource;
		this.setDaemon(true);
	}

	public void run() {

		while (!poison) {
			try {
				processFileRequest(fileRequestQueue.take());

			} catch (IOException e) {
				e.printStackTrace(); // FIXME fix exception handling
			} catch (InterruptedException e) {
				e.printStackTrace(); // FIXME fix exception handling
			}
		}
	}

	private void processFileRequest(BpCoordFileRequest fileRequest) throws IOException {

		if (fileRequest.getStatus().poison) {
			poison = true;
			return;
		}

		List<RegionContent> resultList = null;

		if (fileRequest.areaRequest instanceof GeneRequest) {

			resultList = processGeneSearch((GeneRequest)fileRequest.areaRequest, fileRequest);

		} else { 

			Region region = new Region(fileRequest.getFrom(), fileRequest.getTo());

			resultList = new LinkedList<RegionContent>();

			GeneSet genes = fetchExons(region);
			
			for (Gene gene : genes.values()) {

				LinkedHashMap<ColumnType, Object> values = new LinkedHashMap<ColumnType, Object>();

				values.put(ColumnType.VALUE, gene);

				resultList.add(new RegionContent(gene.getRegion(), values));
			}
		}

		ParsedFileResult result = new ParsedFileResult(resultList, fileRequest, fileRequest.areaRequest, fileRequest.getStatus());

		fileResultQueue.add(result);
		areaRequestThread.notifyAreaRequestHandler();	
	}

	private List<RegionContent> processGeneSearch(GeneRequest areaRequest,
			BpCoordFileRequest fileRequest) throws IOException {

		String searchString = areaRequest.getSearchString().toLowerCase();
		Chromosome chr = areaRequest.start.chr;


		Region region = new Region(1l, Long.MAX_VALUE, chr);

		GeneSet genes = fetchExons(region);
		
		List<RegionContent> resultList = new LinkedList<RegionContent>();

		for (Gene gene : genes.values()) {

			if (gene.getName().toLowerCase().equals(searchString)) {

				LinkedHashMap<ColumnType, Object> values = new LinkedHashMap<ColumnType, Object>();

				values.put(ColumnType.VALUE, gene);
				resultList.add(new RegionContent(gene.getRegion(), values));
			}
		}

		return resultList;
	}

	/**
	 * Find exons in a given range.
	 * 
	 * @param request
	 * @return
	 * @throws IOException 
	 */
	public GeneSet fetchExons(Region request) throws IOException {
		// Read the given region
		TabixReader.Iterator iter = getTabixIterator(request);

		GeneSet genes = new GeneSet();		
		String line;
		
		if (iter != null) { //null if there isn't such chromosome in annotations

			while ((line = iter.next()) != null) {
				parseGtfLine(line, genes);
			}
		}

		return genes;
	}

	private Iterator getTabixIterator(Region request) {
		String chromosome = request.start.chr.toNormalisedString();

		//limit to integer range
		int start = (int) Math.min(Integer.MAX_VALUE, request.start.bp);
		int end = (int) Math.min(Integer.MAX_VALUE, request.end.bp);
		
		//Extend area to be able to draw introns at screen edge, but don't go over MAX_VALUE, or below 1
		//TODO Be more clever to avoid getting so much useless data
		int EXTRA = 500000; //O,5M should be enought for the longest human introns http://www.bioinfo.de/isb/2004040032/
		
		start = (int) Math.max((long)start - EXTRA, 1);
		end = (int) Math.min((long)end + EXTRA, Integer.MAX_VALUE);

		//Check that region is below max bin size of Tabix
		int MAX_BIN_SIZE = 512*1024*1024 - 2;

		start = (int) Math.min(MAX_BIN_SIZE, start);
		end = (int) Math.min(MAX_BIN_SIZE, end);

		start = (int) Math.max(1, start);
		end = (int) Math.max(1, end);

		String queryRegion = chromosome + ":" + start + "-" + end;

		TabixReader.Iterator iter = null;
		
		try {
			iter = dataSource.getReader().query(queryRegion);
		} catch (ArrayIndexOutOfBoundsException e) {
			//No such chromosome
		}
			
		return iter;
	}


	private void parseGtfLine(String line, GeneSet genes) {

		String[] cols;

		String[] ids;

		cols = line.split("\t");

		String chr = cols[0];
		String biotype = cols[1];
		String feature = cols[2];
		String exonStart = cols[3];
		String exonEnd = cols[4];
		String strand = cols[6];

		ids = parseIds(cols[8]);

		String geneId = ids[0];
		String transcId = ids[1];
		String exonIndex = ids[2];
		String geneName = ids[3];
		String transcName = ids[4];

		Region region = new Region(Long.parseLong(exonStart), Long.parseLong(exonEnd), 
				new Chromosome(chr), getStrand(strand));

		Exon exon = new Exon(region, feature, Integer.parseInt(exonIndex));

		genes.addExon(exon, geneId, transcId, geneName, transcName, biotype);

	} 

	private static Strand getStrand(String strand) {

		if ("+".equals(strand)) {
			return Strand.FORWARD;
		} else if ("-".equals(strand)) {
			return Strand.REVERSED;
		} else if (".".equals(strand)) {
			return Strand.NONE;
		}
		return Strand.UNRECOGNIZED;
	}

	private static final String[] ID_FIELDS = { "gene_id", "transcript_id", "exon_number", "gene_name", "transcript_name" };

	public static String[] parseIds(String ids) {		

		String[] split = ids.split(";");
		String[] result = new String[ID_FIELDS.length];

		String key = null;
		String value = null;
		int indexOfQuotationMark = 0;

		for (int i = 0; i < split.length; i++) {

			indexOfQuotationMark = split[i].indexOf("\"");

			key = split[i].substring(1, indexOfQuotationMark - 1);
			value = split[i].substring(indexOfQuotationMark + 1, split[i]
					.lastIndexOf("\""));

			for (int fieldNumber = 0; fieldNumber < ID_FIELDS.length; fieldNumber++) {
				if (ID_FIELDS[fieldNumber].equals(key)) {
					result[fieldNumber] = value;
				}
			}
		}

		return result;
	}

	public String toString() {
		return this.getClass().getName() + " - " + dataSource;
	}

	public class Transcript {

		Region region;
		List<Exon> exons;

	}
}
