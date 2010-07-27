package fi.csc.microarray.client.visualisation.methods.gbrowser.fileFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher.Chunk;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegion;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.RegionContent;

/**
 * Parser for WIG file format.
 * 
 * @see http://genome.ucsc.edu/goldenPath/help/wiggle.html
 */

public class WIGParser extends TsvParser {
	
	private final String VARIABLE_STEP = "variableStep";
	private final String FIXED_STEP = "fixedStep";
	
	
//	String type;//variableStep or fixedStep
//	String chr;
//	Long span;
//	Long startPosition;
//	Long step;
	List<headerDefinition> headers = new ArrayList<headerDefinition>();
	
	private class headerDefinition {
		
		String type;//variableStep or fixedStep
		String chr;
		Long span;
		Long startPosition;
		Long step;
		Long headerPosition;
	}
	
	//fileDefinition is set in the setParser method
	public WIGParser(File file){
		super(null);
		setParser(file);
	}

	@Override
	public RegionContent[] concise(Chunk chunk) {
		return new RegionContent[] {};
	}

	@Override
	public String getName() {
		return "Wiggle file parser";
	}
	
	/**
	 * reading file headers info
	 * @param file
	 */
	public void setParser(File file) {
		
		headerDefinition header = new headerDefinition();
		
		try {
			RandomAccessFile reader = new RandomAccessFile(file, "r");
			String line = reader.readLine();
						
			while (!line.contains("track")){
				line = reader.readLine();
			}
			
			Long fileLength = reader.length();
			//setting headers
			while (reader.getFilePointer() != fileLength) {				
				line = reader.readLine();
				if (line.contains("Step")) {
					header = setHeader(line);
					header.headerPosition = reader.getFilePointer();
					headers.add(header);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * this chunk is WIG file header info in the middle of data
	 * 
	 * for example
	 *  variableStep chrom=chr1 span=25
	 *   
	 */
	public headerDefinition setHeader(String chunk) {

		headerDefinition header = new headerDefinition();
		
		try {
			
			if (chunk.indexOf("\n") != -1){
				chunk = chunk.substring(0, chunk.indexOf("\n"));
			}
			String[] cols = chunk.split(" ");
			
			switch (cols.length) {
				case 2:
					header.type = cols[0];//variable step
					header.chr = cols[1].replace("chrom=chr", "");
					
					break;
				case 3:
					header.type = cols[0];//variable step
					header.chr = cols[1].replace("chrom=chr", "");
					header.span = Long.parseLong(cols[2].replace("span=", ""));
					break;
				case 4:
					header.type = cols[0];//fixed step
					header.chr = cols[1].replace("chrom=chr", "");
					header.startPosition = Long.parseLong(cols[2].replace("start=", ""));
					header.step = Long.parseLong(cols[3].replace("step=", ""));
					break;
				case 5:
					header.type = cols[0];//fixed step
					header.chr = cols[1].replace("chrom=chr", "");
					header.startPosition = Long.parseLong(cols[2].replace("start=", ""));
					header.step = Long.parseLong(cols[3].replace("step=", ""));
					header.span = Long.parseLong(cols[4].replace("span=", ""));
					break;
				default: 
					break;
			}
			
			return header;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
	}
	
	@Override
	public List<RegionContent> getAll(Chunk chunk, Collection<ColumnType> requestedContents) {

		List<RegionContent> rows = new LinkedList<RegionContent>();
		headerDefinition header = getHeader(chunk.getByteLocation());
		setFileDefinition(header);

		if (header.type.equals(FIXED_STEP)) {
			//fixed step
			
			int nextPosition = 0;
			for (String row : chunk.getContent().split("\n")) {
				Map<ColumnType, Object> values = new HashMap<ColumnType, Object>();
				
				String[] cols = row.split(" ");
				
				if (cols.length > 1) {
					
					setHeader(row);
				} else {
					// Calculate start and end positions
					Long start = header.startPosition + nextPosition * header.step;
					Long end = header.startPosition + nextPosition * header.step + header.span - 1;
					
					for (ColumnType requestedContent : requestedContents) {
						values.put(requestedContent, this.get(cols, requestedContent));
					}
					
					rows.add(new RegionContent(new BpCoordRegion(
							start, end, new Chromosome(header.chr)), values));
				}
				nextPosition++;
			}
			
		} else {
			//variable step
			for (String row : chunk.getContent().split("\n")) {
				
				Map<ColumnType, Object> values = new HashMap<ColumnType, Object>();
				
				String[] cols = row.split("\t");
				
				if (cols.length <2) {
					
					setHeader(row);
				} else {

					Long start = Long.parseLong(cols[0]);
					Long end = Long.valueOf(Integer.parseInt(cols[0]) + header.span-1);
					for (ColumnType requestedContent : requestedContents) {
						values.put(requestedContent, this.get(cols, requestedContent));
					}
			
					rows.add(new RegionContent(new BpCoordRegion(
							start, end, new Chromosome(header.chr)), values));
				}
			}	
		}
		
		return rows;
	}
	
	@Override
	public long getHeaderLength(File file) {
		
		FileReader f;
		int lines = 0;
		try {
			
			f = new FileReader(file);
			BufferedReader reader = new BufferedReader(f);
			String str = "";
			String line = "";
			try {
				
				line = reader.readLine();
				while (!isNumber(line.charAt(0))){
					str += line;
					lines++;
					line = reader.readLine();
				}
				
				return str.length() + lines;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	private boolean isNumber(char c){
		try {
			Integer.parseInt(String.valueOf(c));
			return true;
			
		} catch (NumberFormatException e) {
			
			return false;
		}
	}
	
	@Override
	public Object get(String[] cols, ColumnType col) {
		
		String string;
		ColumnDefinition fieldDef;
		
		try {

			if (cols.length == 0) {
				return null;
			}
			
			if (col.equals(ColumnType.SEQUENCE)){
				return new String("GGG");
			} else if (col.equals(ColumnType.STRAND)){
				return Strand.BOTH;
			}
			
			string = cols[getFileDefinition().indexOf(col)].trim();
			fieldDef = getFileDefinition().getFieldDef(col);
			
			if (fieldDef.type == Type.FLOAT) {
				return new Float(string);

			} else if (fieldDef.type == Type.LONG) {

				if (string.length() > 0) {
					return new Long(string);
				} else {
					return Long.MIN_VALUE;
				}
			}
			return null;
			
		} catch (Exception e) {
			throw new RuntimeException("error parsing columns: " + Arrays.toString(cols) + " (looking for: " + col + ")", e);
		}
	}
	
	@Override
	public BpCoordRegion getBpRegion(Chunk chunk) {
		
		Long start = 0l;
		Long end = 0l;
		headerDefinition firstHeader = getHeader(chunk.getByteLocation());
		String startChr = firstHeader.chr;
		String endChr = firstHeader.chr;
		headerDefinition lastHeader = getLastHeader(chunk);
		
		if (lastHeader.chr != null){
			endChr = lastHeader.chr;
		}
		
		if (firstHeader.type.equals(VARIABLE_STEP)) {
			
			try {
				start = Long.valueOf(getFirstRow(chunk)[0]);
				end = Long.valueOf(getLastRow(chunk)[0]) + lastHeader.span - 1;
			} catch (Exception e) {
				end = Long.valueOf(getLastRow(chunk)[0]) + firstHeader.span - 1;
			}
			
		} else {
			
			try {
				start = lastHeader.startPosition;
				end = lastHeader.startPosition + 
				getFixedStepEnd(chunk) * lastHeader.step + lastHeader.span - 1;
				
			} catch (Exception e) {
				start = firstHeader.startPosition;
				end = firstHeader.startPosition + getFixedStepEnd(chunk) * firstHeader.step
				+ firstHeader.span - 1;
			}
		}

		return new BpCoordRegion(start, new Chromosome(startChr), end, new Chromosome(endChr));
	}
	
	public headerDefinition getLastHeader(Chunk chunk) {
		
		String content = chunk.getContent();
		headerDefinition header = new headerDefinition();
		
		int lineStartIndex;
		try {
			lineStartIndex = content.lastIndexOf("chrom", content.length() - 2);
		} catch (Exception e) {
			lineStartIndex = -1;
		}
		
		if (lineStartIndex < 0) {
			return null;
		}

		header = setHeader(content);
		
		return header;
	}
	
	public Long getFixedStepEnd(Chunk chunk) {
		
		int i = 0;
		
		for (String line : chunk.getContent().split("\n")) {
			if (line.contains("chrom")) {
				i=0;
			} else {
				i++;
			}
			
		}
		
		return new Long(i)-1;
	}
	
	@Override
	public String[] getFirstRow(Chunk chunk) {
		
		String content = chunk.getContent();
		
		if (content.startsWith("variableStep")) {
			String row = content.substring(content.indexOf("\n")+1, content.length());
			return content.substring(0, row.indexOf("\n")).split("\t");
			
		} else {
			return content.substring(0, content.indexOf("\n")).split("\t");
			
		}
	}

	public headerDefinition getHeader(Long location) {
		for (int i = 0; i<headers.size(); i++) {
			if (headers.get(i).headerPosition > location) {
				return headers.get(i-1);
			}
		}
		return null;
	}
	
	@Override
	public String[] getLastRow(Chunk chunk) {
		//TODO if the last row is header, then the last-1 row should be taken
		
		//minus two to convert from length to index and skip the last line change
		int lineStartIndex = chunk.getContent().lastIndexOf("\n", chunk.getContent().length() - 2);
		
		if (lineStartIndex < 0) {
			lineStartIndex = 0;
		}
		
		return chunk.getContent().substring(lineStartIndex+1, chunk.getContent().length()).split("\t");
	}
	
	public void setFileDefinition(headerDefinition header) {
		
		if (header.type.equals(VARIABLE_STEP)) {
			
			setFileDefinition(new FileDefinition(Arrays.asList(
					new ColumnDefinition[] { 
							new ColumnDefinition(ColumnType.BP_START, Type.LONG), 
							new ColumnDefinition(ColumnType.VALUE, Type.FLOAT), }
					)));
			
		} else {
			
			setFileDefinition(new FileDefinition(Arrays.asList(
					new ColumnDefinition[] { 
							new ColumnDefinition(ColumnType.VALUE, Type.FLOAT), }
					)));
			
		}
	}
	
}
