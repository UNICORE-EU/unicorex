package de.fzj.unicore.uas.util;

/**
 * Convenience class for storing pairs of objects (e.g. Lists of mappings)
 * @author Alexander Lukichev
 *
 * @param <T1>
 * @param <T2>
 */
public class Pair<T1, T2> {

	private T1 m1;
	private T2 m2;

	public Pair() {
		super();
	}

	public Pair(T1 m1, T2 m2) {
		super();
		this.m1 = m1;
		this.m2 = m2;
	}

	public T1 getM1() {
		return m1;
	}

	public void setM1(T1 m1) {
		this.m1 = m1;
	}

	public T2 getM2() {
		return m2;
	}

	public void setM2(T2 m2) {
		this.m2 = m2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m1 == null) ? 0 : m1.hashCode());
		result = prime * result + ((m2 == null) ? 0 : m2.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (m1 == null) {
			if (other.m1 != null)
				return false;
		} else if (!m1.equals(other.m1))
			return false;
		if (m2 == null) {
			if (other.m2 != null)
				return false;
		} else if (!m2.equals(other.m2))
			return false;
		return true;
	}
	
	public String toString(){
		return String.valueOf(m1)+":"+String.valueOf(m2);
	}
	
}

