// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

public abstract class RegionAnnotations extends AlignmentAnnotations implements
		Iterable<RegionAnnotations.Region>
{
	public RegionAnnotations()
	{
	}

	protected abstract AnnotationElement create(int position);

	public Region get(int partitionIndex)
	{
		// The actual annotation index for this region
		int index = partitionIndex * 2;

		if (index >= annotations.size())
			return null;

		AnnotationElement s = annotations.get(index);
		AnnotationElement e = annotations.get(index + 1);

		return new Region(s.position, e.position);
	}

	public int countRegions()
	{
		return annotations.size() / 2;
	}

	public void deleteAll()
	{
		annotations.clear();
	}

	/* Remove data for the Region at location regionIndex */
	public void remove(int regionIndex)
	{
		int index = regionIndex * 2;

		annotations.remove(index + 1);
		annotations.remove(index);
	}

	/* Returns whether the given Region can be found within these annotations */
	public boolean contains(Region r1)
	{
		System.out.println();
		System.out.println("annotations.size()=" + annotations.size());

		for (Region r2 : this)
		{
			System.out.println("  comparing " + r1 + " with " + r2);
			if (r1.s == r2.s && r1.e == r2.e)
				return true;
		}

		System.out.println("NOT FOUND");
		return false;
	}

	/* Remove data for the region matching the start and end nucleotide pos */
	public void remove(int start, int end)
	{
		for (int i = 0; i < annotations.size(); i += 2)
		{
			AnnotationElement s = annotations.get(i);
			AnnotationElement e = annotations.get(i + 1);

			if (s.position == start && e.position == end)
			{
				annotations.remove(i + 1);
				annotations.remove(i);
			}
		}
	}

	public int addRegion(int start, int end)
	{
		return addRegion(new Region(start, end));
	}

	public int addRegion(Region r)
	{
		// System.out.println("addRegion: size=" + annotations.size());
		int insertionIndex = -1;

		// Search for the best place to insert the new partition elements
		for (int i = 0; i < annotations.size(); i += 2)
		{
			AnnotationElement s = annotations.get(i);
			AnnotationElement e = annotations.get(i + 1);

			if (s.position >= r.getS() && e.position >= r.getE())
			{
				insertionIndex = i;
				break;
			}
		}

		if (insertionIndex == -1)
			insertionIndex = annotations.size();

		// Create two new elements for the partition and insert them
		AnnotationElement s = create(r.getS());
		AnnotationElement e = create(r.getE());
		annotations.add(insertionIndex, s);
		annotations.add(insertionIndex + 1, e);

		// Should always be a number divisible by 2
		// System.out.println("Returning index " + (insertionIndex/2));
		return insertionIndex / 2;
	}

	public Iterator<Region> iterator()
	{
		return new RegionIterator();
	}

	class RegionIterator implements Iterator<Region>
	{
		private int nextIndex = 0;

		public boolean hasNext()
		{
			return (nextIndex < annotations.size());
		}

		public Region next()
		{
			Region region = get(nextIndex / 2);
			nextIndex += 2;

			return region;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	public static class Region implements Comparator<Region>
	{
		// Start and ending (region) nucleotide positions
		private int s;

		private int e;

		public Region(int s, int e)
		{
			this.s = s;
			this.e = e;
		}

		@Override
		public String toString()
		{
			return "Nucleotide " + s + " to Nucleotide " + e;
		}

		public int compare(Region r1, Region r2)
		{
			if (r1.s < r2.s)
				return -1;
			if (r1.s > r2.s)
				return 1;

			return 0;
		}

		public int getS()
		{
			return s;
		}

		public void setS(int s)
		{
			this.s = s;
		}

		public int getE()
		{
			return e;
		}

		public void setE(int e)
		{
			this.e = e;
		}

		@Override
		public int hashCode()
		{
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + e;
			result = PRIME * result + s;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Region other = (Region) obj;
			if (e != other.e)
				return false;
			if (s != other.s)
				return false;
			return true;
		}
	}
}