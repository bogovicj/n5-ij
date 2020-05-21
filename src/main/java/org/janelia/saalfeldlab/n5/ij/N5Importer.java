package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;

@Plugin( type = Command.class, menuPath = "File>Import>N5" )
public class N5Importer implements Command
{
	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	@Parameter
	private LogService log;
	
	@Parameter
	private UIService ui;
	
    @Parameter
    private String n5RootLocation;

    @Parameter(required=false)
    private String n5Dataset;

    @Parameter
    private boolean alignToBlockGrid;

    @Parameter(choices={ BDV_OPTION, IP_OPTION })
    private String openOptions = BDV_OPTION;

	@Override
	public void run()
	{
		System.out.println( n5RootLocation );

		N5Reader n5;
		try
		{
			n5 = getReader();

			if( openOptions.equals( IP_OPTION ))
			{
				ImagePlus imp = N5IJUtils.load( n5, n5Dataset );
				N5ImagePlusMetadata meta = new N5ImagePlusMetadata();
				meta.readMetadata( n5, n5Dataset, imp );
				ui.show( imp );
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
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
			final Interval interval ) throws IOException
	{
		// TODO move to N5Utils

		if ( !n5.datasetExists( dataset ) )
		{
			System.out.println( "no dataset" );
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

	private N5Reader getReader() throws IOException 
	{
		File f = new File( n5RootLocation );

		if( f.exists() && f.isDirectory())
		{
			return new N5FSReader( n5RootLocation );
		}
		else if( f.exists() && f.isFile() )
		{
			return new N5HDF5Reader( n5RootLocation );
		}

		return null;
	}

}
