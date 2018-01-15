package com.paper.ch2;

import java.util.concurrent.ThreadPoolExecutor;

public class Demo {
	
	public static void main(String[] args) {
		ThreadPoolExecutor ew;
		
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 5; j++) {
				int k = 1;
				while (true) {
					if (k==3) {
						continue;
					}
					System.out.println(k);
					k++;
				}
			}
			System.out.println("--------------------");
		}
	}
}
