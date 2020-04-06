package org.janelia.saalfeldlab.n5.demos;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class SaveToN5 {

	public static void main(String[] args) throws IOException {

		new ImageJ();
		
		System.out.println( "loading from ip" );
		ImagePlus imp = IJ.openImage("/Users/bogovicj/tmp/mitosis.tif");
		N5FSWriter n5 = new N5FSWriter("/Users/bogovicj/tmp/mitosis.n5");

		String n5dataset = "raw";
		int[] blockSize = new int[]{ 32, 32, 32, 32, 32 };
		GzipCompression compression = new GzipCompression();

//		System.out.println( "saving" );
//		N5IJUtils.save(imp, n5, n5dataset, blockSize, compression);
//		System.out.println( "done" );

		
		N5FSReader n5reader = new N5FSReader("/Users/bogovicj/tmp/mitosis.n5");
		System.out.println( "loading from n5" );
		ImagePlus impN5 = N5IJUtils.load( n5reader, n5dataset );
		impN5.setTitle("fromN5");
		
		imp.show();
		impN5.show();
		

	}

}
