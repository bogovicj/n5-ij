/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.ui;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5ReaderDataset;

import javax.swing.*;

import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class CropOpenDialog
{
	private JFrame frame;

	private JButton okBtn;

	private List< JSpinner > minSpinners;

	private List< JSpinner > maxSpinners;

	private List< N5ReaderDataset > readerDatasetList;

	private BiConsumer< List< N5ReaderDataset >, Interval > okCallback;

	public CropOpenDialog( final List< N5ReaderDataset > readerDatasetList )
	{
		this.readerDatasetList = readerDatasetList;
	}

	public CropOpenDialog( final N5ReaderDataset readerDataset )
	{
		readerDatasetList = Collections.singletonList( readerDataset );
	}

	public CropOpenDialog( final N5Reader n5Root, final String n5Dataset )
	{
		this( new N5ReaderDataset( n5Root, n5Dataset ) );
	}

	public void run( final BiConsumer< List< N5ReaderDataset >, Interval > okCallback )
    {
		this.okCallback = okCallback;

		long[] dims = null;
		try
		{
			for ( N5ReaderDataset readerDataset : readerDatasetList )
			{
				DatasetAttributes attr = readerDataset.n5.getDatasetAttributes( readerDataset.dataset );
				long[] theseDims = attr.getDimensions();
				if ( dims == null )
				{
					dims = theseDims;
				}
				else
				{
					if ( theseDims.length != dims.length )
					{
						System.err.println( "inconsistent dimensions" );
						return;
					}

					for ( int d = 0; d < theseDims.length; d++ )
					{
						if ( theseDims[ d ] < dims[ d ] )
							dims[ d ] = theseDims[ d ];
					}
				}
			}
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
			return;
		}

        final int nd = dims.length;
		frame = new JFrame( "N5 subset" );
        frame.setLayout( new GridLayout( 7, 1 ));
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        final JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new GridLayout( 1, 4 ));

        final JPanel minPanel = new JPanel();
		minPanel.setLayout( new GridLayout( 1, 4 ));

        final JPanel maxPanel = new JPanel();
        maxPanel.setLayout( new GridLayout( 1, 4 ));

        minSpinners = new ArrayList<>();
        maxSpinners = new ArrayList<>();

        labelPanel.add( new JLabel( "Dimension:", SwingConstants.RIGHT ));
        minPanel.add( new JLabel( "Min:", SwingConstants.RIGHT ));
        maxPanel.add( new JLabel( "Max:", SwingConstants.RIGHT ));
		for ( int d = 0; d < nd; d++ )
		{
			labelPanel.add( new JLabel( "" + d, SwingConstants.CENTER ));

			int max = ( int ) dims[ d ] - 1;
			JSpinner minSpin = new JSpinner( new SpinnerNumberModel( 0, 0, max, 1 ));
			minPanel.add( minSpin );
			minSpinners.add( minSpin );

			JSpinner maxSpin = new JSpinner( new SpinnerNumberModel( max, 0, max, 1 ));
			maxPanel.add( maxSpin );
			maxSpinners.add( maxSpin );
		}

        final JPanel buttonPanel = new JPanel();
        okBtn = new JButton("OK");
        okBtn.setEnabled(true);
        okBtn.addActionListener(e -> ok());
        buttonPanel.add(okBtn);

        JPanel textPanel = new JPanel();
        frame.add( new JLabel( "All dimensions are zero-indexed." ));
        frame.add( new JLabel( "Min/max indexes are inclusive." ));
        frame.add( labelPanel );
        frame.add( minPanel );
        frame.add( maxPanel );
        frame.add( buttonPanel );
		frame.pack();
        frame.setVisible( true );
    }

	private void ok()
	{
		assert ( minSpinners.size() == maxSpinners.size() );

		int nd = minSpinners.size();
		long[] min = new long[ nd ];
		long[] max = new long[ nd ];

		for ( int d = 0; d < nd; d++ )
		{
			min[ d ] = ( ( Integer ) minSpinners.get( d ).getValue() ).longValue();
			max[ d ] = ( ( Integer ) maxSpinners.get( d ).getValue() ).longValue();
		}

		FinalInterval interval = new FinalInterval( min, max );
		okCallback.accept( readerDatasetList, interval );

		frame.setVisible( false );
		frame.dispose();
	}
}
