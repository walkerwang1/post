package com.paper.alg;

import java.util.Random;

public class Test {
	
	static int NUM = 2 + 2;

	public static void main(String[] args) {
		
		int a = 1;
		int b = 4;
		//[0,3],[1,2]
		for(int i = 0 ; i < 0; i++) {
			int num = (int)( Math.random() * ( b - a + 1 )) + a;
			System.out.println(num);
		}
		
		System.out.println((int)(4 * 0.8));
	}
	
}

class M {
	int a;
	int b;
	
	public M() {
		
	}
	
	public int getA() {
		return a;
	}
	public void setA(int a) {
		this.a = a;
	}
	public int getB() {
		return b;
	}
	public void setB(int b) {
		this.b = b;
	}
	
}
