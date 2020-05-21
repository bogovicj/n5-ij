package org.janelia.saalfeldlab.n5.ij;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = Command.class, menuPath = "File>Save As>N5" )
public class N5Exporter implements Command
{
	public static final String N5FS = "Filesystem";
	public static final String N5H5 = "Hdf5";
	public static final String N5Zarr = "Zarr"; // TODO

	public static final String GZIP_COMPRESSION = "gzip";
	public static final String RAW_COMPRESSION = "raw";
	public static final String LZ4_COMPRESSION = "lz4";
	public static final String XZ_COMPRESSION = "xz";
	public static final String BLOSC_COMPRESSION = "blosc";

    @Parameter(visibility=ItemVisibility.MESSAGE, required=false)
    private String message = "Doc line";

	@Parameter
	private ImagePlus image; // or use Dataset?
//	private Dataset image; // or use ImagePlus?
	
    @Parameter( label = "n5 root")
    private String n5RootLocation;

    @Parameter( label = "dataset")
    private String n5Dataset;

    @Parameter( label = "block size")
    private String blockSizeArg;

    @Parameter( label = "compresstion",
    		choices={GZIP_COMPRESSION, RAW_COMPRESSION, LZ4_COMPRESSION, XZ_COMPRESSION}, style="listBox")
    private String compressionArg = GZIP_COMPRESSION;

    @Parameter( label = "container type",
    		choices={N5FS, N5H5}, style="listBox")
    private String type = N5FS;

    private int[] blockSize;

	public <T extends RealType<T> & NativeType<T>> void process() throws IOException
	{
		blockSize = Arrays.stream( blockSizeArg.split( "," )).mapToInt( x -> Integer.parseInt( x ) ).toArray();

		System.out.println( n5RootLocation );

		N5Writer n5 = getWriter();
		Compression compression = getCompression();

//		/* If we use a Dataset */
//		@SuppressWarnings( "unchecked" )
//		Img<T> img = ( Img< T > ) image;
		
		Img<T> img = ImageJFunctions.wrap( image );

		N5Utils.save( img , n5, n5Dataset, blockSize, compression );

		N5ImagePlusMetadata meta = new N5ImagePlusMetadata();
		meta.writeMetadata( n5, n5Dataset, image );
	}

	@Override
	public void run()
	{
		try
		{
			process();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	private Compression getCompression()
	{
		switch( compressionArg )
		{
		case GZIP_COMPRESSION:
			return new GzipCompression();
		case LZ4_COMPRESSION:
			return new Lz4Compression();
		case XZ_COMPRESSION:
			return new XzCompression();
		case RAW_COMPRESSION:
			return new RawCompression();
		default:
			return new RawCompression();
		}
	}
	
	private N5Writer getWriter() throws IOException
	{
		switch( type )
		{
		case N5FS:
			return new N5FSWriter( n5RootLocation );
		case N5H5:
			return new N5HDF5Writer( n5RootLocation, blockSize );
		}
		return null;
	}

}
