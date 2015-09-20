package com.SmartParking.Util;


public class Tuple3<A, B, C> {
	public A first;
	public B second;
	public C third;

	public Tuple3(A a, B b, C c) {
		this.first = a;
		this.second = b;
		this.third = c;
	}

	public static <A, B, C> Tuple3<A, B, C> create(A a, B b, C c) {
		return new Tuple3<A, B, C>(a, b, c);
	}
}
