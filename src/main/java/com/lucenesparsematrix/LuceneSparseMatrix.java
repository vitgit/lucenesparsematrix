package com.lucenesparsematrix;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.ParseException;

public class LuceneSparseMatrix {
	
	private static StandardAnalyzer analyzer = new StandardAnalyzer();
	private static IndexWriter writer;
	static Logger log = Logger.getLogger(LuceneSparseMatrix.class.getName());
	static IndexWriterConfig indexWriterDefaultConfig = new IndexWriterConfig();
	static FieldType iFieldType = new FieldType();
	
	private static float aijExample(int i, int j, int m, int n, String type){
		if(type.equals("identical"))
			return (float) ((i == j) ? 1.0 : 0.0);
		
		if(type.equals("i+j"))
			return (float)(i+j);
		
		if(type.equals("i+j even"))
			return (i % 2 == 0 || j % 2 == 0) ? 0.f : (float)(i+j);

		return (float) 0.0;
	}
	private static int StoreMatrix(String matrIndexDir, int m, int n, String type) throws IOException{
		FSDirectory dir = FSDirectory.open(Paths.get(matrIndexDir));
  		writer = new IndexWriter(dir, indexWriterDefaultConfig);
  		
		iFieldType.setStored(true);
		iFieldType.setTokenized(false);
		iFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
  		
	    int numNonZero = 0;
	    for(int ii = 1; ii <= m; ii++)
	    	for(int jj = 1; jj <= n; jj++){
	    		float aij = aijExample(ii, jj, m, n, type);
	    		if(aij > 0.0 || aij < 0.0){	
	    			Document doc = new Document();
	    		
	    			doc.add(new Field("i", String.valueOf(ii), iFieldType));
	    			doc.add(new Field("j", String.valueOf(jj), iFieldType));
	    			doc.add(new Field("aij", String.valueOf(aij), iFieldType));
	    			
	    			writer.addDocument(doc);  
	    			numNonZero++;
	    		}
	    	}
	    writer.close();
	    return numNonZero;
	}
	private static float [] MatVec(Float[] u, int m, int n, String matrIndexDir) throws IOException, ParseException{
		IndexReader reader = null;
		try {
			FSDirectory dir = FSDirectory.open(Paths.get(matrIndexDir));
			reader = DirectoryReader.open(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		
		float [] v = new float[n];
		for(int ii = 1; ii <= m; ii++){	
			float sum = 0.0f;
			for(int jj = 1; jj <= n; jj++){	
				String ij = "i:" + String.valueOf(ii) + " AND j:" + String.valueOf(jj);
				TopScoreDocCollector collector = TopScoreDocCollector.create(1);
			
				
				QueryParser parser = new QueryParser("aij", analyzer);
				Query q = parser.parse(ij);
				
				searcher.search(q, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;
				if(hits.length == 1){	//since matrix is sparse	
					int docId = hits[0].doc;
					Document d = searcher.doc(docId);
					float aij = Float.valueOf(d.get("aij"));
					sum += aij * u[jj - 1];
				}	
			}
			v[ii - 1] = sum;
		}	
		return v;
	}
	public static void main(String[] args) throws IOException, ParseException{
		//========================================
		String matrIndexDir = "/tmp/matrIndexDir";
		int m = 100;
		int n = 200;
		//========================================
		
		System.out.println ("indexing matrix started");
		
		int numNonZero = StoreMatrix(matrIndexDir, m, n, "i+j even");
		
		System.out.println ("indexing matrix done\nNumber of non-zeros " + numNonZero);
		
		Float[] u = new Float[n];
		for(int ii = 0; ii < n; ii++)
			u[ii] = (float)(ii + 1);
		
		for(int ii = 1; ii <= 10; ii++){
			float[] v = MatVec(u, m, n, matrIndexDir);
			System.out.println (Arrays.toString(v));	
		}
		
	}
}
