package com.SmartParking.Util;

public class Tuple<A, B> {
	public A first;
	public B second;

	public Tuple(A a, B b) {
		this.first = a;
		this.second = b;
	}

	public static <A, B> Tuple<A, B> create(A a, B b) {
		return new Tuple<A, B>(a, b);
	}
}
