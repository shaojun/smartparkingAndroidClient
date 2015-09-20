package com.SmartParking.Util;

public class Tuple5<A, B, C, D, E> {
	public A first;
	public B second;
	public C third;
	public D fourth;
	public E fifth;

	public Tuple5(A a, B b, C c, D d, E e) {
		this.first = a;
		this.second = b;
		this.third = c;
		this.fourth = d;
		this.fifth = e;
	}

	public static <A, B, C, D, E> Tuple5<A, B, C, D, E> create(A a, B b, C c,
			D d, E e) {
		return new Tuple5<A, B, C, D, E>(a, b, c, d, e);
	}
}
