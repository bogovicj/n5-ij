package org.janelia.saalfeldlab.n5;

public class N5ReaderDataset
{
	public final N5Reader n5;

	public final String dataset;

	public N5ReaderDataset( final N5Reader n5, final String dataset )
	{
		this.n5 = n5;
		this.dataset = dataset;
	}

}
