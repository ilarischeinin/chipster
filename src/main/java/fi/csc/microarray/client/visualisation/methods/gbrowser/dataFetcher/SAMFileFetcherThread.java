package fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;
import fi.csc.microarray.client.visualisation.methods.gbrowser.SAMDataSource;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat.Strand;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.AreaRequest;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoord;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegion;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Cigar;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.RegionContent;

public class SAMFileFetcherThread extends Thread {

	final private int SAMPLE_SIZE_BP = 100;

	private static final int RESULT_CHUNK_SIZE = 500;

	private BlockingQueue<SAMFileRequest> fileRequestQueue;
	private ConcurrentLinkedQueue<SAMFileResult> fileResultQueue;

	private SAMDataSource dataSource;

	private SAMHandlerThread areaRequestThread;

	public SAMFileFetcherThread(BlockingQueue<SAMFileRequest> fileRequestQueue, ConcurrentLinkedQueue<SAMFileResult> fileResultQueue, SAMHandlerThread areaRequestThread, SAMDataSource dataSource) {

		this.fileRequestQueue = fileRequestQueue;
		this.fileResultQueue = fileResultQueue;
		this.areaRequestThread = areaRequestThread;
		this.dataSource = dataSource;
		this.setDaemon(true);
	}

	public void run() {

		while (true) {
			try {
				processFileRequest(fileRequestQueue.take());

			} catch (IOException e) {
				e.printStackTrace(); // FIXME fix exception handling
			} catch (InterruptedException e) {
				e.printStackTrace(); // FIXME fix exception handling
			}
		}
	}

	private void processFileRequest(SAMFileRequest fileRequest) throws IOException {
		if (fileRequest.areaRequest.status.concise) {
			sampleToGetConcisedRegion(fileRequest);

		} else {
			fetchReads(fileRequest);
		}
	}

	/**
	 * Find reads in a given range.
	 * 
	 * <p>
	 * TODO add cigar to the list of returned values
	 * <p>
	 * TODO add pair information to the list of returned values
	 * 
	 * @param request
	 * @return
	 */
	public void fetchReads(SAMFileRequest fileRequest) {

		AreaRequest request = fileRequest.areaRequest;
int res = 0;
		// Read the given region
		String chromosome = dataSource.getChromosomeNameUnnormaliser().unnormalise(request.start.chr);
		CloseableIterator<SAMRecord> iterator = dataSource.getReader().query(chromosome, request.start.bp.intValue(), request.end.bp.intValue(), false);

		// Produce results
		while (iterator.hasNext()) {

			List<RegionContent> responseList = new LinkedList<RegionContent>();

			// Split results into chunks
			for (int c = 0; c < RESULT_CHUNK_SIZE && iterator.hasNext(); c++) {
				SAMRecord record = iterator.next();
res++;
				// Region for this read
				BpCoordRegion recordRegion = new BpCoordRegion((long) record.getAlignmentStart(), (long) record.getAlignmentEnd(), request.start.chr);

				// Values for this read
				LinkedHashMap<ColumnType, Object> values = new LinkedHashMap<ColumnType, Object>();

				RegionContent read = new RegionContent(recordRegion, values);

				if (request.requestedContents.contains(ColumnType.STRAND)) {
					values.put(ColumnType.STRAND, record.getReadNegativeStrandFlag() ? Strand.REVERSED : Strand.FORWARD);

				}

				if (request.requestedContents.contains(ColumnType.QUALITY)) {

					/*
					 * Now string because of equality problem described below, should be some nice internal object type in the future
					 */

					values.put(ColumnType.QUALITY, record.getBaseQualityString());
				}

				if (request.requestedContents.contains(ColumnType.CIGAR)) {

					Cigar cigar = new Cigar(read, record.getCigar());
					values.put(ColumnType.CIGAR, cigar);
				}

				// TODO Deal with "=" and "N" in read string
				if (request.requestedContents.contains(ColumnType.SEQUENCE)) {

					String seq = record.getReadString();

					values.put(ColumnType.SEQUENCE, seq);
				}

				/*
				 * NOTE! RegionContents created from the same read area has to be equal in methods equals, hash and compareTo. Primary types
				 * should be ok, but objects (including tables) has to be handled in those methods separately. Otherwise tracks keep adding
				 * the same reads to their read sets again and again.
				 */
				responseList.add(read);
			}

			// Send result
			SAMFileResult result = new SAMFileResult(responseList, fileRequest, fileRequest.areaRequest, fileRequest.getStatus());
			fileResultQueue.add(result);
			areaRequestThread.notifyAreaRequestHandler();
			
		}

		// We are done
		iterator.close();
	}

	private void sampleToGetConcisedRegion(SAMFileRequest request) {

		BpCoord from = request.getFrom();
		BpCoord to = request.getTo();
		
		// Fetch new content by taking sample from the middle of this area
		long stepMiddlepoint = (from.bp + to.bp) / 2;
		long start = stepMiddlepoint - SAMPLE_SIZE_BP / 2;
		long end = stepMiddlepoint + SAMPLE_SIZE_BP / 2;
		CloseableIterator<SAMRecord> iterator = dataSource.getReader().query(dataSource.getChromosomeNameUnnormaliser().unnormalise(from.chr), (int) start, (int) end, false);

		// Count reads in this sample area
		int countForward = 0;
		int countReverse = 0;
		for (Iterator<SAMRecord> i = iterator; i.hasNext();) {
			SAMRecord record = i.next();

			// Accept only records that start in this area (very rough approximation for spliced reads)
			if (record.getAlignmentStart() >= start && record.getAlignmentEnd() <= end) {
				if (record.getReadNegativeStrandFlag()) {
					countReverse++;
				} else {
					countForward++;
				}
			}
		}

		// We are done
		iterator.close();

		// Send result
		LinkedList<RegionContent> content = new LinkedList<RegionContent>();
		content.add(new RegionContent(new BpCoordRegion(from, to), countForward, countReverse));
		SAMFileResult result = new SAMFileResult(content, request, request.areaRequest, request.getStatus());
		fileResultQueue.add(result);
		areaRequestThread.notifyAreaRequestHandler();

	}

}