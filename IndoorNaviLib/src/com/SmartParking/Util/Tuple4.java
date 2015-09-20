package com.SmartParking.Util;

public class Tuple4<A, B, C, D> {
	public A first;
	public B second;
	public C third;
	public D fourth;

	public Tuple4(A a, B b, C c, D d) {
		this.first = a;
		this.second = b;
		this.third = c;
		this.fourth = d;
	}

	public static <A, B, C, D> Tuple4<A, B, C, D> create(A a, B b, C c, D d) {
		return new Tuple4<A, B, C, D>(a, b, c, d);
	}
}
