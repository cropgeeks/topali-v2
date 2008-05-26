// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

public class Distance<T>
{

	T obj1;
	T obj2;
	
	double distance;

	public Distance() {
		
	}
	
	public Distance(T obj1, T obj2, double distance)
	{
		this.obj1 = obj1;
		this.obj2 = obj2;
		this.distance = distance;
	}

	public T getObj1()
	{
		return obj1;
	}

	public void setObj1(T obj1)
	{
		this.obj1 = obj1;
	}

	public T getObj2()
	{
		return obj2;
	}

	public void setObj2(T obj2)
	{
		this.obj2 = obj2;
	}

	public double getDistance()
	{
		return distance;
	}

	public void setDistance(double distance)
	{
		this.distance = distance;
	}
	
}
