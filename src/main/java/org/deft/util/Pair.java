package org.deft.util;

public class Pair<T1, T2> {
	
	public final T1 _1;
	public final T2 _2;
	
	public Pair(T1 first, T2 second) {
		this._1 = first;
		this._2 = second;
	}

	public static <X, Y> Pair<X, Y> of(X _1, Y _2) {
		return new Pair<X, Y>(_1, _2);
	}
}
