package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataWriter;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.scijava.log.LogService;

import ij.ImagePlus;
import ij.plugin.PlugIn;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class N5Importer implements PlugIn
{
	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	public static final String MetadataAutoKey = "Auto-detect";
	public static final String MetadataImageJKey = "ImageJMetadata";
	public static final String MetadataN5CosemKey = "Cosem Metadata";
	public static final String MetadataN5ViewerKey = "N5Viewer Metadata";
	public static final String MetadataCustomKey = "CustomMetadata";
	public static final String MetadataDefaultKey = "DefaultMetadata";
	
	public static final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
					new N5ImagePlusMetadata( "" ),
					new N5CosemMetadata( "", null, null ),
					new N5ViewerMetadataParser( false ),
					new DefaultMetadata( "", 1 )
				};

    private N5Reader n5;

	private DatasetSelectorDialog selectionDialogNew;
	
	private DataSelection selection;

	private Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes;

	private Interval subset;

	private boolean asVirtual;

	public N5Importer()
	{
		// default image plus metadata writers
		impMetaWriterTypes = new HashMap< Class<?>, ImageplusMetadata< ? > >();
		impMetaWriterTypes.put( N5ImagePlusMetadata.class, new N5ImagePlusMetadata( "" ) );
		impMetaWriterTypes.put( N5CosemMetadata.class, new N5CosemMetadata( "", null, null ) );
		impMetaWriterTypes.put( N5ViewerMetadataParser.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( N5SingleScaleMetadata.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( DefaultMetadata.class, new DefaultMetadata( "", 1 ) );
	}

	@Override
	public void run( String arg )
	{
		selectionDialogNew = new DatasetSelectorDialog( N5Importer::getReader, PARSERS );
		selectionDialogNew.virtualOption();
		selectionDialogNew.run(
				selection -> {
					this.selection = selection;
					this.n5 = selection.n5;
					this.asVirtual = selectionDialogNew.isVirtual();
					this.subset = selectionDialogNew.getMinMax();
					try { process(); }
					catch ( Exception e ) { e.printStackTrace(); }
				});
	}

	public static N5Reader getReader( final String path )
	{
		if( path == null )
			return null;

		File f = new File( path );
		if( f.isDirectory() && path.endsWith( ".n5" ))
		{
			try
			{
				return new N5FSReader( path );
			}
			catch ( IOException e1 )
			{
				 e1.printStackTrace();
			}
		}
		else if( f.isFile() && 
				( path.endsWith( ".h5"  ) || path.endsWith( ".hdf" ) || path.endsWith( ".hdf5" )))
		{
			try
			{
				return new N5HDF5Reader( path, 32, 32, 32 );
			}
			catch ( IOException e )
			{
				 e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > void process() throws ImgLibException, IOException
	{
		for( N5Metadata datasetMeta : selection.metadata )
		{
			String d = datasetMeta.getPath();
			RandomAccessibleInterval<T> imgRaw = (RandomAccessibleInterval<T>) N5Utils.open( n5, d );

			RandomAccessibleInterval<T> img;
			if( subset != null )
				img = Views.interval( imgRaw, subset );
			else
				img = imgRaw;

			ImagePlus imp;
			if( asVirtual )
			{
				imp = ImageJFunctions.wrap( img, d );
			}
			else
			{
				ImagePlusImg<T,?> ipImg = new ImagePlusImgFactory<>( Views.flatIterable( img ).firstElement()).create( img );
				LoopBuilder.setImages( img, ipImg ).forEachPixel( (x,y) -> y.set( x ) );
				imp = ipImg.getImagePlus();
			}

			try
			{ 
				ImageplusMetadata< M > ipMeta = ( ImageplusMetadata< M > ) impMetaWriterTypes.get( datasetMeta.getClass() );
				ipMeta.writeMetadata( ( M ) datasetMeta, imp );
			}
			catch( Exception e )
			{
				System.err.println("Failed to convert metadata to Imageplus for " + d );
			}

			imp.show();
		}
	}

	public static Interval containingBlockAlignedInterval(
			final N5Reader n5, 
			final String dataset, 
			final Interval interval ) throws IOException
	{
		return containingBlockAlignedInterval( n5, dataset, interval, null );
	}

	/**
	 * Returns the smallest {@link Interval} that contains the input interval
	 * and contains complete blocks.
	 * 
	 * @param n5 the n5 reader
	 * @param dataset the dataset
	 * @param interval the interval
	 * @return the smallest containing interval
	 * @throws IOException 
	 */
	public static Interval containingBlockAlignedInterval(
			final N5Reader n5, 
			final String dataset, 
			final Interval interval,
			final LogService log ) throws IOException
	{
		// TODO move to N5Utils?
		if ( !n5.datasetExists( dataset ) )
		{
			if( log != null )
				log.error( "no dataset" );

			return null;
		}

		DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
		int nd = attrs.getNumDimensions();
		int[] blockSize = attrs.getBlockSize();
		long[] dims = attrs.getDimensions();

		long[] min = new long[ nd ];
		long[] max = new long[ nd ];
		for( int d = 0; d < nd; d++ )
		{
			// check that interval aligns with blocks
			min[ d ] = interval.min( d )- (interval.min( d ) % blockSize[ d ]);
			max[ d ] = interval.max( d )  + ((interval.max( d )  + blockSize[ d ] - 1 ) % blockSize[ d ]);

			// check that interval is contained in the dataset dimensions
			min[ d ] = Math.max( 0, interval.min( d ) );
			max[ d ] = Math.min( dims[ d ] - 1, interval.max( d ) );
		}

		return new FinalInterval( min, max );
	}
}
